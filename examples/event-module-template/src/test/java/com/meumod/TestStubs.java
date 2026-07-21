package com.meumod;

import com.pedrodalben.bigbangeventos.engine.EventEngine;
import com.pedrodalben.bigbangeventos.persistence.EventStorage;
import com.pedrodalben.bigbangeventos.platform.PlatformScheduler;
import com.pedrodalben.bigbangeventos.platform.PlatformTeleportService;
import com.pedrodalben.bigbangeventos.platform.PlatformPlayerService;
import com.pedrodalben.bigbangeventos.snapshot.SnapshotGateway;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

class TestStubs {
    static EventEngine createEngine() {
        return new EventEngine(
            new MemoryStorage(),
            Clock.systemUTC(),
            new StubSnapshotGateway(),
            new StubTeleportService(),
            new StubPlayerService(),
            new StubScheduler()
        );
    }

    static class MemoryStorage implements EventStorage {
        private final Map<String, String> store = new HashMap<>();
        public void save(String key, String data) { store.put(key, data); }
        public String load(String key) { return store.get(key); }
        public void delete(String key) { store.remove(key); }
        @Override public Map<String, String> loadAll() { return new HashMap<>(store); }
    }

    static class StubSnapshotGateway implements SnapshotGateway {
        private final Map<UUID, Map<String, String>> snapshots = new HashMap<>();
        @Override public void save(UUID playerId, Map<String, String> data) { snapshots.put(playerId, data); }
        @Override public Map<String, String> load(UUID playerId) { return snapshots.getOrDefault(playerId, new HashMap<>()); }
        @Override public void delete(UUID playerId) { snapshots.remove(playerId); }
    }

    static class StubTeleportService implements PlatformTeleportService {
        @Override public boolean teleport(UUID playerId, String dimension, double x, double y, double z, float yaw, float pitch) { return true; }
    }

    static class StubPlayerService implements PlatformPlayerService {
        @Override public boolean isOnline(UUID playerId) { return true; }
        @Override public void sendMessage(UUID playerId, String message) {}
        @Override public void sendTitle(UUID playerId, String title, String subtitle, int fadeIn, int stay, int fadeOut) {}
    }

    static class StubScheduler implements PlatformScheduler {
        @Override public boolean isServerThread() { return true; }
        @Override public void executeOnServerThread(Runnable r) { r.run(); }
        @Override public ScheduledHandle schedule(java.time.Duration delay, Runnable task) { task.run(); return () -> {}; }
        @Override public ScheduledHandle scheduleRepeating(java.time.Duration interval, Runnable task) { task.run(); return () -> {}; }
    }
}
