package br.com.bigbangcraft.eventos.gladiator;

import com.pedrodalben.bigbangeventos.api.OperationResult;
import com.pedrodalben.bigbangeventos.definition.EventDefinition;
import com.pedrodalben.bigbangeventos.participant.combat.ParticipantCombatState;
import com.pedrodalben.bigbangeventos.session.EventSession;
import com.pedrodalben.bigbangeventos.session.round.RoundFinishReason;
import com.pedrodalben.bigbangeventos.session.round.SessionRound;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

public final class GladiatorCompletionService {

    @FunctionalInterface
    public interface WinHandler {
        void onWin(String eventId, UUID sessionId, UUID winnerId, String mode);
    }

    public OperationResult checkWinCondition(EventSession session, EventDefinition def, SessionRound round,
                                              String mode, int scoreLimit, WinHandler handler) {
        if ("LAST_PLAYER_STANDING".equals(mode)) {
            return checkLastPlayerStanding(session, def, round, handler);
        } else if ("FREE_FOR_ALL".equals(mode)) {
            return checkFreeForAll(session, def, round, scoreLimit, handler);
        }
        return OperationResult.ok("Nenhuma condição de vitória");
    }

    private OperationResult checkLastPlayerStanding(EventSession session, EventDefinition def,
                                                      SessionRound round, WinHandler handler) {
        List<UUID> alive = session.combatStates().values().stream()
                .filter(cs -> !cs.eliminated())
                .map(ParticipantCombatState::participantId)
                .toList();

        if (alive.size() <= 1) {
            UUID winner = alive.isEmpty() ? null : alive.get(0);
            if (round != null) {
                round.finishReason(RoundFinishReason.LAST_PARTICIPANT);
                round.winnerParticipantId(winner);
            }
            if (handler != null && winner != null) {
                handler.onWin(def.id(), session.id(), winner, "LAST_PLAYER_STANDING");
            }
            return OperationResult.ok("Último participante restante");
        }
        return OperationResult.ok(alive.size() + " participantes restantes");
    }

    private OperationResult checkFreeForAll(EventSession session, EventDefinition def,
                                              SessionRound round, int scoreLimit, WinHandler handler) {
        if (scoreLimit <= 0) return OperationResult.ok("Sem score limit");

        for (var entry : session.combatStates().entrySet()) {
            ParticipantCombatState cs = entry.getValue();
            if (cs.sessionKills() >= scoreLimit) {
                if (round != null) {
                    round.finishReason(RoundFinishReason.SCORE_LIMIT);
                    round.winnerParticipantId(entry.getKey());
                }
                if (handler != null) {
                    handler.onWin(def.id(), session.id(), entry.getKey(), "FREE_FOR_ALL");
                }
                return OperationResult.ok("Score limit atingido");
            }
        }
        return OperationResult.ok("Nenhum score limit atingido");
    }
}
