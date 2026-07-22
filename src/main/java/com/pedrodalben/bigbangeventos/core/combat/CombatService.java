package com.pedrodalben.bigbangeventos.core.combat;

import com.pedrodalben.bigbangeventos.api.OperationResult;
import com.pedrodalben.bigbangeventos.definition.combat.CombatRuleSet;
import com.pedrodalben.bigbangeventos.definition.combat.CombatRuleSet.OutOfBoundsPolicy;
import com.pedrodalben.bigbangeventos.domain.CombatEvents;
import com.pedrodalben.bigbangeventos.domain.DomainEventBus;
import com.pedrodalben.bigbangeventos.participant.combat.EliminationReason;
import com.pedrodalben.bigbangeventos.participant.combat.ParticipantCombatState;
import com.pedrodalben.bigbangeventos.session.EventSession;
import com.pedrodalben.bigbangeventos.session.round.SessionRound;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public final class CombatService {
    private final Clock clock;
    private final DomainEventBus events;
    private final LifeService lifeService;
    private final EliminationService eliminationService;

    public CombatService(Clock clock, DomainEventBus events, LifeService lifeService, EliminationService eliminationService) {
        this.clock = clock; this.events = events; this.lifeService = lifeService; this.eliminationService = eliminationService;
    }

    public OperationResult onDeath(EventSession session, ParticipantCombatState victim, ParticipantCombatState killer,
                                    UUID killerId, String damageSource, String eventId,
                                    CombatRuleSet rules, EliminationService.EliminationHook eliminationHook) {
        if (victim.eliminated()) return OperationResult.ok("Jogador já eliminado");
        if (victim.isInvulnerable(clock.instant())) return OperationResult.ok("Jogador invulnerável");

        victim.death(killerId != null ? killerId : null);
        events.publish(new CombatEvents.ParticipantDeath(eventId, session.id(), victim.participantId(), killerId, damageSource));

        if (killer != null && killerId != null && !killerId.equals(victim.participantId())) {
            if (!rules.friendlyFire() && areSameTeam(session, killerId, victim.participantId())) {
                return OperationResult.ok("Friendly fire bloqueado");
            }
            killer.kill();
            events.publish(new CombatEvents.ParticipantKill(eventId, session.id(), killerId, victim.participantId()));
        }

        lifeService.removeLife(victim, session.id(), eventId);

        if (victim.livesRemaining() <= 0) {
            return eliminationService.eliminate(session, victim, eventId, EliminationReason.NO_LIVES, eliminationHook);
        }

        return OperationResult.ok("Morte registrada — vidas restantes: " + victim.livesRemaining());
    }

    public OperationResult handleOutOfBounds(EventSession session, ParticipantCombatState state, UUID playerId, String eventId,
                                               CombatRuleSet rules, EliminationService.EliminationHook eliminationHook) {
        if (rules.outOfBoundsPolicy() == OutOfBoundsPolicy.IGNORE) return OperationResult.ok("Política IGNORE");
        if (state.eliminated()) return OperationResult.ok("Jogador já eliminado");

        if (rules.outOfBoundsPolicy() == OutOfBoundsPolicy.ELIMINATE) {
            return eliminationService.eliminate(session, state, eventId, EliminationReason.OUT_OF_BOUNDS, eliminationHook);
        }
        return OperationResult.ok("Out of bounds ignorado");
    }

    public OperationResult addKillScore(ParticipantCombatState killerState, SessionRound round, long scorePerKill) {
        killerState.kill();
        if (round != null) {
            round.addParticipantScore(killerState.participantId().toString(), scorePerKill);
        }
        return OperationResult.ok("Kill registrado");
    }

    public void resetRoundStats(EventSession session) {
        session.combatStates().values().forEach(ParticipantCombatState::resetRoundStats);
    }

    private boolean areSameTeam(EventSession session, UUID p1, UUID p2) {
        var t1 = session.teams().values().stream().filter(t -> t.hasMember(p1)).findFirst();
        var t2 = session.teams().values().stream().filter(t -> t.hasMember(p2)).findFirst();
        return t1.isPresent() && t2.isPresent() && t1.get().teamDefinitionId().equals(t2.get().teamDefinitionId());
    }

    public LifeService lifeService() { return lifeService; }
    public EliminationService eliminationService() { return eliminationService; }
}
