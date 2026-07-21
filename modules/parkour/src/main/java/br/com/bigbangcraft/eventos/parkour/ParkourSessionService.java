package br.com.bigbangcraft.eventos.parkour;

import br.com.bigbangcraft.eventos.parkour.model.*;
import com.pedrodalben.bigbangeventos.api.BigBangEventosApi;
import com.pedrodalben.bigbangeventos.api.OperationResult;
import com.pedrodalben.bigbangeventos.api.module.EventModuleLogger;
import com.pedrodalben.bigbangeventos.definition.EventDefinition;
import com.pedrodalben.bigbangeventos.definition.EventLocation;
import com.pedrodalben.bigbangeventos.participant.EventParticipant;
import com.pedrodalben.bigbangeventos.participant.ParticipantState;
import com.pedrodalben.bigbangeventos.platform.PlatformPlayerService;
import com.pedrodalben.bigbangeventos.platform.PlatformScheduler;
import com.pedrodalben.bigbangeventos.platform.PlatformTeleportService;
import com.pedrodalben.bigbangeventos.platform.StoredLocation;
import com.pedrodalben.bigbangeventos.session.EventSession;
import com.pedrodalben.bigbangeventos.session.SessionState;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ParkourSessionService {

    private final BigBangEventosApi api;
    private final PlatformScheduler scheduler;
    private final PlatformPlayerService players;
    private final PlatformTeleportService teleport;
    private final EventModuleLogger logger;
    private final ParkourParticipantService participantService;
    private final ParkourCheckpointService checkpointService;
    private final ParkourFallService fallService;
    private final ParkourTimerService timerService;
    private final ParkourRankingService rankingService;
    private final ParkourCompletionService completionService;
    private final ParkourValidator validator;

    private final Set<UUID> activeSessions = ConcurrentHashMap.newKeySet();
    private final Map<UUID, PlatformScheduler.ScheduledHandle> tickHandles = new ConcurrentHashMap<>();

    public ParkourSessionService(BigBangEventosApi api, PlatformScheduler scheduler,
                                  PlatformPlayerService players, PlatformTeleportService teleport,
                                  EventModuleLogger logger,
                                  ParkourParticipantService participantService,
                                  ParkourCheckpointService checkpointService,
                                  ParkourFallService fallService,
                                  ParkourTimerService timerService,
                                  ParkourRankingService rankingService,
                                  ParkourCompletionService completionService,
                                  ParkourValidator validator) {
        this.api = api;
        this.scheduler = scheduler;
        this.players = players;
        this.teleport = teleport;
        this.logger = logger;
        this.participantService = participantService;
        this.checkpointService = checkpointService;
        this.fallService = fallService;
        this.timerService = timerService;
        this.rankingService = rankingService;
        this.completionService = completionService;
        this.validator = validator;
    }

    public ParkourValidator validator() {
        return validator;
    }

    public ParkourCheckpointService checkpointService() {
        return checkpointService;
    }

    public ParkourParticipantService participantService() {
        return participantService;
    }

    public ParkourTimerService timerService() {
        return timerService;
    }

    public ParkourRankingService rankingService() {
        return rankingService;
    }

    public ParkourCompletionService completionService() {
        return completionService;
    }

    public ParkourFallService fallService() {
        return fallService;
    }

    public void onSessionStart(EventSession session) {
        String eventId = session.eventId();
        String sessionId = session.id().toString();
        EventDefinition def = api.findEvent(eventId).orElse(null);
        if (def == null) {
            logger.warn("ParkourSessionService: definição não encontrada para {}", eventId);
            return;
        }

        if (!activeSessions.add(session.id())) {
            logger.warn("Sessão {} já está ativa.", session.id());
            return;
        }

        logger.info("Parkour iniciado para evento '{}' com {} participantes.", eventId, session.participantCount());

        EventLocation startLoc = ParkourConfiguration.getStartLocation(def).orElse(null);
        if (startLoc == null) {
            logger.warn("Parkour '{}' sem localização de início definida.", eventId);
            return;
        }

        StoredLocation start = new StoredLocation(startLoc.serverId(), startLoc.dimension(),
                startLoc.x(), startLoc.y(), startLoc.z(), startLoc.yaw(), startLoc.pitch());

        List<ParkourCheckpoint> checkpoints = checkpointService.getCheckpoints(eventId);
        if (checkpoints.isEmpty()) {
            logger.warn("Parkour '{}' sem checkpoints.", eventId);
        }

        String firstCpId = checkpoints.isEmpty() ? null : checkpoints.get(0).id();

        for (EventParticipant participant : session.participants()) {
            UUID pid = participant.playerId();
            String playerStr = pid.toString();

            var data = participantService.getOrCreate(sessionId, playerStr);
            data.currentCheckpointId(firstCpId);
            data.startedAt(Instant.now());
            data.clearCompletedCheckpoints();
            data.falls(0);
            data.attempts(0);

            timerService.start(sessionId, playerStr);

            if (players.isOnline(pid)) {
                teleport.teleport(pid, start);
                players.sendTitle(pid, "§6Parkour!", "§eVá até o primeiro checkpoint");
                players.sendMessage(pid, "§aParkour iniciado! Complete todos os checkpoints.");
            }
        }

        startPeriodicCheck(session);
    }

    public void onSessionFinish(EventSession session) {
        String sessionId = session.id().toString();
        logger.info("Parkour finalizado para sessão {}.", sessionId);

        stopPeriodicCheck(session.id());

        var ranking = rankingService.getRanking(session);
        String rankingStr = rankingService.formatRanking(session);

        for (EventParticipant participant : session.participants()) {
            UUID pid = participant.playerId();
            String playerStr = pid.toString();

            timerService.stop(sessionId, playerStr);

            if (participant.state() == ParticipantState.ACTIVE) {
                participant.state(ParticipantState.FINISHED);
            }

            if (players.isOnline(pid)) {
                players.sendMessage(pid, "§6Parkour encerrado!");
                players.sendMessage(pid, rankingStr);
            }
        }

        activeSessions.remove(session.id());
        participantService.clearSession(sessionId);
        fallService.clearCooldowns(sessionId);
    }

    public void onSessionCancel(EventSession session, String reason) {
        String sessionId = session.id().toString();
        logger.info("Parkour cancelado para sessão {}: {}", sessionId, reason);

        stopPeriodicCheck(session.id());

        for (EventParticipant participant : session.participants()) {
            UUID pid = participant.playerId();
            timerService.stop(sessionId, pid.toString());
            if (players.isOnline(pid)) {
                players.sendMessage(pid, "§cParkour cancelado: " + reason);
            }
        }

        activeSessions.remove(session.id());
        participantService.clearSession(sessionId);
        fallService.clearCooldowns(sessionId);
    }

    private void startPeriodicCheck(EventSession session) {
        UUID sessionUuid = session.id();
        var handle = scheduler.scheduleRepeating(Duration.ofMillis(500), () -> {
            if (!activeSessions.contains(sessionUuid)) return;

            EventSession current = api.getActiveSession(session.eventId()).orElse(null);
            if (current == null || current.state() != SessionState.RUNNING) {
                stopPeriodicCheck(sessionUuid);
                return;
            }

            EventDefinition def = api.findEvent(session.eventId()).orElse(null);
            if (def == null) return;

            String sessionId = sessionUuid.toString();

            for (EventParticipant participant : current.participants()) {
                if (participant.state() != ParticipantState.ACTIVE) continue;
                UUID pid = participant.playerId();
                String playerStr = pid.toString();

                if (!players.isOnline(pid)) continue;

                players.captureLocation(pid).ifPresent(loc -> {
                    tickCheckpointProgression(def, current, sessionId, playerStr, participant, loc);
                    tickFinishCheck(def, current, sessionId, playerStr, pid, loc);
                });
                fallService.checkFall(current, pid);
            }
        });
        tickHandles.put(sessionUuid, handle);
    }

    private void tickCheckpointProgression(EventDefinition def, EventSession session,
                                            String sessionId, String playerStr,
                                            EventParticipant participant, StoredLocation loc) {
        var nextCp = checkpointService.getNextCheckpoint(session.eventId(), sessionId, playerStr);
        if (nextCp.isEmpty()) return;

        ParkourCheckpoint cp = nextCp.get();
        if (cp.contains(loc.serverId(), loc.dimension(), loc.x(), loc.y(), loc.z())) {
            boolean completed = checkpointService.completeCheckpoint(
                    session.eventId(), sessionId, playerStr, cp.id());
            if (completed) {
                String elapsed = ParkourTimerService.format(
                        timerService.getElapsedMillis(sessionId, playerStr));
                UUID pid = participant.playerId();
                players.sendMessage(pid, "§a✔ Checkpoint §f" + cp.id() + "§a completado! (§7" + elapsed + "§a)");
                players.sendTitle(pid, "§a✔ " + cp.id(), "§7" + elapsed);

                participant.checkpoint(checkpointService.getCurrentCheckpointId(sessionId, playerStr) != null
                        ? checkpointService.getCheckpoints(session.eventId()).indexOf(cp) + 1 : 0);
            }
        }
    }

    private void tickFinishCheck(EventDefinition def, EventSession session,
                                  String sessionId, String playerStr, UUID pid,
                                  StoredLocation loc) {
        ParkourFinishMode finishMode = ParkourConfiguration.getFinishMode(def);
        if (finishMode == ParkourFinishMode.MANUAL) return;

        boolean allDone = checkpointService.hasCompletedAll(session.eventId(), sessionId, playerStr);
        boolean cpRequired = ParkourConfiguration.isCheckpointsRequired(def);
        boolean noCpNeeded = !cpRequired && !checkpointService.hasCheckpoints(session.eventId());

        if (!allDone && !noCpNeeded) {
            if (finishMode == ParkourFinishMode.ALL_FINISHERS || finishMode == ParkourFinishMode.FIRST_FINISHER) {
                ParkourConfiguration.getFinishLocation(def).ifPresent(finishLoc -> {
                    double dx = loc.x() - finishLoc.x();
                    double dy = loc.y() - finishLoc.y();
                    double dz = loc.z() - finishLoc.z();
                    double radius = ParkourConfiguration.getFinishRadius(def);
                    if (dx * dx + dy * dy + dz * dz <= radius * radius) {
                        completionService.complete(session, pid, null);
                    }
                });
            }
            return;
        }

        if (finishMode == ParkourFinishMode.ALL_FINISHERS) {
            completionService.complete(session, pid, null);
            boolean allFinished = session.participants().stream()
                    .allMatch(p -> p.state() == ParticipantState.FINISHED
                            || p.state() == ParticipantState.LEFT
                            || p.state() == ParticipantState.DISQUALIFIED);
            if (allFinished) {
                api.finishEvent(session.eventId());
            }
        } else if (finishMode == ParkourFinishMode.FIRST_FINISHER) {
            completionService.complete(session, pid, null);
        }
    }

    private void stopPeriodicCheck(UUID sessionId) {
        var handle = tickHandles.remove(sessionId);
        if (handle != null) {
            handle.cancel();
        }
    }

    public Set<UUID> activeSessions() {
        return Set.copyOf(activeSessions);
    }

    public void shutdown() {
        for (UUID id : List.copyOf(activeSessions)) {
            stopPeriodicCheck(id);
        }
        activeSessions.clear();
    }
}
