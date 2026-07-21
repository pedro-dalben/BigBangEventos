package br.com.bigbangcraft.eventos.parkour;

import br.com.bigbangcraft.eventos.parkour.model.*;
import com.pedrodalben.bigbangeventos.api.BigBangEventosApi;
import com.pedrodalben.bigbangeventos.definition.EventDefinition;
import com.pedrodalben.bigbangeventos.definition.EventLocation;
import com.pedrodalben.bigbangeventos.platform.PlatformPlayerService;
import com.pedrodalben.bigbangeventos.platform.StoredLocation;
import com.pedrodalben.bigbangeventos.platform.PlatformTeleportService;
import com.pedrodalben.bigbangeventos.session.EventSession;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ParkourFallService {

    private final BigBangEventosApi api;
    private final PlatformPlayerService players;
    private final PlatformTeleportService teleport;
    private final ParkourParticipantService participantService;
    private final ParkourCheckpointService checkpointService;
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 1000;

    public ParkourFallService(BigBangEventosApi api, PlatformPlayerService players,
                              PlatformTeleportService teleport,
                              ParkourParticipantService participantService,
                              ParkourCheckpointService checkpointService) {
        this.api = api;
        this.players = players;
        this.teleport = teleport;
        this.participantService = participantService;
        this.checkpointService = checkpointService;
    }

    public boolean checkFall(EventSession session, UUID playerId) {
        EventDefinition def = api.findEvent(session.eventId()).orElse(null);
        if (def == null) return false;

        ParkourFallMode fallMode = ParkourConfiguration.getFallMode(def);
        if (fallMode == ParkourFallMode.VOID) return false;

        double fallY = ParkourConfiguration.getFallYLevel(def);
        String playerStr = playerId.toString();
        String sessionId = session.id().toString();
        ParkourParticipantData data = participantService.getOrCreate(sessionId, playerStr);

        // Check cooldown
        String cooldownKey = sessionId + ":" + playerStr;
        long now = System.currentTimeMillis();
        Long lastFall = cooldowns.get(cooldownKey);
        if (lastFall != null && (now - lastFall) < COOLDOWN_MS) return false;

        boolean fell = players.captureLocation(playerId)
                .map(loc -> loc.y() < fallY)
                .orElse(false);

        if (fell) {
            cooldowns.put(cooldownKey, now);
            data.incrementFalls();
            data.incrementAttempts();
            data.resetInProgress(true);

            StoredLocation destination = getResetDestination(def, sessionId, playerStr);
            teleport.teleport(playerId, destination);

            data.resetInProgress(false);

            if (isMaxAttemptsReached(def, data)) {
                session.participant(playerId).ifPresent(p -> {
                    p.leave("max_attempts");
                    session.removeParticipant(playerId);
                });
            }
        }

        return fell;
    }

    public StoredLocation getResetDestination(EventDefinition def, String sessionId, String playerId) {
        ParkourResetMode resetMode = ParkourConfiguration.getResetMode(def);
        if (resetMode == ParkourResetMode.LAST_CHECKPOINT) {
            ParkourParticipantData data = participantService.get(sessionId, playerId).orElse(null);
            if (data != null && data.currentCheckpointId() != null) {
                String cpId = data.currentCheckpointId();
                ParkourCheckpoint cp = checkpointService.findCheckpoint(def.id(), cpId).orElse(null);
                if (cp != null) {
                    EventLocation loc = cp.location();
                    return new StoredLocation(loc.serverId(), loc.dimension(),
                            loc.x(), loc.y(), loc.z(), loc.yaw(), loc.pitch());
                }
            }
        }
        EventLocation start = ParkourConfiguration.getStartLocation(def).orElse(null);
        if (start != null) {
            return new StoredLocation(start.serverId(), start.dimension(),
                    start.x(), start.y(), start.z(), start.yaw(), start.pitch());
        }
        return new StoredLocation(def.serverId(), "minecraft:overworld", 0, 64, 0, 0, 0);
    }

    public int getFallCount(String sessionId, String playerId) {
        return participantService.get(sessionId, playerId)
                .map(ParkourParticipantData::falls)
                .orElse(0);
    }

    public int getAttempts(String sessionId, String playerId) {
        return participantService.get(sessionId, playerId)
                .map(ParkourParticipantData::attempts)
                .orElse(0);
    }

    public boolean isMaxAttemptsReached(String sessionId, String playerId) {
        EventSession session = findSession(sessionId);
        if (session == null) return false;
        EventDefinition def = api.findEvent(session.eventId()).orElse(null);
        if (def == null) return false;
        ParkourParticipantData data = participantService.get(sessionId, playerId).orElse(null);
        if (data == null) return false;
        return isMaxAttemptsReached(def, data);
    }

    private boolean isMaxAttemptsReached(EventDefinition def, ParkourParticipantData data) {
        int max = ParkourConfiguration.getMaxAttempts(def);
        return max > 0 && data.attempts() >= max;
    }

    private EventSession findSession(String sessionId) {
        UUID uid;
        try { uid = UUID.fromString(sessionId); } catch (IllegalArgumentException e) { return null; }
        return api.getActiveSession(uid.toString()).orElse(null);
    }

    public void clearCooldowns(String sessionId) {
        cooldowns.entrySet().removeIf(e -> e.getKey().startsWith(sessionId + ":"));
    }
}
