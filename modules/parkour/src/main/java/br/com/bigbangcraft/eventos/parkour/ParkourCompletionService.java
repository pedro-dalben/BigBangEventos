package br.com.bigbangcraft.eventos.parkour;

import br.com.bigbangcraft.eventos.parkour.model.*;
import com.pedrodalben.bigbangeventos.api.BigBangEventosApi;
import com.pedrodalben.bigbangeventos.api.OperationResult;
import com.pedrodalben.bigbangeventos.definition.EventDefinition;
import com.pedrodalben.bigbangeventos.definition.EventLocation;
import com.pedrodalben.bigbangeventos.definition.LocationName;
import com.pedrodalben.bigbangeventos.participant.EventParticipant;
import com.pedrodalben.bigbangeventos.participant.ParticipantState;
import com.pedrodalben.bigbangeventos.platform.PlatformTeleportService;
import com.pedrodalben.bigbangeventos.platform.StoredLocation;
import com.pedrodalben.bigbangeventos.session.EventSession;
import com.pedrodalben.bigbangeventos.session.SessionState;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public final class ParkourCompletionService {

    private final BigBangEventosApi api;
    private final ParkourParticipantService participantService;
    private final ParkourTimerService timerService;
    private final ParkourRankingService rankingService;
    private final ParkourCheckpointService checkpointService;
    private final PlatformTeleportService teleport;

    public ParkourCompletionService(BigBangEventosApi api, ParkourParticipantService participantService,
                                    ParkourTimerService timerService, ParkourRankingService rankingService,
                                    ParkourCheckpointService checkpointService, PlatformTeleportService teleport) {
        this.api = api;
        this.participantService = participantService;
        this.timerService = timerService;
        this.rankingService = rankingService;
        this.checkpointService = checkpointService;
        this.teleport = teleport;
    }

    public OperationResult complete(EventSession session, UUID playerId, ParkourCompleteDestination destination) {
        if (session.state() != SessionState.RUNNING) {
            return OperationResult.fail("session_not_running", "Sessão não está em execução.");
        }

        EventParticipant participant = session.participant(playerId).orElse(null);
        if (participant == null) {
            return OperationResult.fail("not_participant", "Jogador não está no evento.");
        }

        if (participant.state() != ParticipantState.ACTIVE) {
            if (participant.state() == ParticipantState.FINISHED) {
                return OperationResult.fail("already_finished", "Jogador já finalizou o parkour.");
            }
            return OperationResult.fail("invalid_state", "Jogador não está ativo no parkour.");
        }

        String sessionId = session.id().toString();
        String playerStr = playerId.toString();

        if (!isFinished(session, playerId)) {
            return OperationResult.fail("checkpoints_incomplete", "Nem todos os checkpoints foram completados.");
        }

        Instant now = Instant.now();
        var pd = participantService.getOrCreate(sessionId, playerStr);
        pd.finishedAt(now);

        long elapsed = Duration.between(pd.startedAt(), now).toMillis();
        if (elapsed <= 0) elapsed = timerService.getElapsedMillis(sessionId, playerStr);
        pd.elapsedMillis(elapsed);

        participant.finish(now);
        participant.data("parkour_elapsed", String.valueOf(elapsed));
        participant.data("parkour_finished_at", now.toString());

        // Recalculate rankings
        List<ParkourRankingService.RankedEntry> ranking = rankingService.getRanking(session);
        for (ParkourRankingService.RankedEntry entry : ranking) {
            session.participant(entry.playerId()).ifPresent(p -> p.position(entry.position()));
        }

        // Teleport to destination
        if (destination == null) {
            destination = ParkourConfiguration.getCompleteDestination(
                    api.findEvent(session.eventId()).orElse(null));
        }
        teleportToDestination(session, playerId, destination);

        EventDefinition def = api.findEvent(session.eventId()).orElse(null);
        if (def != null && ParkourConfiguration.getFinishMode(def) == ParkourFinishMode.FIRST_FINISHER) {
            api.finishEvent(session.eventId());
        }

        return OperationResult.ok("§aVocê completou o parkour em " + ParkourTimerService.format(elapsed) + "!");
    }

    public boolean isFinished(EventSession session, UUID playerId) {
        String sessionId = session.id().toString();
        String playerStr = playerId.toString();
        EventDefinition def = api.findEvent(session.eventId()).orElse(null);
        if (def == null) return false;
        if (checkpointService.hasCompletedAll(session.eventId(), sessionId, playerStr)) return true;
        return !ParkourConfiguration.isCheckpointsRequired(def);
    }

    private void teleportToDestination(EventSession session, UUID playerId, ParkourCompleteDestination destination) {
        EventDefinition def = api.findEvent(session.eventId()).orElse(null);
        if (def == null) return;

        StoredLocation target;
        switch (destination) {
            case EXIT -> {
                EventLocation loc = ParkourConfiguration.getFinishLocation(def).orElse(null);
                if (loc != null) {
                    target = new StoredLocation(loc.serverId(), loc.dimension(), loc.x(), loc.y(), loc.z(), loc.yaw(), loc.pitch());
                } else {
                    target = defaultLocation(def);
                }
            }
            case SPECTATOR -> {
                EventLocation loc = def.location(LocationName.SPECTATOR).orElse(null);
                if (loc != null) {
                    target = new StoredLocation(loc.serverId(), loc.dimension(), loc.x(), loc.y(), loc.z(), loc.yaw(), loc.pitch());
                } else {
                    target = defaultLocation(def);
                }
            }
            case ORIGINAL -> {
                target = defaultLocation(def);
            }
            case STAY -> {
                return;
            }
            default -> {
                target = defaultLocation(def);
            }
        }
        teleport.teleport(playerId, target);
    }

    private StoredLocation defaultLocation(EventDefinition def) {
        return new StoredLocation(def.serverId(), "minecraft:overworld", 0, 64, 0, 0, 0);
    }
}
