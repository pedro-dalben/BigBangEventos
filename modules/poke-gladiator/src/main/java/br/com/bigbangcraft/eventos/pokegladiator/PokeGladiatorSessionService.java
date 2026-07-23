package br.com.bigbangcraft.eventos.pokegladiator;

import com.pedrodalben.bigbangeventos.api.module.EventModuleContext;
import com.pedrodalben.bigbangeventos.definition.EventDefinition;
import com.pedrodalben.bigbangeventos.definition.LocationName;
import com.pedrodalben.bigbangeventos.definition.combat.CombatRuleSet;
import com.pedrodalben.bigbangeventos.domain.CombatEvents;
import com.pedrodalben.bigbangeventos.domain.DomainEventBus;
import com.pedrodalben.bigbangeventos.participant.combat.EliminationReason;
import com.pedrodalben.bigbangeventos.participant.combat.ParticipantCombatState;
import com.pedrodalben.bigbangeventos.model.respawn.RespawnPolicy;
import com.pedrodalben.bigbangeventos.platform.StoredLocation;
import com.pedrodalben.bigbangeventos.session.EventSession;
import com.pedrodalben.bigbangeventos.session.round.RoundFinishReason;
import com.pedrodalben.bigbangeventos.session.round.SessionRound;

import java.util.*;

public class PokeGladiatorSessionService {

    private final EventModuleContext ctx;
    private final Map<String, List<DomainEventBus.Subscription>> subs = new HashMap<>();

    public PokeGladiatorSessionService(EventModuleContext ctx) {
        this.ctx = ctx;
    }

    public void enable() {
        var events = ctx.events();
        var sub1 = events.subscribe("poke_gladiator", CombatEvents.ParticipantDeath.class, this::handleDeath);
        var sub2 = events.subscribe("poke_gladiator", CombatEvents.ParticipantRespawned.class, this::handleRespawn);
        subs.computeIfAbsent("default", k -> new ArrayList<>()).addAll(List.of(sub1, sub2));
    }

    public void onSessionStart(EventSession session) {
        EventDefinition def = ctx.api().findEvent(session.eventId()).orElse(null);
        if (def == null) return;

        int lives = PokeGladiatorConfiguration.initialLives(def);
        for (var p : session.participants()) {
            session.combatStateOrCreate(p.playerId(), lives);
        }

        int rounds = PokeGladiatorConfiguration.roundsTotal(def);
        int limit = PokeGladiatorConfiguration.roundTime(def);
        if (rounds > 0) {
            ctx.rounds().prepare(session, 1, java.time.Duration.ofSeconds(limit), null);
            ctx.rounds().start(session);
        }

        ctx.logger().info("Sessao PokeGladiator iniciada: " + session.eventId());
    }

    public void onSessionFinish(EventSession session) {
        cleanup(session);
        ctx.logger().info("Sessao PokeGladiator finalizada: " + session.eventId());
    }

    public void onSessionCancel(EventSession session) {
        cleanup(session);
        ctx.logger().info("Sessao PokeGladiator cancelada: " + session.eventId());
    }

    public void cleanupAll() {
        for (var subList : subs.values()) {
            for (var sub : subList) sub.close();
        }
        subs.clear();
    }

    private void handleDeath(CombatEvents.ParticipantDeath death) {
        var api = ctx.api();
        EventSession session = api.getActiveSession(death.eventId()).orElse(null);
        if (session == null) return;
        EventDefinition def = api.findEvent(death.eventId()).orElse(null);
        if (def == null) return;

        ParticipantCombatState victim = session.combatState(death.victimId()).orElse(null);
        if (victim == null || victim.eliminated()) return;
        if (victim.isInvulnerable(java.time.Instant.now())) return;

        String lossPolicy = PokeGladiatorConfiguration.lossPolicy(def);
        if (!"TRAINER_DEATH".equals(lossPolicy) && !"EITHER".equals(lossPolicy)) return;

        kill(death, session, def, victim);
    }

    private void kill(CombatEvents.ParticipantDeath death, EventSession session, EventDefinition def, ParticipantCombatState victim) {
        var rules = buildRules(def);

        if (death.killerId() != null) {
            ParticipantCombatState killer = session.combatState(death.killerId()).orElse(null);
            if (killer != null && !killer.eliminated()) {
                if (!rules.friendlyFire() && areSameTeam(session, death.killerId(), death.victimId()))
                    return;

                killer.kill();
                SessionRound round = ctx.rounds().activeRound(session);
                if (round != null) round.addParticipantScore(death.killerId().toString(), 1);
                ctx.events().publish(new CombatEvents.ParticipantKill(death.eventId(), session.id(), death.killerId(), death.victimId()));
            }
        }

        victim.death();
        boolean eliminated = processElimination(death, session, def, victim);

        if (!eliminated) {
            scheduleRespawn(death, session, def, victim);
        }

        checkWin(death, session, def);
    }

    private boolean processElimination(CombatEvents.ParticipantDeath death, EventSession session, EventDefinition def, ParticipantCombatState victim) {
        if (victim.livesRemaining() > 0) return false;

        ctx.eliminations().eliminateDirect(session, victim, death.eventId(), EliminationReason.NO_LIVES);
        ctx.events().publish(new CombatEvents.ParticipantEliminated(death.eventId(), session.id(), death.victimId(), EliminationReason.NO_LIVES));

        if (PokeGladiatorConfiguration.becomeSpectator(def)) {
            EventDefinition d = api().findEvent(death.eventId()).orElse(null);
            StoredLocation spectatorLoc = null;
            if (d != null) {
                spectatorLoc = d.location(LocationName.SPECTATOR)
                    .map(l -> new StoredLocation(l.serverId(), l.dimension(), l.x(), l.y(), l.z(), l.yaw(), l.pitch()))
                    .orElse(null);
            }
            ctx.spectators().makeSpectator(session, death.victimId(), com.pedrodalben.bigbangeventos.participant.spectator.SpectatorReason.ELIMINATED, spectatorLoc);
        }

        return true;
    }

    private void scheduleRespawn(CombatEvents.ParticipantDeath death, EventSession session, EventDefinition def, ParticipantCombatState victim) {
        String policyStr = "DELAYED";
        int delay = PokeGladiatorConfiguration.respawnDelay(def);
        int invul = PokeGladiatorConfiguration.invulnerability(def);

        EventDefinition d = api().findEvent(death.eventId()).orElse(null);
        StoredLocation spawnLoc = null;
        if (d != null) {
            spawnLoc = d.location(LocationName.ENTRANCE)
                .map(l -> new StoredLocation(l.serverId(), l.dimension(), l.x(), l.y(), l.z(), l.yaw(), l.pitch()))
                .orElse(null);
        }

        RespawnPolicy policy = switch (policyStr) {
            case "IMMEDIATE" -> RespawnPolicy.IMMEDIATE;
            case "NONE" -> RespawnPolicy.NONE;
            default -> RespawnPolicy.DELAYED;
        };

        ctx.respawns().scheduleRespawn(session, victim, death.eventId(), policy, delay, invul, spawnLoc);
    }

    private void handleRespawn(CombatEvents.ParticipantRespawned respawn) {}

    private void checkWin(CombatEvents.ParticipantDeath death, EventSession session, EventDefinition def) {
        String mode = PokeGladiatorConfiguration.mode(def);
        long activeCount = session.combatStates().values().stream()
            .filter(cs -> !cs.eliminated()).count();

        if ("LAST_TRAINER_STANDING".equals(mode) && activeCount <= 1) {
            SessionRound round = ctx.rounds().activeRound(session);
            UUID winnerId = session.combatStates().entrySet().stream()
                .filter(e -> !e.getValue().eliminated())
                .map(Map.Entry::getKey).findFirst().orElse(null);
            if (round != null) ctx.rounds().finish(session, RoundFinishReason.LAST_PARTICIPANT, winnerId, null);
            api().finishEvent(death.eventId());
        }
    }

    private void cleanup(EventSession session) {
        ctx.combat().resetRoundStats(session);
    }

    private CombatRuleSet buildRules(EventDefinition def) {
        return new CombatRuleSet(
            PokeGladiatorConfiguration.trainerVsTrainer(def),
            PokeGladiatorConfiguration.friendlyFire(def),
            true, true, true, false, false, false, false, true,
            List.of(),
            CombatRuleSet.OutOfBoundsPolicy.IGNORE
        );
    }

    private boolean areSameTeam(EventSession session, UUID playerA, UUID playerB) {
        var teamA = ctx.teams().getPlayerTeam(session, playerA);
        var teamB = ctx.teams().getPlayerTeam(session, playerB);
        return teamA != null && teamB != null && teamA.teamDefinitionId().equals(teamB.teamDefinitionId());
    }

    private com.pedrodalben.bigbangeventos.api.BigBangEventosApi api() { return ctx.api(); }
}
