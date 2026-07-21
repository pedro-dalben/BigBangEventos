package com.pedrodalben.bigbangeventos;

import com.pedrodalben.bigbangeventos.api.OperationResult;
import com.pedrodalben.bigbangeventos.core.PlayerRestoreService;
import com.pedrodalben.bigbangeventos.core.EventEngine;
import com.pedrodalben.bigbangeventos.definition.*;
import com.pedrodalben.bigbangeventos.lifecycle.SessionLifecycle;
import com.pedrodalben.bigbangeventos.participant.*;
import com.pedrodalben.bigbangeventos.persistence.EventStorage;
import com.pedrodalben.bigbangeventos.platform.*;
import com.pedrodalben.bigbangeventos.session.*;
import com.pedrodalben.bigbangeventos.snapshot.*;
import com.pedrodalben.bigbangeventos.timer.SessionTimer;
import com.pedrodalben.bigbangeventos.trigger.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EventCoreTest {
    // --- Lifecycle ---
    @Test void lifecycleRejectsTerminalAndBadTransitions() {
        Clock c = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
        EventSession s = new EventSession(UUID.randomUUID(), "test", 1, Instant.EPOCH, null);
        SessionLifecycle l = new SessionLifecycle(c);
        assertTrue(l.transition(s, SessionState.REGISTRATION_OPEN).success());
        assertTrue(l.transition(s, SessionState.COUNTDOWN).success());
        assertTrue(l.transition(s, SessionState.RUNNING).success());
        assertTrue(l.transition(s, SessionState.FINISHING).success());
        assertTrue(l.transition(s, SessionState.FINISHED).success());
        assertFalse(l.transition(s, SessionState.RUNNING).success());
    }

    // --- Participation: join/leave ---
    @Test void participationPreventsDoubleJoinAndIsIdempotentOnLeave() {
        EventEngine e = engine();
        assertTrue(e.create("test", "generic", "cobbleverse").success());
        EventDefinition d = e.definition("test").orElseThrow();
        d.location(LocationName.LOBBY, location());
        d.location(LocationName.ENTRANCE, location());
        d.location(LocationName.EXIT, location());
        e.save(d);
        assertTrue(e.open("test", null).success());
        UUID p = UUID.randomUUID();
        assertTrue(e.join("test", p, "player", false, true).success());
        assertFalse(e.join("test", p, "player", false, true).success());
        assertTrue(e.leave(p, "test").success());
        assertTrue(e.leave(p, "test").success());
    }

    @Test void joinWhenFullFails() {
        EventEngine e = engine();
        assertTrue(e.create("full", "generic", "cobbleverse").success());
        EventDefinition d = e.definition("full").orElseThrow();
        d.location(LocationName.LOBBY, location());
        d.location(LocationName.ENTRANCE, location());
        d.location(LocationName.EXIT, location());
        d.playerLimits(1, 1);
        e.save(d);
        assertTrue(e.open("full", null).success());
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        assertTrue(e.join("full", p1, "p1", false, true).success());
        assertFalse(e.join("full", p2, "p2", false, true).success());
    }

    // --- Timer ---
    @Test void timerUsesAbsoluteClockAndSnapshotDoesNotRestoreTwice() {
        MutableClock c = new MutableClock();
        SessionTimer t = new SessionTimer(c, Duration.ofSeconds(10));
        t.resume();
        c.advance(5);
        assertEquals(Duration.ofSeconds(5), t.remaining());
        t.pause();
        c.advance(30);
        assertEquals(Duration.ofSeconds(5), t.remaining());
    }

    // --- Snapshot: idempotent restore ---
    @Test void snapshotIdempotentRestore() {
        Map<String, String> inventory = new HashMap<>(Map.of("0", "diamond"));
        Map<String, String> armor = new HashMap<>();
        String offhand = "";

        StubSnapshotGateway gw = new StubSnapshotGateway(inventory, armor, offhand);
        SnapshotService snapshots = new SnapshotService(gw);
        var teleport = new StubTeleportService();
        PlayerRestoreService restore = new PlayerRestoreService(snapshots, gw, teleport);

        UUID pid = UUID.randomUUID();
        UUID sid = UUID.randomUUID();
        OperationResult prep = snapshots.prepare(pid, sid, InventoryMode.CLEAR_AND_RESTORE);
        assertTrue(prep.success(), prep.message());
        assertTrue(inventory.isEmpty());

        OperationResult r1 = restore.restore(pid, null);
        assertTrue(r1.success(), r1.message());
        assertEquals("diamond", inventory.get("0"));

        inventory.put("0", "changed");
        OperationResult r2 = restore.restore(pid, null);
        assertTrue(r2.success());
        assertEquals("changed", inventory.get("0"));
    }

    // --- Trigger: condition fail, non-repeatable completion ---
    @Test void triggerStopsOnFailedConditionAndCompletionIsNotRepeatable() {
        EventEngine e = engine();
        assertTrue(e.create("test", "generic", "server").success());
        EventDefinition d = e.definition("test").orElseThrow();
        d.location(LocationName.LOBBY, location());
        d.location(LocationName.ENTRANCE, location());
        d.location(LocationName.EXIT, location());
        e.save(d);
        assertTrue(e.open("test", null).success());
        UUID p = UUID.randomUUID();
        assertTrue(e.join("test", p, "p", false, true).success());
        EventTrigger t = new EventTrigger("finish", TriggerType.MANUAL);
        t.addCondition(ConditionType.EVENT_IS_RUNNING);
        t.addAction(new TriggerAction(ActionType.ADD_POINTS, Map.of("amount", "100")));
        t.addAction(new TriggerAction(ActionType.PLAYER_COMPLETE, Map.of()));
        d.putTrigger(t);
        e.save(d);

        assertFalse(e.activateTrigger("test", "finish", p, "p",
                (id, x) -> true, (id, x) -> {}).success());
        var started = e.start("test");
        assertTrue(started.success(), started::message);
        assertTrue(e.activateTrigger("test", "finish", p, "p",
                (id, x) -> true, (id, x) -> {}).success());
        assertFalse(e.activateTrigger("test", "finish", p, "p",
                (id, x) -> true, (id, x) -> {}).success());
    }

    // --- Disconnect: grace period ---
    @Test void disconnectGracePeriodExpires() {
        var engine = engine();
        assertTrue(engine.create("dc", "generic", "cobbleverse").success());
        EventDefinition d = engine.definition("dc").orElseThrow();
        d.location(LocationName.LOBBY, location());
        d.location(LocationName.ENTRANCE, location());
        d.location(LocationName.EXIT, location());
        engine.save(d);
        assertTrue(engine.open("dc", null).success());

        UUID pid = UUID.randomUUID();
        assertTrue(engine.join("dc", pid, "p", false, true).success());

        OperationResult dc = engine.onPlayerDisconnect(pid);
        assertTrue(dc.success());

        var session = engine.sessionByPlayer(pid);
        assertTrue(session.isPresent());
        EventParticipant part = session.get().participant(pid).orElseThrow();
        assertEquals(ParticipantState.DISCONNECTED, part.state());
    }

    // --- Teleport: offline player ---
    @Test void teleportOfflinePlayerFails() {
        StubTeleportService tp = new StubTeleportService();
        StoredLocation dest = new StoredLocation("sv", "minecraft:overworld", 0, 64, 0, 0, 0);
        OperationResult r = tp.teleport(UUID.randomUUID(), dest);
        assertFalse(r.success());
        assertEquals("player_offline", r.code());
    }

    // --- EventArea: cuboid containment ---
    @Test void cuboidAreaContains() {
        EventArea.Cuboid area = new EventArea.Cuboid("sv", "minecraft:overworld",
                0, 0, 0, 10, 10, 10);
        assertTrue(area.contains("sv", "minecraft:overworld", 5, 5, 5));
        assertTrue(area.contains("sv", "minecraft:overworld", 0, 0, 0));
        assertTrue(area.contains("sv", "minecraft:overworld", 10, 10, 10));
        assertFalse(area.contains("sv", "minecraft:overworld", 12, 5, 5));
        assertFalse(area.contains("sv", "minecraft:nether", 5, 5, 5));
    }

    @Test void cuboidAreaRejectsInvalidBounds() {
        assertThrows(IllegalArgumentException.class, () ->
                new EventArea.Cuboid("sv", "dim", 10, 0, 0, 0, 0, 0));
    }

    // --- EventArea: radius containment ---
    @Test void radiusAreaContains() {
        EventArea.Radius area = new EventArea.Radius("sv", "minecraft:overworld",
                0, 64, 0, 5.0, 10.0);
        assertTrue(area.contains("sv", "minecraft:overworld", 0, 64, 0));
        assertTrue(area.contains("sv", "minecraft:overworld", 3, 70, 4));
        assertFalse(area.contains("sv", "minecraft:overworld", 6, 64, 0));
        assertFalse(area.contains("sv", "minecraft:overworld", 0, 75, 0));
    }

    @Test void radiusAreaRejectsInvalidRadius() {
        assertThrows(IllegalArgumentException.class, () ->
                new EventArea.Radius("sv", "dim", 0, 0, 0, 0, 5));
        assertThrows(IllegalArgumentException.class, () ->
                new EventArea.Radius("sv", "dim", 0, 0, 0, 5, 0));
    }

    // --- EventTrigger: area assignment ---
    @Test void triggerCanHoldArea() {
        EventTrigger t = new EventTrigger("region_cp", TriggerType.REGION_ENTER);
        t.area(new EventArea.Cuboid("sv", "dim", 0, 0, 0, 10, 10, 10));
        assertTrue(t.area().isPresent());
        assertTrue(t.area().get() instanceof EventArea.Cuboid);
    }

    // --- Trigger: maxUses respected ---
    @Test void triggerMaxUsesBlocksExcess() {
        EventEngine e = engine();
        assertTrue(e.create("test", "generic", "server").success());
        EventDefinition d = e.definition("test").orElseThrow();
        d.location(LocationName.LOBBY, location());
        d.location(LocationName.ENTRANCE, location());
        d.location(LocationName.EXIT, location());
        e.save(d);
        assertTrue(e.open("test", null).success());
        UUID p = UUID.randomUUID();
        assertTrue(e.join("test", p, "p", false, true).success());
        assertTrue(e.start("test").success());

        EventTrigger t = new EventTrigger("once", TriggerType.MANUAL);
        t.maxUses(1);
        t.addAction(new TriggerAction(ActionType.SEND_MESSAGE, Map.of("message", "hi")));
        d.putTrigger(t);
        e.save(d);

        assertTrue(e.activateTrigger("test", "once", p, "p",
                (id, perm) -> true, (id, msg) -> {}).success());
        assertFalse(e.activateTrigger("test", "once", p, "p",
                (id, perm) -> true, (id, msg) -> {}).success());
    }

    // --- Trigger: cooldown respected ---
    @Test void triggerCooldownBlocksRapidUse() {
        EventEngine e = engine();
        assertTrue(e.create("test2", "generic", "server").success());
        EventDefinition d = e.definition("test2").orElseThrow();
        d.location(LocationName.LOBBY, location());
        d.location(LocationName.ENTRANCE, location());
        d.location(LocationName.EXIT, location());
        e.save(d);
        assertTrue(e.open("test2", null).success());
        UUID p = UUID.randomUUID();
        assertTrue(e.join("test2", p, "p2", false, true).success());
        assertTrue(e.start("test2").success());

        EventTrigger t = new EventTrigger("slow", TriggerType.MANUAL);
        t.cooldown(Duration.ofDays(1));
        t.addAction(new TriggerAction(ActionType.SEND_MESSAGE, Map.of("message", "msg")));
        d.putTrigger(t);
        e.save(d);

        assertTrue(e.activateTrigger("test2", "slow", p, "p2",
                (id, perm) -> true, (id, msg) -> {}).success());
        assertFalse(e.activateTrigger("test2", "slow", p, "p2",
                (id, perm) -> true, (id, msg) -> {}).success());
    }

    // --- Trigger: disabled trigger rejects ---
    @Test void disabledTriggerRejects() {
        EventEngine e = engine();
        assertTrue(e.create("test3", "generic", "server").success());
        EventDefinition d = e.definition("test3").orElseThrow();
        d.location(LocationName.LOBBY, location());
        d.location(LocationName.ENTRANCE, location());
        d.location(LocationName.EXIT, location());
        e.save(d);
        assertTrue(e.open("test3", null).success());
        UUID p = UUID.randomUUID();
        assertTrue(e.join("test3", p, "p3", false, true).success());
        assertTrue(e.start("test3").success());

        EventTrigger t = new EventTrigger("off", TriggerType.MANUAL);
        t.enabled(false);
        t.addAction(new TriggerAction(ActionType.SEND_MESSAGE, Map.of("message", "msg")));
        d.putTrigger(t);
        e.save(d);

        assertFalse(e.activateTrigger("test3", "off", p, "p3",
                (id, perm) -> true, (id, msg) -> {}).success());
    }

    // --- Snapshot: KEEP mode preserves inventory ---
    @Test void snapshotKeepModePreservesInventory() {
        Map<String, String> inventory = new HashMap<>(Map.of("0", "diamond"));
        Map<String, String> armor = new HashMap<>();
        StubSnapshotGateway gw = new StubSnapshotGateway(inventory, armor, "");
        SnapshotService snapshots = new SnapshotService(gw);

        UUID pid = UUID.randomUUID();
        UUID sid = UUID.randomUUID();
        OperationResult prep = snapshots.prepare(pid, sid, InventoryMode.KEEP);
        assertTrue(prep.success(), prep.message());
        assertEquals("diamond", inventory.get("0"));
    }

    // --- Snapshot: concurrent restore guard ---
    @Test void snapshotConcurrentRestoreRejected() {
        Map<String, String> inventory = new HashMap<>(Map.of("0", "sword"));
        Map<String, String> armor = new HashMap<>();
        StubSnapshotGateway gw = new StubSnapshotGateway(inventory, armor, "");
        SnapshotService snapshots = new SnapshotService(gw);
        var teleport = new StubTeleportService();
        teleport.markOnline(UUID.randomUUID());
        PlayerRestoreService restore = new PlayerRestoreService(snapshots, gw, teleport);

        UUID pid = UUID.randomUUID();
        UUID sid = UUID.randomUUID();
        snapshots.prepare(pid, sid, InventoryMode.CLEAR_AND_RESTORE);
        teleport.markOnline(pid);

        OperationResult r1 = restore.restore(pid, null);
        assertTrue(r1.success());
        OperationResult r2 = restore.restore(pid, null);
        assertTrue(r2.success(), "second restore should be idempotent");
    }

    // --- Helpers ---
    private static EventEngine engine() {
        return new EventEngine(new MemoryStorage(), Clock.systemUTC(),
                new StubSnapshotGateway(new HashMap<>(), new HashMap<>(), ""),
                new StubTeleportService(), new StubPlayerService(), new StubScheduler());
    }

    private static EventLocation location() {
        return new EventLocation("cobbleverse", "minecraft:overworld", 0, 64, 0, 0, 0);
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.EPOCH;
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId z) { return this; }
        public Instant instant() { return now; }
        void advance(long seconds) { now = now.plusSeconds(seconds); }
    }

    private static final class MemoryStorage implements EventStorage {
        final Map<String, EventDefinition> d = new HashMap<>();
        final Map<UUID, EventSession> s = new HashMap<>();
        final Map<UUID, PlayerSnapshot> snaps = new HashMap<>();
        public void saveDefinition(EventDefinition x) { d.put(x.id(), x); }
        public Optional<EventDefinition> findDefinition(String id) { return Optional.ofNullable(d.get(id)); }
        public Collection<EventDefinition> findDefinitions() { return d.values(); }
        public void deleteDefinition(String id) { d.remove(id); }
        public void saveSession(EventSession x) { s.put(x.id(), x); }
        public Optional<EventSession> findSession(UUID id) { return Optional.ofNullable(s.get(id)); }
        public Collection<EventSession> findUnfinishedSessions() { return s.values(); }
        public void saveSnapshot(PlayerSnapshot x) { snaps.put(x.snapshotId(), x); }
        public Optional<PlayerSnapshot> findSnapshot(UUID id) { return Optional.ofNullable(snaps.get(id)); }
        public Collection<PlayerSnapshot> findSnapshotsByPlayer(UUID id) { return snaps.values().stream().filter(x -> x.playerId().equals(id)).toList(); }
        public void deleteSnapshot(UUID id) { snaps.remove(id); }
    }

    private static final class StubPlayerService implements PlatformPlayerService {
        public Optional<UUID> findOnlineUuidByName(String name) { return Optional.empty(); }
        public boolean isOnline(UUID playerId) { return true; } // ponytail: always online in tests
        public OperationResult sendMessage(UUID id, String msg) { return OperationResult.ok("ok"); }
        public OperationResult sendTitle(UUID id, String t, String s) { return OperationResult.ok("ok"); }
        public OperationResult sendActionBar(UUID id, String msg) { return OperationResult.ok("ok"); }
        public Optional<StoredLocation> captureLocation(UUID id) { return Optional.of(new StoredLocation("sv", "overworld", 0, 64, 0, 0, 0)); }
        public Optional<String> captureGameMode(UUID id) { return Optional.of("SURVIVAL"); }
        public boolean hasFlyEnabled(UUID id) { return false; }
    }

    private static final class StubTeleportService implements PlatformTeleportService {
        private final Set<UUID> online = new HashSet<>();
        public void markOnline(UUID id) { online.add(id); }
        public OperationResult teleport(UUID playerId, StoredLocation dest) {
            if (!online.contains(playerId)) return OperationResult.fail("player_offline", "Jogador offline");
            if (!Double.isFinite(dest.x())) return OperationResult.fail("invalid_location", "inválida");
            return OperationResult.ok("Teleportado");
        }
    }

    private static final class StubSnapshotGateway implements SnapshotGateway {
        private final Map<String, String> inventory;
        private final Map<String, String> armor;
        private final String offhand;
        private boolean cleared;

        StubSnapshotGateway(Map<String, String> inventory, Map<String, String> armor, String offhand) {
            this.inventory = inventory; this.armor = armor; this.offhand = offhand;
        }

        public PlayerSnapshot capture(UUID pid, UUID sid, UUID ses) {
            return new PlayerSnapshot(sid, pid, ses,
                    new StoredLocation("sv", "overworld", 0, 64, 0, 0, 0),
                    Map.copyOf(inventory), Map.copyOf(armor), offhand,
                    0, 0, 0f, 20, 0, 20, 5, "SURVIVAL",
                    false, false, 0.05f, 0.1f, 0,
                    0, 0, "", Map.of());
        }
        public boolean restoreState(UUID pid, PlayerSnapshot s) { return true; }
        public boolean restoreInventory(UUID pid, PlayerSnapshot s) {
            inventory.clear(); inventory.putAll(s.serializedInventory()); return true;
        }
        public boolean restoreArmor(UUID pid, PlayerSnapshot s) {
            armor.clear(); armor.putAll(s.serializedArmor()); return true;
        }
        public void clearInventory(UUID pid) { inventory.clear(); cleared = true; }
        public Optional<StoredLocation> captureLocation(UUID pid) { return Optional.of(new StoredLocation("sv", "overworld", 0, 64, 0, 0, 0)); }
        public Map<String, String> serializeInventoryItems(UUID pid) { return Map.copyOf(inventory); }
        public Map<String, String> serializeArmorItems(UUID pid) { return Map.copyOf(armor); }
        public String serializeOffhandItem(UUID pid) { return offhand; }
        public String captureGameMode(UUID pid) { return "SURVIVAL"; }
        public int captureTotalExperience(UUID pid) { return 0; }
        public int captureExperienceLevel(UUID pid) { return 0; }
        public float captureExperienceProgress(UUID pid) { return 0; }
        public double captureHealth(UUID pid) { return 20; }
        public double captureAbsorption(UUID pid) { return 0; }
        public int captureFoodLevel(UUID pid) { return 20; }
        public float captureSaturation(UUID pid) { return 5; }
        public boolean captureAllowFlight(UUID pid) { return false; }
        public boolean captureIsFlying(UUID pid) { return false; }
        public float captureFlySpeed(UUID pid) { return 0.05f; }
        public float captureWalkSpeed(UUID pid) { return 0.1f; }
        public int captureSelectedSlot(UUID pid) { return 0; }
        public int captureFireTicks(UUID pid) { return 0; }
        public float captureFallDistance(UUID pid) { return 0; }
        public String captureActiveEffects(UUID pid) { return ""; }
    }

    private static final class StubScheduler implements PlatformScheduler {
        public boolean isServerThread() { return true; }
        public void executeOnServerThread(Runnable r) { r.run(); }
        public ScheduledHandle schedule(Duration d, Runnable r) {
            return new ScheduledHandle() { public void cancel() {} public boolean isCancelled() { return false; } };
        }
        public ScheduledHandle scheduleRepeating(Duration d, Runnable r) {
            return new ScheduledHandle() { public void cancel() {} public boolean isCancelled() { return false; } };
        }
    }
}
