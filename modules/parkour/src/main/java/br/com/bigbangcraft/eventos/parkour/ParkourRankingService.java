package br.com.bigbangcraft.eventos.parkour;

import br.com.bigbangcraft.eventos.parkour.model.ParkourParticipantData;
import br.com.bigbangcraft.eventos.parkour.model.ParkourRankingStrategy;
import com.pedrodalben.bigbangeventos.participant.EventParticipant;
import com.pedrodalben.bigbangeventos.participant.ParticipantState;
import com.pedrodalben.bigbangeventos.session.EventSession;

import java.util.*;
import java.util.stream.Collectors;

public final class ParkourRankingService {

    private final ParkourParticipantService participantService;
    private final ParkourTimerService timerService;

    public ParkourRankingService(ParkourParticipantService participantService, ParkourTimerService timerService) {
        this.participantService = participantService;
        this.timerService = timerService;
    }

    public List<RankedEntry> getRanking(EventSession session) {
        String sessionId = session.id().toString();
        String eventId = session.eventId();

        List<RankedEntry> finished = new ArrayList<>();
        List<RankedEntry> nonFinished = new ArrayList<>();

        for (EventParticipant participant : session.participants()) {
            String pid = participant.playerId().toString();
            ParkourParticipantData data = participantService.get(sessionId, pid).orElse(null);
            long elapsed = data != null && data.finishedAt() != null
                    ? data.elapsedMillis()
                    : timerService.getElapsedMillis(sessionId, pid);
            boolean isFinished = participant.state() == ParticipantState.FINISHED;

            RankedEntry entry = new RankedEntry(
                    participant.playerId(),
                    participant.knownName(),
                    elapsed,
                    isFinished,
                    data != null ? data.currentCheckpointId() : null,
                    data != null ? data.completedCount() : 0,
                    0
            );

            if (isFinished) {
                finished.add(entry);
            } else {
                nonFinished.add(entry);
            }
        }

        finished.sort(Comparator.comparingLong(RankedEntry::elapsedMillis));
        nonFinished.sort(Comparator.comparingLong(RankedEntry::elapsedMillis));

        int pos = 1;
        for (RankedEntry entry : finished) {
            entry = new RankedEntry(entry.playerId(), entry.knownName(), entry.elapsedMillis(),
                    entry.finished(), entry.currentCheckpointId(), entry.checkpointsCompleted(), pos);
            finished.set(pos - 1, entry);
            pos++;
        }
        for (RankedEntry entry : nonFinished) {
            entry = new RankedEntry(entry.playerId(), entry.knownName(), entry.elapsedMillis(),
                    entry.finished(), entry.currentCheckpointId(), entry.checkpointsCompleted(), pos);
            nonFinished.set(pos - 1 - finished.size(), entry);
            pos++;
        }

        List<RankedEntry> result = new ArrayList<>(finished);
        result.addAll(nonFinished);
        return result;
    }

    public String formatRanking(EventSession session) {
        List<RankedEntry> ranking = getRanking(session);
        if (ranking.isEmpty()) return "§7Nenhum participante.";

        StringBuilder sb = new StringBuilder("§6=== Ranking Parkour ===\n");
        for (RankedEntry entry : ranking) {
            String time = entry.finished()
                    ? ParkourTimerService.format(entry.elapsedMillis())
                    : "§7em andamento§r";
            String status = entry.finished() ? "§a✔§r" : "§e▶§r";
            String cpInfo = entry.currentCheckpointId() != null
                    ? " §7(CP: " + entry.currentCheckpointId() + ")" : "";
            sb.append(String.format(" §f%s. %s %s %s%s\n",
                    entry.position(), status, entry.knownName(), time, cpInfo));
        }
        return sb.toString();
    }

    public record RankedEntry(UUID playerId, String knownName, long elapsedMillis,
                              boolean finished, String currentCheckpointId,
                              int checkpointsCompleted, int position) {}
}
