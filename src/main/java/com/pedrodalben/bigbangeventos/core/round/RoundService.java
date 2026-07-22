package com.pedrodalben.bigbangeventos.core.round;

import com.pedrodalben.bigbangeventos.api.OperationResult;
import com.pedrodalben.bigbangeventos.domain.DomainEventBus;
import com.pedrodalben.bigbangeventos.domain.RoundEvents;
import com.pedrodalben.bigbangeventos.session.EventSession;
import com.pedrodalben.bigbangeventos.session.round.RoundFinishReason;
import com.pedrodalben.bigbangeventos.session.round.RoundState;
import com.pedrodalben.bigbangeventos.session.round.SessionRound;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public final class RoundService {
    private final Clock clock;
    private final DomainEventBus events;

    public RoundService(Clock clock, DomainEventBus events) {
        this.clock = clock; this.events = events;
    }

    public synchronized OperationResult prepare(EventSession session, int number, Duration timeLimit, Instant deadline) {
        if (activeRound(session) != null && activeRound(session).state() == RoundState.ACTIVE)
            return OperationResult.fail("round_active", "Rodada ativa já existe");
        SessionRound round = new SessionRound(UUID.randomUUID(), session.eventId(), session.id(), number);
        round.state(RoundState.PREPARING, clock.instant());
        if (deadline != null) round.deadline(deadline);
        else if (timeLimit != null) round.deadline(clock.instant().plus(timeLimit));
        session.addRound(round);
        events.publish(new RoundEvents.RoundPrepared(session.eventId(), session.id(), round.roundId(), number));
        return OperationResult.ok("Rodada preparada");
    }

    public synchronized OperationResult startCountdown(EventSession session) {
        SessionRound round = requireActive(session);
        if (round == null) return OperationResult.fail("no_round", "Nenhuma rodada ativa");
        round.state(RoundState.COUNTDOWN, clock.instant());
        return OperationResult.ok("Contagem regressiva iniciada");
    }

    public synchronized OperationResult start(EventSession session) {
        SessionRound round = requireActive(session);
        if (round == null) return OperationResult.fail("no_round", "Nenhuma rodada ativa");
        if (round.state() == RoundState.ACTIVE) return OperationResult.ok("Rodada já ativa");
        round.state(RoundState.ACTIVE, clock.instant());
        events.publish(new RoundEvents.RoundStarted(session.eventId(), session.id(), round.roundId(), round.number()));
        return OperationResult.ok("Rodada iniciada");
    }

    public synchronized OperationResult finish(EventSession session, RoundFinishReason reason,
                                                UUID winnerParticipant, String winnerTeamDef) {
        SessionRound round = requireActive(session);
        if (round == null) return OperationResult.fail("no_round", "Nenhuma rodada ativa");
        if (round.state() == RoundState.FINISHED) return OperationResult.ok("Rodada já finalizada");
        round.state(RoundState.FINISHING, clock.instant());
        round.finishReason(reason);
        round.winnerParticipantId(winnerParticipant);
        round.winnerTeamDefId(winnerTeamDef);
        round.state(RoundState.FINISHED, clock.instant());
        events.publish(new RoundEvents.RoundFinished(session.eventId(), session.id(), round.roundId(),
                round.number(), winnerParticipant, winnerTeamDef));
        return OperationResult.ok("Rodada finalizada");
    }

    public synchronized OperationResult cancel(EventSession session) {
        SessionRound round = requireActive(session);
        if (round == null) return OperationResult.fail("no_round", "Nenhuma rodada ativa");
        if (round.state() == RoundState.CANCELLED) return OperationResult.ok("Rodada já cancelada");
        round.state(RoundState.CANCELLED, clock.instant());
        events.publish(new RoundEvents.RoundCancelled(session.eventId(), session.id(), round.roundId(), round.number()));
        return OperationResult.ok("Rodada cancelada");
    }

    public synchronized OperationResult fail(EventSession session) {
        SessionRound round = requireActive(session);
        if (round == null) return OperationResult.fail("no_round", "Nenhuma rodada ativa");
        if (round.state() == RoundState.FAILED) return OperationResult.ok("Rodada já falhou");
        round.state(RoundState.FAILED, clock.instant());
        events.publish(new RoundEvents.RoundFailed(session.eventId(), session.id(), round.roundId(), round.number()));
        return OperationResult.ok("Rodada falhou");
    }

    public synchronized OperationResult advance(EventSession session) {
        SessionRound round = requireActive(session);
        if (round == null) return OperationResult.fail("no_round", "Nenhuma rodada ativa");
        if (round.state() != RoundState.FINISHED && round.state() != RoundState.CANCELLED && round.state() != RoundState.FAILED)
            return OperationResult.fail("round_not_finished", "Rodada atual ainda não terminou");
        int nextNumber = round.number() + 1;
        return prepare(session, nextNumber, null, null);
    }

    public SessionRound activeRound(EventSession session) {
        return session.rounds().values().stream()
                .filter(r -> r.state() == RoundState.WAITING || r.state() == RoundState.PREPARING
                        || r.state() == RoundState.COUNTDOWN || r.state() == RoundState.ACTIVE)
                .max(Comparator.comparingInt(SessionRound::number)).orElse(null);
    }

    public SessionRound currentActiveRound(EventSession session) {
        return session.rounds().values().stream()
                .filter(r -> r.state() == RoundState.ACTIVE)
                .findFirst().orElse(null);
    }

    public List<SessionRound> listRounds(EventSession session) {
        return session.rounds().values().stream()
                .sorted(Comparator.comparingInt(SessionRound::number))
                .toList();
    }

    public synchronized void failAllOnRecovery(EventSession session) {
        for (SessionRound r : session.rounds().values()) {
            if (r.state() == RoundState.ACTIVE || r.state() == RoundState.COUNTDOWN
                    || r.state() == RoundState.PREPARING || r.state() == RoundState.WAITING) {
                r.state(RoundState.FAILED, clock.instant());
            }
        }
    }

    private SessionRound requireActive(EventSession session) {
        return activeRound(session);
    }
}
