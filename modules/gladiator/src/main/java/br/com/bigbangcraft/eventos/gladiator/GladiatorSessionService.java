package br.com.bigbangcraft.eventos.gladiator;

import com.pedrodalben.bigbangeventos.api.OperationResult;
import com.pedrodalben.bigbangeventos.core.EventEngine;
import com.pedrodalben.bigbangeventos.definition.EventDefinition;
import com.pedrodalben.bigbangeventos.definition.EventLocation;
import com.pedrodalben.bigbangeventos.definition.LocationName;
import com.pedrodalben.bigbangeventos.definition.combat.CombatRuleSet;
import com.pedrodalben.bigbangeventos.definition.combat.CombatRuleSet.OutOfBoundsPolicy;
import com.pedrodalben.bigbangeventos.domain.CombatEvents;
import com.pedrodalben.bigbangeventos.domain.DomainEventBus;
import com.pedrodalben.bigbangeventos.domain.RoundEvents;
import com.pedrodalben.bigbangeventos.session.round.RoundFinishReason;
import com.pedrodalben.bigbangeventos.model.respawn.RespawnPolicy;
import com.pedrodalben.bigbangeventos.participant.combat.EliminationReason;
import com.pedrodalben.bigbangeventos.participant.combat.ParticipantCombatState;
import com.pedrodalben.bigbangeventos.participant.spectator.SpectatorReason;
import com.pedrodalben.bigbangeventos.platform.StoredLocation;
import com.pedrodalben.bigbangeventos.session.EventSession;
import com.pedrodalben.bigbangeventos.session.round.RoundState;
import com.pedrodalben.bigbangeventos.session.round.SessionRound;
import net.minecraft.server.MinecraftServer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class GladiatorSessionService {
    private final EventEngine engine;
    private final DomainEventBus events;
    private final MinecraftServer server;
    private final GladiatorRankingService rankingService;
    private final GladiatorCompletionService completionService;
    private final GladiatorValidator validator;
    private final Map<String, RegistrationHandle> registrations = new ConcurrentHashMap<>();

    private record RegistrationHandle(AutoCloseable sub) implements AutoCloseable {
        public void close() { try { sub.close(); } catch (Exception e) {} }
    }

    public GladiatorSessionService(EventEngine engine, DomainEventBus events, MinecraftServer server,
                                    GladiatorRankingService ranking, GladiatorCompletionService completion,
                                    GladiatorValidator validator) {
        this.engine = engine; this.events = events; this.server = server;
        this.rankingService = ranking; this.completionService = completion; this.validator = validator;
    }

    public void enable() {
        subscribeDeath(events.subscribe("gladiator", CombatEvents.ParticipantDeath.class, this::onDeath));
        subscribeRespawned(events.subscribe("gladiator", CombatEvents.ParticipantRespawned.class, this::onRespawned));
    }

    public void disable() {
        registrations.values().forEach(RegistrationHandle::close);
        registrations.clear();
    }

    private void subscribeDeath(AutoCloseable s) {
        registrations.put("death", new RegistrationHandle(s));
    }
    private void subscribeRespawned(AutoCloseable s) {
        registrations.put("respawned", new RegistrationHandle(s));
    }

    public void prepareSession(EventSession session) {
        EventDefinition def = engine.definition(session.eventId()).orElse(null);
        if (def == null) return;
        int totalRounds = GladiatorConfiguration.roundsTotal(def);
        int timeLimit = GladiatorConfiguration.roundTimeLimit(def);
        int lives = GladiatorConfiguration.initialLives(def);

        for (EventParticipantWrapper p : session.participants().stream().map(EventParticipantWrapper::new).toList()) {
            session.combatStateOrCreate(p.id(), lives);
        }

        if (totalRounds > 0) {
            engine.rounds().prepare(session, 1,
                    timeLimit > 0 ? Duration.ofSeconds(timeLimit) : null, null);
        }
    }

    public void startRound(EventSession session) {
        EventDefinition def = engine.definition(session.eventId()).orElse(null);
        if (def == null) return;
        SessionRound round = engine.rounds().currentActiveRound(session);
        if (round == null) {
            engine.rounds().prepare(session, 1,
                    Duration.ofSeconds(GladiatorConfiguration.roundTimeLimit(def)), null);
            round = engine.rounds().currentActiveRound(session);
        }
        if (round != null) {
            engine.rounds().start(session);
        }
    }

    public void finishSession(EventSession session) {
        EventDefinition def = engine.definition(session.eventId()).orElse(null);
        if (def == null) return;
        UUID winner = determineWinner(session, def);
        if (winner != null) {
            server.getPlayerList().getPlayer(winner);
        }
        cleanup(session);
    }

    public void cleanup(EventSession session) {
        engine.respawn().cleanupSession(session.id());
    }

    public OperationResult handleDeath(EventSession session, CombatEvents.ParticipantDeath deathEvent) {
        EventDefinition def = engine.definition(session.eventId()).orElse(null);
        if (def == null) return OperationResult.fail("no_def", "Definição não encontrada");
        String mode = GladiatorConfiguration.mode(def);
        ParticipantCombatState victim = session.combatState(deathEvent.victimId()).orElse(null);
        if (victim == null || victim.eliminated()) return OperationResult.ok("Ignorado");

        SessionRound round = engine.rounds().currentActiveRound(session);
        if (round == null || round.state() != RoundState.ACTIVE) return OperationResult.ok("Fora da rodada ativa");

        CombatRuleSet rules = buildRules(def);

        ParticipantCombatState killer = deathEvent.killerId() != null
                ? session.combatState(deathEvent.killerId()).orElse(null) : null;

        if (rules.friendlyFire() && killer != null && deathEvent.killerId() != null) {
            var kTeam = engine.teams().getPlayerTeam(session, deathEvent.killerId());
            var vTeam = engine.teams().getPlayerTeam(session, deathEvent.victimId());
            if (kTeam != null && vTeam != null && kTeam.teamDefinitionId().equals(vTeam.teamDefinitionId()))
                return OperationResult.ok("Friendly fire bloqueado");
        }

        if (deathEvent.killerId() != null && !deathEvent.killerId().equals(deathEvent.victimId())) {
            if (killer != null) {
                killer.kill();
                round.addParticipantScore(deathEvent.killerId().toString(), GladiatorConfiguration.scorePerKill(def));
            }
        }

        victim.death();
        victim.livesRemaining(victim.livesRemaining() - 1);

        if (victim.livesRemaining() <= 0) {
            eliminatePlayer(session, def, victim);
        } else {
            scheduleRespawn(session, def, victim);
        }

        checkWin(session, def, round, mode);

        return OperationResult.ok("Morte processada");
    }

    private void eliminatePlayer(EventSession session, EventDefinition def, ParticipantCombatState state) {
        state.eliminated(true, EliminationReason.NO_LIVES, Instant.now());
        engine.eliminationService().eliminateDirect(session, state, def.id(), EliminationReason.NO_LIVES);

        if (GladiatorConfiguration.becomeSpectator(def)) {
            Optional<EventLocation> specLoc = def.location(LocationName.SPECTATOR);
            StoredLocation dest = specLoc.map(l -> new StoredLocation(l.serverId(), l.dimension(),
                    l.x(), l.y(), l.z(), l.yaw(), l.pitch())).orElse(null);
            engine.spectator().makeSpectator(session, state.participantId(), SpectatorReason.ELIMINATED, dest);
        }
    }

    private void scheduleRespawn(EventSession session, EventDefinition def, ParticipantCombatState state) {
        String policyStr = GladiatorConfiguration.respawnPolicy(def);
        RespawnPolicy policy = switch (policyStr) {
            case "NONE" -> RespawnPolicy.NONE;
            case "IMMEDIATE" -> RespawnPolicy.IMMEDIATE;
            case "DELAYED" -> RespawnPolicy.DELAYED;
            case "AT_TEAM_SPAWN" -> RespawnPolicy.AT_TEAM_SPAWN;
            case "AT_PERSONAL_SPAWN" -> RespawnPolicy.AT_PERSONAL_SPAWN;
            default -> RespawnPolicy.DELAYED;
        };

        int delay = GladiatorConfiguration.respawnDelay(def);
        int invul = GladiatorConfiguration.invulnerabilitySeconds(def);
        StoredLocation spawn = findSpawn(session, def, state.participantId());

        engine.respawn().scheduleRespawn(session, state, def.id(), policy, delay, invul, spawn);
    }

    private void checkWin(EventSession session, EventDefinition def, SessionRound round, String mode) {
        int scoreLimit = GladiatorConfiguration.scoreLimit(def);

        if ("LAST_PLAYER_STANDING".equals(mode)) {
            long active = session.combatStates().values().stream()
                    .filter(cs -> !cs.eliminated()).count();
            if (active <= 1) {
                UUID winner = session.combatStates().values().stream()
                        .filter(cs -> !cs.eliminated())
                        .map(ParticipantCombatState::participantId)
                        .findFirst().orElse(null);
                engine.rounds().finish(session, RoundFinishReason.LAST_PARTICIPANT, winner, null);
                engine.finish(session.eventId());
            }
        } else if ("FREE_FOR_ALL".equals(mode) && scoreLimit > 0) {
            for (var entry : session.combatStates().entrySet()) {
                if (entry.getValue().sessionKills() >= scoreLimit) {
                    engine.rounds().finish(session, RoundFinishReason.SCORE_LIMIT, entry.getKey(), null);
                    engine.finish(session.eventId());
                    break;
                }
            }
        }
    }

    private UUID determineWinner(EventSession session, EventDefinition def) {
        String strategy = GladiatorConfiguration.rankingStrategy(def);
        var ranked = rankingService.rank(session, strategy);
        return ranked.isEmpty() ? null : ranked.get(0).playerId();
    }

    public CombatRuleSet buildRules(EventDefinition def) {
        return new CombatRuleSet(
            GladiatorConfiguration.pvpEnabled(def),
            GladiatorConfiguration.friendlyFire(def),
            GladiatorConfiguration.fallDamage(def),
            GladiatorConfiguration.voidEliminates(def),
            GladiatorConfiguration.environmentDamage(def),
            false, false, false, false, false,
            List.of(),
            "ELIMINATE".equals(GladiatorConfiguration.outOfBoundsPolicy(def))
                    ? OutOfBoundsPolicy.ELIMINATE : OutOfBoundsPolicy.TELEPORT_BACK
        );
    }

    private StoredLocation findSpawn(EventSession session, EventDefinition def, UUID playerId) {
        Optional<EventLocation> respawnLoc = def.location(LocationName.RESPAWN);
        if (respawnLoc.isPresent()) {
            var l = respawnLoc.get();
            return new StoredLocation(l.serverId(), l.dimension(), l.x(), l.y(), l.z(), l.yaw(), l.pitch());
        }
        Optional<EventLocation> entrance = def.location(LocationName.ENTRANCE);
        if (entrance.isPresent()) {
            var l = entrance.get();
            return new StoredLocation(l.serverId(), l.dimension(), l.x(), l.y(), l.z(), l.yaw(), l.pitch());
        }
        return null;
    }

    private void onDeath(CombatEvents.ParticipantDeath event) {
        engine.activeSession(event.eventId()).ifPresent(session -> {
            handleDeath(session, event);
        });
    }

    private void onRespawned(CombatEvents.ParticipantRespawned event) {}

    private record EventParticipantWrapper(UUID id, String name) {
        EventParticipantWrapper(com.pedrodalben.bigbangeventos.participant.EventParticipant p) {
            this(p.playerId(), p.knownName());
        }
    }
}
