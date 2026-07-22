package com.pedrodalben.bigbangeventos.core.combat;

import com.pedrodalben.bigbangeventos.api.OperationResult;
import com.pedrodalben.bigbangeventos.domain.CombatEvents;
import com.pedrodalben.bigbangeventos.domain.DomainEventBus;
import com.pedrodalben.bigbangeventos.participant.combat.ParticipantCombatState;
import java.util.UUID;

public final class LifeService {
    private final DomainEventBus events;

    public LifeService(DomainEventBus events) {
        this.events = events;
    }

    public OperationResult initializeLives(ParticipantCombatState state, int lives, int maxLives) {
        if (lives < 0) return OperationResult.fail("invalid_lives", "Vidas não podem ser negativas");
        state.livesRemaining(lives);
        return OperationResult.ok("Vidas inicializadas");
    }

    public OperationResult addLife(ParticipantCombatState state, UUID sessionId, String eventId, int amount) {
        if (amount < 0) return OperationResult.fail("invalid_amount", "Quantidade inválida");
        if (state.eliminated()) return OperationResult.fail("eliminated", "Jogador eliminado");
        state.addLife();
        events.publish(new CombatEvents.ParticipantLifeChanged(eventId, sessionId, state.participantId(), state.livesRemaining(), amount));
        return OperationResult.ok("Vida adicionada");
    }

    public OperationResult removeLife(ParticipantCombatState state, UUID sessionId, String eventId) {
        if (state.eliminated()) return OperationResult.fail("eliminated", "Jogador eliminado");
        state.removeLife();
        events.publish(new CombatEvents.ParticipantLifeChanged(eventId, sessionId, state.participantId(), state.livesRemaining(), -1));
        if (state.livesRemaining() <= 0) {
            return OperationResult.ok("Sem vidas — elegível para eliminação");
        }
        return OperationResult.ok("Vida removida");
    }

    public OperationResult setLives(ParticipantCombatState state, UUID sessionId, String eventId, int lives) {
        if (lives < 0) return OperationResult.fail("invalid_lives", "Vidas não podem ser negativas");
        int delta = lives - state.livesRemaining();
        state.livesRemaining(lives);
        events.publish(new CombatEvents.ParticipantLifeChanged(eventId, sessionId, state.participantId(), state.livesRemaining(), delta));
        return OperationResult.ok("Vidas definidas");
    }

    public int getLives(ParticipantCombatState state) {
        return state.livesRemaining();
    }
}
