package com.pedrodalben.bigbangeventos.core.combat;

import com.pedrodalben.bigbangeventos.api.OperationResult;
import com.pedrodalben.bigbangeventos.domain.CombatEvents;
import com.pedrodalben.bigbangeventos.domain.DomainEventBus;
import com.pedrodalben.bigbangeventos.participant.combat.EliminationReason;
import com.pedrodalben.bigbangeventos.participant.combat.ParticipantCombatState;
import com.pedrodalben.bigbangeventos.session.EventSession;
import com.pedrodalben.bigbangeventos.participant.ParticipantState;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public final class EliminationService {
    private final Clock clock;
    private final DomainEventBus events;

    public EliminationService(Clock clock, DomainEventBus events) {
        this.clock = clock; this.events = events;
    }

    @FunctionalInterface
    public interface EliminationHook {
        void onEliminated(String eventId, UUID sessionId, UUID player, EliminationReason reason);
    }

    public OperationResult eliminate(EventSession session, ParticipantCombatState state, String eventId,
                                      EliminationReason reason, EliminationHook hook) {
        if (state.eliminated()) return OperationResult.ok("Jogador já eliminado");
        Instant now = clock.instant();
        state.eliminated(true, reason, now);
        var p = session.participant(state.participantId());
        p.ifPresent(ep -> {
            ep.state(ParticipantState.ELIMINATED);
            ep.data("elimination_reason", reason.name().toLowerCase());
        });
        events.publish(new CombatEvents.ParticipantEliminated(eventId, session.id(), state.participantId(), reason));
        if (hook != null) hook.onEliminated(eventId, session.id(), state.participantId(), reason);
        return OperationResult.ok("Jogador eliminado");
    }

    public OperationResult eliminateDirect(EventSession session, ParticipantCombatState state, String eventId, EliminationReason reason) {
        return eliminate(session, state, eventId, reason, null);
    }
}
