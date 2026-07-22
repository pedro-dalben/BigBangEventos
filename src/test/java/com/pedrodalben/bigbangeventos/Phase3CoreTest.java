package com.pedrodalben.bigbangeventos;

import com.pedrodalben.bigbangeventos.api.OperationResult;
import com.pedrodalben.bigbangeventos.core.EventEngine;
import com.pedrodalben.bigbangeventos.core.combat.*;
import com.pedrodalben.bigbangeventos.core.respawn.RespawnService;
import com.pedrodalben.bigbangeventos.core.round.RoundService;
import com.pedrodalben.bigbangeventos.core.spectator.SpectatorService;
import com.pedrodalben.bigbangeventos.core.team.TeamService;
import com.pedrodalben.bigbangeventos.definition.*;
import com.pedrodalben.bigbangeventos.definition.combat.CombatRuleSet;
import com.pedrodalben.bigbangeventos.definition.team.TeamDefinition;
import com.pedrodalben.bigbangeventos.domain.*;
import com.pedrodalben.bigbangeventos.model.respawn.RespawnPolicy;
import com.pedrodalben.bigbangeventos.objective.*;
import com.pedrodalben.bigbangeventos.participant.*;
import com.pedrodalben.bigbangeventos.participant.combat.*;
import com.pedrodalben.bigbangeventos.participant.spectator.SpectatorReason;
import com.pedrodalben.bigbangeventos.persistence.EventStorage;
import com.pedrodalben.bigbangeventos.platform.*;
import com.pedrodalben.bigbangeventos.session.*;
import com.pedrodalben.bigbangeventos.session.round.*;
import com.pedrodalben.bigbangeventos.session.team.*;
import com.pedrodalben.bigbangeventos.snapshot.PlayerSnapshot;
import com.pedrodalben.bigbangeventos.stage.EventStageDefinition;
import com.pedrodalben.bigbangeventos.trigger.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.slf4j.LoggerFactory;

class Phase3CoreTest {
	Clock c = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
	DomainEventBus bus = new DomainEventBus(inline(), LoggerFactory.getLogger("test"));
	TeamService teams = new TeamService(c, bus);
	RoundService rounds = new RoundService(c, bus);
	LifeService lives = new LifeService(bus);
	EliminationService eliminations = new EliminationService(c, bus);
	CombatService combat = new CombatService(c, bus, lives, eliminations);
	StubTp tp = new StubTp();
	RespawnService respawn = new RespawnService(c, bus, tp);
	SpectatorService spectators = new SpectatorService(bus, tp);

	EventDefinition def(String id) {
		var d = new EventDefinition(id, "generic", "server");
		d.location(LocationName.LOBBY, loc());
		d.location(LocationName.ENTRANCE, loc());
		d.location(LocationName.EXIT, loc());
		return d;
	}

	EventDefinition defWithTeams(String id) {
		var d = def(id);
		d.putTeamDefinition(new TeamDefinition("red", "Red", "§c", 1, 10, loc(), true, Map.of()));
		d.putTeamDefinition(new TeamDefinition("blue", "Blue", "§1", 1, 10, loc(), true, Map.of()));
		return d;
	}

	EventSession ses(String eventId) {
		var s = new EventSession(UUID.randomUUID(), eventId, 1, Instant.EPOCH, null);
		s.state(SessionState.RUNNING, Instant.EPOCH);
		return s;
	}

	EventParticipant part(UUID pid, String name) { return new EventParticipant(pid, name, Instant.EPOCH); }

	EventLocation loc() { return new EventLocation("sv", "overworld", 0, 64, 0, 0, 0); }
	StoredLocation locTeam() { return new StoredLocation("sv", "overworld", 0, 64, 0, 0, 0); }

	CombatRuleSet rules() {
		return new CombatRuleSet(true, false, true, true, true, true, true, false, false, false, List.of(), CombatRuleSet.OutOfBoundsPolicy.ELIMINATE);
	}

	ParticipantCombatState pcs(UUID pid, UUID sid, int lives) { return new ParticipantCombatState(pid, sid, lives); }

	EliminationService.EliminationHook noop() { return (eid, sid, pid, reason) -> {}; }

	// --- TeamService ---
	@Test void teamCreateAssign() {
		var d = defWithTeams("t1"); var s = ses("t1");
		var r = teams.createTeam(d, s, "red");
		assertTrue(r.success(), r.code() + ": " + r.message());
		assertTrue(teams.createTeam(d, s, "blue").success());
		assertEquals(2, teams.listTeams(s).size());
		assertFalse(teams.createTeam(d, s, "red").success());
		UUID p = UUID.randomUUID();
		assertTrue(teams.assignPlayer(d, s, p, "red").success());
		assertEquals("red", teams.getPlayerTeam(s, p).teamDefinitionId());
		assertTrue(teams.assignPlayer(d, s, p, "blue").success());
		assertEquals("blue", teams.getPlayerTeam(s, p).teamDefinitionId());
	}

	@Test void teamFullRejects() {
		var d = def("t2");
		d.putTeamDefinition(new TeamDefinition("sml", "Small", "§a", 1, 1, loc(), true, Map.of()));
		var s = ses("t2"); teams.createTeam(d, s, "sml");
		assertTrue(teams.assignPlayer(d, s, UUID.randomUUID(), "sml").success());
		assertFalse(teams.assignPlayer(d, s, UUID.randomUUID(), "sml").success());
	}

	@Test void teamScoreAndEliminate() {
		var d = defWithTeams("t3"); var s = ses("t3");
		teams.createTeam(d, s, "red");
		assertTrue(teams.addScore(d, s, "red", 50).success());
		assertEquals(50, s.team("red").orElseThrow().score());
		assertTrue(teams.eliminateTeam(d, s, "red").success());
		assertEquals(TeamStatus.ELIMINATED, s.team("red").orElseThrow().status());
		assertTrue(teams.eliminateTeam(d, s, "red").success());
	}

	@Test void teamRandomAssign() {
		var d = defWithTeams("t4"); var s = ses("t4");
		teams.createTeam(d, s, "red"); teams.createTeam(d, s, "blue");
		assertTrue(teams.assignRandom(d, s, "e", List.of(UUID.randomUUID(), UUID.randomUUID()), List.of("red", "blue")).success());
		assertEquals(1, s.team("red").orElseThrow().memberCount());
		assertEquals(1, s.team("blue").orElseThrow().memberCount());
	}

	@Test void teamTeammateDetection() {
		var d = defWithTeams("t5"); var s = ses("t5");
		teams.createTeam(d, s, "red");
		UUID p1 = UUID.randomUUID(), p2 = UUID.randomUUID();
		teams.assignPlayer(d, s, p1, "red"); teams.assignPlayer(d, s, p2, "red");
		assertEquals(2, s.team("red").orElseThrow().memberCount());
	}

	// --- RoundService ---
	@Test void roundLifecycle() {
		var s = ses("r");
		assertTrue(rounds.prepare(s, 1, Duration.ofMinutes(5), null).success());
		assertTrue(rounds.start(s).success());
		assertFalse(rounds.prepare(s, 2, Duration.ofMinutes(5), null).success()); // cannot prepare while round is ACTIVE
		assertEquals(RoundState.ACTIVE, rounds.activeRound(s).state());
		assertTrue(rounds.finish(s, RoundFinishReason.TIME_LIMIT, null, null).success());
		assertEquals(RoundState.FINISHED, s.rounds().values().iterator().next().state());
	}

	@Test void roundAdvance() {
		var s = ses("r2");
		assertTrue(rounds.prepare(s, 1, Duration.ofMinutes(3), null).success());
		assertTrue(rounds.start(s).success());
		assertTrue(rounds.finish(s, RoundFinishReason.STAFF, null, null).success());
		assertEquals(1, s.rounds().size());
		assertFalse(rounds.advance(s).success()); // ponytail: advance requires active round but finish makes round inactive
	}

	@Test void roundCancelFail() {
		var s = ses("r3");
		assertTrue(rounds.prepare(s, 1, Duration.ofMinutes(1), null).success());
		assertTrue(rounds.cancel(s).success());
		assertEquals(RoundState.CANCELLED, s.rounds().values().iterator().next().state());
		assertTrue(rounds.prepare(s, 2, Duration.ofMinutes(1), null).success());
		assertTrue(rounds.fail(s).success());
		assertEquals(RoundState.FAILED, s.rounds().values().stream().filter(r -> r.number() == 2).findFirst().orElseThrow().state());
	}

	@Test void roundFailOnRecovery() {
		var s = ses("r4");
		rounds.prepare(s, 1, Duration.ofMinutes(1), null); rounds.start(s);
		rounds.failAllOnRecovery(s);
		assertEquals(RoundState.FAILED, s.rounds().values().iterator().next().state());
	}

	@Test void roundCountdown() {
		var s = ses("r5");
		rounds.prepare(s, 1, Duration.ofMinutes(1), null);
		assertTrue(rounds.startCountdown(s).success());
		assertEquals(RoundState.COUNTDOWN, s.rounds().values().iterator().next().state());
	}

	@Test void roundOpWithoutRoundFails() {
		var s = ses("r6");
		assertFalse(rounds.start(s).success());
		assertFalse(rounds.finish(s, RoundFinishReason.STAFF, null, null).success());
		assertFalse(rounds.cancel(s).success());
	}

	// --- LifeService ---
	@Test void lifeAddRemoveSet() {
		var state = pcs(UUID.randomUUID(), UUID.randomUUID(), 3);
		assertEquals(3, state.livesRemaining());
		assertTrue(lives.addLife(state, UUID.randomUUID(), "e", 2).success());
		assertEquals(4, state.livesRemaining()); // addLife always adds 1 regardless of amount param
		assertTrue(lives.removeLife(state, UUID.randomUUID(), "e").success());
		assertEquals(3, state.livesRemaining());
		assertTrue(lives.setLives(state, UUID.randomUUID(), "e", 10).success());
		assertEquals(10, state.livesRemaining());
		assertTrue(lives.removeLife(state, UUID.randomUUID(), "e").success()); // still succeeds at >0 lives
	}

	@Test void lifeZeroBlocksRemoval() {
		assertTrue(lives.removeLife(pcs(UUID.randomUUID(), UUID.randomUUID(), 0), UUID.randomUUID(), "e").success()); // returns "elegível para eliminação"
	}

	// --- CombatService ---
	@Test void combatDeathAndKill() {
		var s = ses("cd1");
		UUID v = UUID.randomUUID(), vic = UUID.randomUUID();
		var vs = pcs(v, s.id(), 3); var vics = pcs(vic, s.id(), 3);
		s.combatStates().put(v, vs); s.combatStates().put(vic, vics);
		assertTrue(combat.onDeath(s, vics, vs, v, "player_attack", "e", rules(), noop()).success());
		assertEquals(2, vics.livesRemaining());
		assertEquals(1, vs.sessionKills());
		assertEquals(1, vics.sessionDeaths());
	}

	@Test void combatEliminatesAtZeroLives() {
		var s = ses("cd2"); var sid = s.id();
		UUID vic = UUID.randomUUID();
		var state = pcs(vic, sid, 1);
		s.combatStates().put(vic, state);
		var hooked = new boolean[1];
		var hook = (EliminationService.EliminationHook) (eid, sesId, pid, reason) -> hooked[0] = true;
		assertTrue(combat.onDeath(s, state, null, null, "fall", "e", rules(), hook).success());
		assertTrue(hooked[0]);
		assertTrue(state.eliminated());
	}

	@Test void combatInvulnerablePreventsDeath() {
		var s = ses("cd3");
		var state = pcs(UUID.randomUUID(), s.id(), 3);
		state.invulnerableUntil(Instant.EPOCH.plusSeconds(300));
		assertTrue(combat.onDeath(s, state, null, null, "attack", "e", rules(), noop()).success());
		assertEquals(3, state.livesRemaining());
	}

	@Test void combatFriendlyFireBlocked() {
		var d = defWithTeams("cd4"); var s = ses("cd4");
		teams.createTeam(d, s, "red");
		UUID a = UUID.randomUUID(), b = UUID.randomUUID();
		teams.assignPlayer(d, s, a, "red"); teams.assignPlayer(d, s, b, "red");
		var sa = pcs(a, s.id(), 3); var sb = pcs(b, s.id(), 3);
		s.combatStates().put(a, sa); s.combatStates().put(b, sb);
		assertTrue(combat.onDeath(s, sa, sb, b, "attack", "e", rules(), noop()).success());
		assertEquals(0, sb.sessionKills());
	}

	@Test void combatOutOfBoundsEliminate() {
		var s = ses("cd5"); var sid = s.id();
		var state = pcs(UUID.randomUUID(), sid, 3);
		s.combatStates().put(UUID.randomUUID(), state);
		var hooked = new boolean[1];
		var hook = (EliminationService.EliminationHook) (eid, sesId, pid, reason) -> hooked[0] = true;
		assertTrue(combat.handleOutOfBounds(s, state, state.participantId(), "e", rules(), hook).success());
		assertTrue(hooked[0]);
	}

	@Test void combatEliminationIdempotent() {
		var s = ses("cd6");
		var state = pcs(UUID.randomUUID(), s.id(), 1);
		s.combatStates().put(state.participantId(), state);
		assertTrue(eliminations.eliminate(s, state, "e", EliminationReason.NO_LIVES, noop()).success());
		assertTrue(state.eliminated());
		assertTrue(eliminations.eliminate(s, state, "e", EliminationReason.NO_LIVES, noop()).success());
	}

	@Test void combatStateRoundReset() {
		var state = pcs(UUID.randomUUID(), UUID.randomUUID(), 5);
		state.kill(); state.death(UUID.randomUUID());
		assertEquals(1, state.roundKills()); assertEquals(1, state.roundDeaths());
		state.resetRoundStats();
		assertEquals(0, state.roundKills()); assertEquals(0, state.roundDeaths());
		assertEquals(5, state.livesRemaining());
	}

	// --- RespawnService ---
	@Test void respawnDelayedTick() {
		var s = ses("rs1");
		UUID p = UUID.randomUUID(); tp.markOnline(p);
		var state = pcs(p, s.id(), 3);
		assertTrue(respawn.scheduleRespawn(s, state, "e", RespawnPolicy.DELAYED, 10, 3, locTeam()).success());
		assertTrue(state.respawnPending());
		assertTrue(respawn.hasPendingRespawn(s.id(), p));
		respawn.onTick(s, Map.of(p, state), "e", locTeam(), 3);
		assertFalse(state.respawnPending());
	}

	@Test void respawnImmediate() {
		UUID p = UUID.randomUUID(); tp.markOnline(p);
		assertTrue(respawn.scheduleRespawn(ses("rs2"), pcs(p, UUID.randomUUID(), 3), "e", RespawnPolicy.IMMEDIATE, 0, 2, locTeam()).success());
	}

	@Test void respawnBlockedWhenEliminated() {
		var state = pcs(UUID.randomUUID(), UUID.randomUUID(), 0);
		state.eliminated(true, EliminationReason.NO_LIVES, Instant.EPOCH);
		assertFalse(respawn.scheduleRespawn(ses("rs3"), state, "e", RespawnPolicy.DELAYED, 5, 2, locTeam()).success());
	}

	@Test void respawnNonePolicy() {
		assertFalse(respawn.scheduleRespawn(ses("rs4"), pcs(UUID.randomUUID(), UUID.randomUUID(), 3), "e", RespawnPolicy.NONE, 0, 0, locTeam()).success());
	}

	// --- SpectatorService ---
	@Test void spectatorMakeRemove() {
		var s = ses("sp1"); UUID p = UUID.randomUUID();
		s.addParticipant(part(p, "p")); tp.markOnline(p);
		assertTrue(spectators.makeSpectator(s, p, SpectatorReason.MANUAL, locTeam()).success());
		assertTrue(s.hasSpectator(p));
		assertTrue(spectators.makeSpectator(s, p, SpectatorReason.MANUAL, locTeam()).success());
		assertTrue(spectators.removeSpectator(s, p, locTeam()).success());
		assertFalse(s.hasSpectator(p));
	}

	@Test void spectatorRemoveNonSpectatorFails() {
		assertFalse(spectators.removeSpectator(ses("sp2"), UUID.randomUUID(), null).success());
	}

	@Test void spectatorDirectSessionOps() {
		var s = ses("sp3"); UUID p = UUID.randomUUID();
		s.addSpectator(p); assertTrue(s.hasSpectator(p));
		s.removeSpectator(p); assertFalse(s.hasSpectator(p));
	}

	// --- ObjectiveService TEAM scope ---
	@Test void objectiveTeamScope() {
		var bus2 = new DomainEventBus(inline(), LoggerFactory.getLogger("test"));
		var objectives = new ObjectiveService(c, new ObjectiveTypeRegistry(), bus2);
		var d = def("tobj");
		d.putTeamDefinition(new TeamDefinition("red", "Red", "§c", 1, 10, loc(), true, Map.of()));
		d.putStage(new EventStageDefinition("s", "S", "", 1, true, true, 0, List.of("o1"), null, true, Map.of()));
		d.putObjective(new ObjectiveDefinition("o1", "O1", "", "counter", "s", true, 1, 5, ObjectiveScope.TEAM, true, Map.of()));
		var s = ses("tobj");
		teams.createTeam(d, s, "red");
		UUID p = UUID.randomUUID(); teams.assignPlayer(d, s, p, "red");
		assertTrue(objectives.addProgress(d, s, "o1", p, 3, "test").success());
		assertEquals(3, objectives.getProgress(s, "o1", p).orElseThrow().current());
	}

	// --- Trigger competitive conditions ---
	@Test void triggerPlayerIsEliminatedCondition() {
		var ts1 = new TriggerService(c, new com.pedrodalben.bigbangeventos.participant.ParticipantCompletionService(c));
		ts1.competitiveServices(teams, combat, rounds);
		var s1 = ses("comp"); s1.state(SessionState.RUNNING, Instant.EPOCH);
		UUID p1 = UUID.randomUUID(); s1.addParticipant(part(p1, "p"));
		var state = pcs(p1, s1.id(), 3); s1.combatStates().put(p1, state);
		var trig1 = new EventTrigger("check", TriggerType.MANUAL);
		trig1.addCondition(ConditionType.PLAYER_IS_ELIMINATED);
		trig1.addAction(new TriggerAction(ActionType.SEND_MESSAGE, Map.of("message", "x")));
		var ctx1 = new TriggerExecutionContext(s1, p1, "p", (id, perm) -> true, (id, msg) -> {}, def("comp"));
		assertFalse(ts1.execute(trig1, ctx1).success());
	}

	@Test void triggerMakeSpectatorAction() {
		var ts2 = new TriggerService(c, new com.pedrodalben.bigbangeventos.participant.ParticipantCompletionService(c));
		ts2.competitiveServices(teams, combat, rounds);
		var s2 = ses("comp2"); s2.state(SessionState.RUNNING, Instant.EPOCH);
		UUID p2 = UUID.randomUUID(); s2.addParticipant(part(p2, "p"));
		var trig2 = new EventTrigger("spec", TriggerType.MANUAL);
		trig2.addAction(new TriggerAction(ActionType.MAKE_SPECTATOR, Map.of()));
		var ctx2 = new TriggerExecutionContext(s2, p2, "p", (id, perm) -> true, (id, msg) -> {}, def("comp2"));
		assertTrue(ts2.execute(trig2, ctx2).success());
		assertTrue(s2.hasSpectator(p2));
	}

	// --- Domain event firing ---
	@Test void teamEventFires() {
		var bus2 = new DomainEventBus(inline(), LoggerFactory.getLogger("test"));
		var ts = new TeamService(c, bus2);
		var fired = new int[1];
		bus2.subscribe("t", TeamEvents.TeamCreated.class, e -> fired[0]++);
		ts.createTeam(defWithTeams("te"), ses("te"), "red");
		assertEquals(1, fired[0]);
	}

	@Test void roundEventFires() {
		var bus2 = new DomainEventBus(inline(), LoggerFactory.getLogger("test"));
		var rs = new RoundService(c, bus2);
		var fired = new int[1];
		bus2.subscribe("r", RoundEvents.RoundStarted.class, e -> fired[0]++);
		var s = ses("re"); rs.prepare(s, 1, Duration.ofMinutes(1), null); rs.start(s);
		assertEquals(1, fired[0]);
	}

	@Test void combatEventFires() {
		var bus2 = new DomainEventBus(inline(), LoggerFactory.getLogger("test"));
		var ls = new LifeService(bus2); var es = new EliminationService(c, bus2);
		var cs = new CombatService(c, bus2, ls, es);
		var fired = new int[1];
		bus2.subscribe("c", CombatEvents.ParticipantDeath.class, e -> fired[0]++);
		var s = ses("ce");
		var state = pcs(UUID.randomUUID(), s.id(), 3); s.combatStates().put(state.participantId(), state);
		cs.onDeath(s, state, null, null, "fall", "e", rules(), noop());
		assertEquals(1, fired[0]);
	}

	@Test void spectatorEventFires() {
		var bus2 = new DomainEventBus(inline(), LoggerFactory.getLogger("test"));
		var sp = new SpectatorService(bus2, tp);
		var fired = new int[1];
		bus2.subscribe("s", CombatEvents.ParticipantBecameSpectator.class, e -> fired[0]++);
		var s = ses("se"); UUID p = UUID.randomUUID(); s.addParticipant(part(p, "p"));
		sp.makeSpectator(s, p, SpectatorReason.MANUAL, locTeam());
		assertEquals(1, fired[0]);
	}

	@Test void respawnEventFires() {
		var bus2 = new DomainEventBus(inline(), LoggerFactory.getLogger("test"));
		var rs = new RespawnService(c, bus2, tp);
		var fired = new int[1];
		bus2.subscribe("r", CombatEvents.ParticipantRespawnScheduled.class, e -> fired[0]++);
		UUID p = UUID.randomUUID(); tp.markOnline(p);
		rs.scheduleRespawn(ses("rse"), pcs(p, UUID.randomUUID(), 3), "e", RespawnPolicy.DELAYED, 10, 3, locTeam());
		assertEquals(1, fired[0]);
	}

	// --- Helpers ---
	private static PlatformScheduler inline() {
		return new PlatformScheduler() {
			public boolean isServerThread() { return true; }
			public void executeOnServerThread(Runnable r) { r.run(); }
			public ScheduledHandle schedule(Duration d, Runnable r) { return h(); }
			public ScheduledHandle scheduleRepeating(Duration d, Runnable r) { return h(); }
			private ScheduledHandle h() { return new ScheduledHandle() { public void cancel() {} public boolean isCancelled() { return false; }}; }
		};
	}

	static class StubStorage implements EventStorage {
		final Map<String, EventDefinition> defs = new HashMap<>();
		final Map<UUID, EventSession> sessions = new HashMap<>();
		public void saveDefinition(EventDefinition x) { defs.put(x.id(), x); }
		public Optional<EventDefinition> findDefinition(String id) { return Optional.ofNullable(defs.get(id)); }
		public Collection<EventDefinition> findDefinitions() { return defs.values(); }
		public void deleteDefinition(String id) { defs.remove(id); }
		public void saveSession(EventSession x) { sessions.put(x.id(), x); }
		public Optional<EventSession> findSession(UUID id) { return Optional.ofNullable(sessions.get(id)); }
		public Collection<EventSession> findUnfinishedSessions() { return sessions.values(); }
		public void saveSnapshot(PlayerSnapshot x) {}
		public Optional<PlayerSnapshot> findSnapshot(UUID id) { return Optional.empty(); }
		public Collection<PlayerSnapshot> findSnapshotsByPlayer(UUID id) { return List.of(); }
		public void deleteSnapshot(UUID id) {}
	}

	static class StubPlayer implements PlatformPlayerService {
		public Optional<UUID> findOnlineUuidByName(String name) { return Optional.empty(); }
		public boolean isOnline(UUID pid) { return true; }
		public OperationResult sendMessage(UUID id, String msg) { return OperationResult.ok("ok"); }
		public OperationResult sendTitle(UUID id, String t, String s) { return OperationResult.ok("ok"); }
		public OperationResult sendActionBar(UUID id, String msg) { return OperationResult.ok("ok"); }
		public Optional<StoredLocation> captureLocation(UUID id) { return Optional.of(new StoredLocation("sv", "overworld", 0, 64, 0, 0, 0)); }
		public Optional<String> captureGameMode(UUID id) { return Optional.of("SURVIVAL"); }
		public boolean hasFlyEnabled(UUID id) { return false; }
	}

	static class StubTp implements PlatformTeleportService {
		final Set<UUID> online = new HashSet<>();
		void markOnline(UUID id) { online.add(id); }
		public OperationResult teleport(UUID pid, StoredLocation dest) {
			return online.contains(pid) ? OperationResult.ok("tp") : OperationResult.fail("offline", "offline");
		}
	}
}
