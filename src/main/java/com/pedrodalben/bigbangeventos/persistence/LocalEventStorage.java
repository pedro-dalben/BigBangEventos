package com.pedrodalben.bigbangeventos.persistence;

import com.pedrodalben.bigbangeventos.definition.*;
import com.pedrodalben.bigbangeventos.participant.EventParticipant;
import com.pedrodalben.bigbangeventos.platform.StoredLocation;
import com.pedrodalben.bigbangeventos.session.*;
import com.pedrodalben.bigbangeventos.snapshot.*;
import org.yaml.snakeyaml.Yaml;
import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

public final class LocalEventStorage implements EventStorage {
    private final Path events, sessions, snapshotsDir;
    private final Yaml yaml = new Yaml();
    private final Map<String, EventDefinition> definitions = new HashMap<>();
    private final Map<UUID, EventSession> loadedSessions = new HashMap<>();
    private final Map<UUID, PlayerSnapshot> loadedSnapshots = new HashMap<>();

    public LocalEventStorage(Path root) {
        events = root.resolve("events");
        sessions = root.resolve("sessions");
        snapshotsDir = root.resolve("snapshots");
        try {
            Files.createDirectories(events);
            Files.createDirectories(sessions);
            Files.createDirectories(snapshotsDir);
            load();
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível abrir armazenamento", e);
        }
    }

    @Override
    public synchronized void saveDefinition(EventDefinition d) {
        definitions.put(d.id(), d);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("schema-version", 1);
        m.put("id", d.id()); m.put("type", d.type()); m.put("server", d.serverId());
        m.put("display-name", d.displayName()); m.put("description", d.description());
        m.put("enabled", d.enabled()); m.put("min-players", d.minPlayers());
        m.put("max-players", d.maxPlayers());
        Map<String, Object> loc = new LinkedHashMap<>();
        d.locations().forEach((k, v) -> loc.put(k.name(), Map.of(
                "dimension", v.dimension(), "x", v.x(), "y", v.y(), "z", v.z(),
                "yaw", v.yaw(), "pitch", v.pitch())));
        m.put("locations", loc);
        Map<String, Object> triggers = new LinkedHashMap<>();
        d.triggers().forEach(t -> {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("type", t.type().name()); x.put("enabled", t.enabled());
            x.put("binding", t.binding().orElse(""));
            x.put("conditions", t.conditions().stream().map(Enum::name).toList());
            x.put("actions", t.actions().stream().map(a -> Map.of(
                    "type", a.type().name(), "arguments", a.arguments())).toList());
            triggers.put(t.id(), x);
        });
        m.put("triggers", triggers);
        write(events.resolve(d.id() + ".yml"), m);
    }

    @Override public synchronized Optional<EventDefinition> findDefinition(String id) {
        return Optional.ofNullable(definitions.get(id));
    }
    @Override public synchronized Collection<EventDefinition> findDefinitions() {
        return List.copyOf(definitions.values());
    }
    @Override public synchronized void deleteDefinition(String id) {
        definitions.remove(id);
        try { Files.deleteIfExists(events.resolve(id + ".yml")); }
        catch (IOException e) { throw new IllegalStateException("Não foi possível excluir evento", e); }
    }

    @Override
    public synchronized void saveSession(EventSession s) {
        loadedSessions.put(s.id(), s);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("schema-version", 1);
        m.put("id", s.id().toString()); m.put("event-id", s.eventId());
        m.put("configuration-version", s.configurationVersion());
        m.put("created-at", s.createdAt().toString());
        m.put("state", s.state().name());
        m.put("cancel-reason", s.cancelReason().orElse(""));

        List<Map<String, Object>> parts = new ArrayList<>();
        for (EventParticipant p : s.participants()) {
            Map<String, Object> pm = new LinkedHashMap<>();
            pm.put("player-id", p.playerId().toString());
            pm.put("name", p.knownName());
            pm.put("joined-at", p.joinedAt().toString());
            pm.put("state", p.state().name());
            pm.put("score", p.score());
            pm.put("position", p.position());
            p.snapshotId().ifPresent(sid -> pm.put("snapshot-id", sid.toString()));
            parts.add(pm);
        }
        m.put("participants", parts);
        write(sessions.resolve(s.id() + ".yml"), m);
    }

    @Override public synchronized Optional<EventSession> findSession(UUID id) {
        return Optional.ofNullable(loadedSessions.get(id));
    }
    @Override public synchronized Collection<EventSession> findUnfinishedSessions() {
        return loadedSessions.values().stream()
                .filter(s -> s.state() != SessionState.FINISHED
                        && s.state() != SessionState.CANCELLED
                        && s.state() != SessionState.FAILED)
                .toList();
    }

    @Override
    public synchronized void saveSnapshot(PlayerSnapshot s) {
        loadedSnapshots.put(s.snapshotId(), s);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("schema-version", 1);
        m.put("snapshot-id", s.snapshotId().toString());
        m.put("player-id", s.playerId().toString());
        m.put("session-id", s.sessionId().toString());
        m.put("state", s.state().name());
        m.put("original-location", Map.of(
                "dimension", s.originalLocation().dimension(),
                "x", s.originalLocation().x(), "y", s.originalLocation().y(),
                "z", s.originalLocation().z(),
                "yaw", s.originalLocation().yaw(), "pitch", s.originalLocation().pitch()));
        m.put("inventory", s.serializedInventory());
        m.put("armor", s.serializedArmor());
        m.put("offhand", s.serializedOffhand());
        m.put("experience", Map.of("total", s.totalExperience(), "level", s.experienceLevel(),
                "progress", s.experienceProgress()));
        m.put("health", s.health()); m.put("absorption", s.absorption());
        m.put("food", Map.of("level", s.foodLevel(), "saturation", s.saturation()));
        m.put("gameMode", s.gameMode());
        m.put("flight", Map.of("allow", s.allowFlight(), "flying", s.isFlying(),
                "flySpeed", s.flySpeed(), "walkSpeed", s.walkSpeed()));
        m.put("selectedSlot", s.selectedSlot());
        m.put("fireTicks", s.fireTicks()); m.put("fallDistance", s.fallDistance());
        m.put("activeEffects", s.activeEffects());
        m.put("restoredComponents", s.restoredComponents().stream().map(Enum::name).toList());
        m.put("createdAtMs", s.createdAtMs());
        s.exitDestination().ifPresent(d -> m.put("exitDestination", d));

        Path playerDir = snapshotsDir.resolve(s.playerId().toString());
        try { Files.createDirectories(playerDir); }
        catch (IOException e) { throw new IllegalStateException("Falha ao criar diretório de snapshots", e); }
        write(playerDir.resolve(s.snapshotId() + ".yml"), m);
    }

    @Override public synchronized Optional<PlayerSnapshot> findSnapshot(UUID snapshotId) {
        return Optional.ofNullable(loadedSnapshots.get(snapshotId));
    }

    @Override public synchronized Collection<PlayerSnapshot> findSnapshotsByPlayer(UUID playerId) {
        return loadedSnapshots.values().stream()
                .filter(s -> s.playerId().equals(playerId))
                .toList();
    }

    @Override public synchronized void deleteSnapshot(UUID snapshotId) {
        PlayerSnapshot s = loadedSnapshots.remove(snapshotId);
        if (s != null) {
            try { Files.deleteIfExists(snapshotsDir.resolve(s.playerId().toString())
                    .resolve(snapshotId + ".yml")); }
            catch (IOException ignored) {}
        }
    }

    @SuppressWarnings("unchecked")
    private void load() throws IOException {
        try (var stream = Files.list(events)) {
            stream.filter(p -> p.toString().endsWith(".yml")).forEach(p -> {
                try (Reader r = Files.newBufferedReader(p)) {
                    Map<String, Object> m = yaml.load(r);
                    EventDefinition d = new EventDefinition(str(m, "id"), str(m, "type"), str(m, "server"));
                    d.displayName(str(m, "display-name"));
                    d.description(str(m, "description"));
                    d.enabled(Boolean.parseBoolean(str(m, "enabled")));
                    d.playerLimits(integer(m, "min-players"), integer(m, "max-players"));
                    Object raw = m.get("locations");
                    if (raw instanceof Map<?, ?> locations) for (var e : locations.entrySet()) {
                        Map<String, Object> v = (Map<String, Object>) e.getValue();
                        d.location(LocationName.valueOf(e.getKey().toString()),
                                new EventLocation(d.serverId(), str(v, "dimension"),
                                        number(v, "x"), number(v, "y"), number(v, "z"),
                                        (float) number(v, "yaw"), (float) number(v, "pitch")));
                    }
                    Object triggerRaw = m.get("triggers");
                    if (triggerRaw instanceof Map<?, ?> ts) for (var e : ts.entrySet()) {
                        Map<String, Object> x = (Map<String, Object>) e.getValue();
                        var t = new com.pedrodalben.bigbangeventos.trigger.EventTrigger(
                                e.getKey().toString(),
                                com.pedrodalben.bigbangeventos.trigger.TriggerType.valueOf(str(x, "type")));
                        t.enabled(Boolean.parseBoolean(str(x, "enabled")));
                        if (!str(x, "binding").isBlank()) t.binding(str(x, "binding"));
                        Object cs = x.get("conditions"); if (cs instanceof List<?> l)
                            for (Object c : l) t.addCondition(com.pedrodalben.bigbangeventos.trigger.ConditionType.valueOf(c.toString()));
                        Object as = x.get("actions"); if (as instanceof List<?> l)
                            for (Object a : l) {
                                Map<String, Object> am = (Map<String, Object>) a;
                                Map<String, String> args = new HashMap<>();
                                Object ar = am.get("arguments"); if (ar instanceof Map<?, ?> map)
                                    map.forEach((k, v) -> args.put(k.toString(), v.toString()));
                                t.addAction(new com.pedrodalben.bigbangeventos.trigger.TriggerAction(
                                        com.pedrodalben.bigbangeventos.trigger.ActionType.valueOf(str(am, "type")), args));
                            }
                        d.putTrigger(t);
                    }
                    definitions.put(d.id(), d);
                } catch (Exception e) {
                    throw new IllegalStateException("Definição inválida em " + p, e);
                }
            });
        }
    }

    private void write(Path p, Map<String, Object> m) {
        Path tmp = p.resolveSibling(p.getFileName() + ".tmp");
        try (Writer w = Files.newBufferedWriter(tmp)) {
            yaml.dump(m, w);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao salvar " + p, e);
        }
        try {
            if (Files.exists(p)) Files.delete(p);
            Files.move(tmp, p, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            try { Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING); }
            catch (IOException e2) {
                throw new IllegalStateException("Falha ao salvar atomicamente " + p, e2);
            }
        }
    }

    private static String str(Map<String, Object> m, String key) {
        return String.valueOf(m.getOrDefault(key, ""));
    }
    private static int integer(Map<String, Object> m, String key) {
        return Integer.parseInt(str(m, key));
    }
    private static double number(Map<String, Object> m, String key) {
        return Double.parseDouble(str(m, key));
    }
}
