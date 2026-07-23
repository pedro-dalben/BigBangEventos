package com.pedrodalben.bigbangeventos.api.combat;

import com.pedrodalben.bigbangeventos.core.team.TeamService;
import com.pedrodalben.bigbangeventos.definition.combat.CombatRuleSet;
import com.pedrodalben.bigbangeventos.session.EventSession;

import java.util.UUID;

public final class CombatActorEligibilityService {
    private final TeamService teams;

    public CombatActorEligibilityService(TeamService teams) {
        this.teams = teams;
    }

    public boolean canTarget(EventSession session, CombatActor attacker, CombatActor victim,
                             CombatRuleSet rules) {
        if (!attacker.sessionId().equals(victim.sessionId())) return false;
        if (isSpectator(session, attacker.ownerPlayerId())) return false;
        if (isSpectator(session, victim.ownerPlayerId())) return false;
        if (isEliminated(session, attacker.ownerPlayerId())) return false;
        if (isEliminated(session, victim.ownerPlayerId())) return false;

        if (!rules.friendlyFire() && areSameTeam(session, attacker.teamId(), victim.teamId()))
            return false;

        return true;
    }

    public boolean isActorInSession(CombatActor actor, UUID sessionId) {
        return actor != null && sessionId.equals(actor.sessionId());
    }

    public boolean isOwnerParticipating(CombatActor actor, EventSession session) {
        if (actor == null || actor.ownerPlayerId() == null) return false;
        return session.participants().stream()
            .anyMatch(p -> p.playerId().equals(actor.ownerPlayerId()));
    }

    private boolean isSpectator(EventSession session, UUID playerId) {
        if (playerId == null) return false;
        return session.hasSpectator(playerId);
    }

    private boolean isEliminated(EventSession session, UUID playerId) {
        if (playerId == null) return false;
        return session.combatState(playerId)
            .map(s -> s.eliminated())
            .orElse(false);
    }

    private boolean areSameTeam(EventSession session, UUID teamIdA, UUID teamIdB) {
        return teamIdA != null && teamIdB != null && teamIdA.equals(teamIdB);
    }
}
