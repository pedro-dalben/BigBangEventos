package com.pedrodalben.bigbangeventos.participant;

import com.pedrodalben.bigbangeventos.api.OperationResult;
import com.pedrodalben.bigbangeventos.core.PlayerRestoreService;
import com.pedrodalben.bigbangeventos.definition.EventDefinition;
import com.pedrodalben.bigbangeventos.platform.*;
import com.pedrodalben.bigbangeventos.session.*;
import com.pedrodalben.bigbangeventos.snapshot.*;
import java.time.Clock;
import java.util.*;

public final class ParticipationService {
    private final Clock clock;
    private final Map<UUID, UUID> sessionByPlayer = new HashMap<>();
    private final Map<UUID, Object> playerLocks = new HashMap<>();
    private final Map<UUID, Object> sessionLocks = new HashMap<>();
    private final SnapshotService snapshots;
    private final PlatformTeleportService teleport;
    private final PlatformPlayerService players;
    private final PlayerRestoreService restore;
    private final InventoryMode defaultInventoryMode;

    public ParticipationService(Clock clock, SnapshotService snapshots,
                                PlatformTeleportService teleport, PlatformPlayerService players,
                                PlayerRestoreService restore, InventoryMode defaultInventoryMode) {
        this.clock = clock;
        this.snapshots = snapshots;
        this.teleport = teleport;
        this.players = players;
        this.restore = restore;
        this.defaultInventoryMode = defaultInventoryMode;
    }

    public synchronized OperationResult join(EventDefinition definition, EventSession session,
                                              UUID playerId, String playerName, boolean forced, boolean allowed) {
        Object playerLock = playerLocks.computeIfAbsent(playerId, k -> new Object());
        Object sessionLock = sessionLocks.computeIfAbsent(session.id(), k -> new Object());

        synchronized (playerLock) {
            synchronized (sessionLock) {
                return joinLocked(definition, session, playerId, playerName, forced, allowed);
            }
        }
    }

    private OperationResult joinLocked(EventDefinition definition, EventSession session,
                                        UUID playerId, String playerName, boolean forced, boolean allowed) {
        if (!forced && !allowed)
            return OperationResult.fail("no_permission", "Sem permissão para entrar");
        if (!definition.enabled())
            return OperationResult.fail("disabled", "Evento desabilitado");
        if (session.state() != SessionState.REGISTRATION_OPEN)
            return OperationResult.fail("registration_closed", "Inscrições indisponíveis");
        if (sessionByPlayer.containsKey(playerId))
            return OperationResult.fail("already_in_event", "Jogador já está em evento");
        if (definition.maxPlayers() > 0 && session.participantCount() >= definition.maxPlayers())
            return OperationResult.fail("full", "Evento lotado");
        if (!players.isOnline(playerId))
            return OperationResult.fail("player_offline", "Jogador não está online");

        EventParticipant participant = new EventParticipant(playerId, playerName, clock.instant());
        session.addParticipant(participant);

        InventoryMode invMode = resolveInventoryMode(definition);
        OperationResult snapshotResult = snapshots.prepare(playerId, session.id(), invMode);
        if (!snapshotResult.success()) {
            session.removeParticipant(playerId);
            return snapshotResult;
        }

        PlayerSnapshot snapshot = snapshots.findPendingForPlayer(playerId);
        if (snapshot != null) {
            participant.snapshotId(snapshot.snapshotId());
        }

        sessionByPlayer.put(playerId, session.id());

        definition.location(com.pedrodalben.bigbangeventos.definition.LocationName.LOBBY).ifPresent(lobby -> {
            StoredLocation dest = new StoredLocation(lobby.serverId(), lobby.dimension(),
                    lobby.x(), lobby.y(), lobby.z(), lobby.yaw(), lobby.pitch());
            OperationResult tpResult = teleport.teleport(playerId, dest);
            if (!tpResult.success()) {
                // ponytail: teleport failed after snapshot — keep snapshot, player stays
                // but is registered. Staff can fix.
            }
        });

        AuditLogger.playerJoined(playerId, definition.id(), session.id());
        return OperationResult.ok("Entrada confirmada");
    }

    public synchronized OperationResult forceJoin(EventDefinition definition, EventSession session,
                                                   UUID playerId, String playerName) {
        return join(definition, session, playerId, playerName, true, true);
    }

    public synchronized OperationResult leave(EventSession session, UUID playerId, String reason) {
        Object playerLock = playerLocks.computeIfAbsent(playerId, k -> new Object());
        Object sessionLock = sessionLocks.computeIfAbsent(session.id(), k -> new Object());

        synchronized (playerLock) {
            synchronized (sessionLock) {
                return leaveLocked(session, playerId, reason);
            }
        }
    }

    private OperationResult leaveLocked(EventSession session, UUID playerId, String reason) {
        EventParticipant p = session.participant(playerId).orElse(null);
        if (p == null) return OperationResult.ok("Jogador já não participa");

        if (p.state() == ParticipantState.LEFT || p.state() == ParticipantState.RESTORED)
            return OperationResult.ok("Jogador já saiu");

        OperationResult restoreResult = restore.restore(playerId, null);

        p.leave(reason);
        session.removeParticipant(playerId);
        sessionByPlayer.remove(playerId);

        AuditLogger.playerLeft(playerId, session.eventId(), reason);
        return restoreResult.success() ? OperationResult.ok("Saída confirmada")
                : OperationResult.fail("restore_failed", "Saída registrada, mas restauração falhou — use /evento recovery retry");
    }

    public synchronized OperationResult kick(EventSession session, UUID actorId, UUID playerId, String reason) {
        AuditLogger.playerKicked(actorId, playerId, session.eventId());
        return leave(session, playerId, "kick: " + reason);
    }

    public synchronized Optional<UUID> sessionFor(UUID playerId) {
        return Optional.ofNullable(sessionByPlayer.get(playerId));
    }

    public synchronized void rebuild(EventSession session) {
        session.participants().forEach(p -> sessionByPlayer.put(p.playerId(), session.id()));
    }

    private InventoryMode resolveInventoryMode(EventDefinition definition) {
        Map<String, Object> settings = definition.typeSettings();
        if (settings.containsKey("inventoryMode")) {
            try {
                return InventoryMode.valueOf(settings.get("inventoryMode").toString().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        return defaultInventoryMode;
    }
}
