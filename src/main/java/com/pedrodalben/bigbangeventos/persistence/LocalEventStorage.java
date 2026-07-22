package com.pedrodalben.bigbangeventos.persistence;

import com.pedrodalben.bigbangeventos.definition.*;
import com.pedrodalben.bigbangeventos.participant.EventParticipant;
import com.pedrodalben.bigbangeventos.participant.ParticipantState;
import com.pedrodalben.bigbangeventos.platform.StoredLocation;
import com.pedrodalben.bigbangeventos.session.*;
import com.pedrodalben.bigbangeventos.snapshot.*;
import com.pedrodalben.bigbangeventos.objective.*;
import com.pedrodalben.bigbangeventos.stage.*;
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
        m.put("schema-version", 2);
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
            t.area().ifPresent(area -> x.put("area", areaMap(area)));
            triggers.put(t.id(), x);
        });
        m.put("triggers", triggers);
        m.put("stages", d.stages().stream().map(LocalEventStorage::stageMap).toList());
        m.put("objectives", d.objectives().stream().map(LocalEventStorage::objectiveMap).toList());
        Map<String, Object> settings = d.typeSettings();
        if (!settings.isEmpty()) m.put("type-settings", settings);
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
        m.put("schema-version", 2);
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
        m.put("objective-progress", s.objectiveProgress().values().stream().map(LocalEventStorage::objectiveProgressMap).toList());
        m.put("stage-progress", s.stageProgress().values().stream().map(LocalEventStorage::stageProgressMap).toList());
        m.put("data", dataMap(s.rawData()));
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
                        if (x.get("area") instanceof Map<?, ?> area) t.area(readArea((Map<String,Object>) area));
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
                    Object ts = m.get("type-settings");
                    if (ts instanceof Map<?, ?> typeSettings) {
                        for (var e : typeSettings.entrySet()) {
                            d.typeSetting(e.getKey().toString(), e.getValue());
                        }
                    }
                    if (m.get("stages") instanceof List<?> stages) for (Object rawStage : stages) {
                        Map<String,Object> s = (Map<String,Object>) rawStage;
                        d.putStage(new EventStageDefinition(str(s,"id"),str(s,"display-name"),str(s,"description"),integer(s,"order"),bool(s,"required"),bool(s,"enabled"),longValue(s,"time-limit-seconds"),strings(s.get("objective-ids")),blankToNull(str(s,"next-stage-id")),bool(s,"auto-complete-when-objectives-complete"),stringMap(s.get("metadata"))));
                    }
                    if (m.get("objectives") instanceof List<?> objectives) for (Object rawObjective : objectives) {
                        Map<String,Object> o = (Map<String,Object>) rawObjective;
                        d.putObjective(new ObjectiveDefinition(str(o,"id"),str(o,"display-name"),str(o,"description"),str(o,"type-id"),str(o,"stage-id"),bool(o,"required"),integer(o,"order"),longValue(o,"target"),ObjectiveScope.valueOf(str(o,"scope")),bool(o,"enabled"),stringMap(o.get("metadata"))));
                    }
                    definitions.put(d.id(), d);
                } catch (Exception e) {
                    throw new IllegalStateException("Definição inválida em " + p, e);
                }
            });
        }
        try (var stream = Files.list(sessions)) {
            stream.filter(p -> p.toString().endsWith(".yml")).forEach(this::loadSession);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadSession(Path path) {
        try (Reader r=Files.newBufferedReader(path)) {
            Map<String,Object> m=yaml.load(r); UUID id=UUID.fromString(str(m,"id")); EventSession s=new EventSession(id,str(m,"event-id"),integer(m,"configuration-version"),Instant.parse(str(m,"created-at")),null); s.restoreState(SessionState.valueOf(str(m,"state")),Instant.now()); s.cancelReason(blankToNull(str(m,"cancel-reason")));
            if(m.get("participants") instanceof List<?> parts)for(Object raw:parts){Map<String,Object> p=(Map<String,Object>)raw;UUID pid=UUID.fromString(str(p,"player-id"));var ep=new EventParticipant(pid,str(p,"name"),Instant.parse(str(p,"joined-at")));ep.state(ParticipantState.valueOf(str(p,"state")));ep.restoreScore(integer(p,"score"));ep.position(integer(p,"position"));s.addParticipant(ep);}
            if(m.get("objective-progress") instanceof List<?> progress)for(Object raw:progress){Map<String,Object> p=(Map<String,Object>)raw;UUID pid=blankToNull(str(p,"player-id"))==null?null:UUID.fromString(str(p,"player-id"));var op=new com.pedrodalben.bigbangeventos.objective.ObjectiveProgress(str(p,"objective-id"),ObjectiveScope.valueOf(str(p,"scope")),pid,longValue(p,"target"),longValue(p,"current"),ObjectiveStatus.valueOf(str(p,"status")),Instant.parse(str(p,"updated-at")));op.metadata(stringMap(p.get("metadata")));s.objectiveProgress().put(p.get("scope")+":"+p.get("objective-id")+":"+(pid==null?"session":pid),op);}
            if(m.get("stage-progress") instanceof List<?> progress)for(Object raw:progress){Map<String,Object> p=(Map<String,Object>)raw;String idValue=str(p,"stage-id");s.stageProgress().put(idValue,new SessionStageProgress(idValue,StageStatus.valueOf(str(p,"status")),Instant.now()));}
            if(m.get("data") instanceof Map<?,?> data)for(var e:data.entrySet()){Map<String,Object> value=(Map<String,Object>)e.getValue();s.rawData().loadRaw(e.getKey().toString(),str(value,"type"),value.get("value"));}
            loadedSessions.put(id,s);
        } catch(Exception e){throw new IllegalStateException("Sessão inválida em "+path,e);}
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
    private static long longValue(Map<String,Object> m,String key){return Long.parseLong(str(m,key));}
    private static boolean bool(Map<String,Object> m,String key){return Boolean.parseBoolean(str(m,key));}
    private static String blankToNull(String value){return value.isBlank()?null:value;}
    private static List<String> strings(Object raw){if(!(raw instanceof List<?> l))return List.of();return l.stream().map(Object::toString).toList();}
    private static Map<String,String> stringMap(Object raw){if(!(raw instanceof Map<?,?> m))return Map.of();var out=new LinkedHashMap<String,String>();m.forEach((k,v)->out.put(k.toString(),v.toString()));return Map.copyOf(out);}
    private static Map<String,Object> areaMap(EventArea area){
        if(area instanceof EventArea.Cuboid c)return Map.of("kind","CUBOID","server",c.serverId(),"dimension",c.dimension(),"min-x",c.minX(),"min-y",c.minY(),"min-z",c.minZ(),"max-x",c.maxX(),"max-y",c.maxY(),"max-z",c.maxZ());
        var r=(EventArea.Radius)area;return Map.of("kind","RADIUS","server",r.serverId(),"dimension",r.dimension(),"center-x",r.centerX(),"center-y",r.centerY(),"center-z",r.centerZ(),"radius",r.radius(),"vertical-radius",r.verticalRadius());
    }
    private static Map<String,Object> stageMap(EventStageDefinition s){var m=new LinkedHashMap<String,Object>();m.put("id",s.id());m.put("display-name",s.displayName());m.put("description",s.description());m.put("order",s.order());m.put("required",s.required());m.put("enabled",s.enabled());m.put("time-limit-seconds",s.timeLimitSeconds());m.put("objective-ids",s.objectiveIds());m.put("next-stage-id",s.nextStageId()==null?"":s.nextStageId());m.put("auto-complete-when-objectives-complete",s.autoCompleteWhenObjectivesComplete());m.put("metadata",s.metadata());return m;}
    private static Map<String,Object> objectiveMap(ObjectiveDefinition o){var m=new LinkedHashMap<String,Object>();m.put("id",o.id());m.put("display-name",o.displayName());m.put("description",o.description());m.put("type-id",o.typeId());m.put("stage-id",o.stageId());m.put("required",o.required());m.put("order",o.order());m.put("target",o.target());m.put("scope",o.scope().name());m.put("enabled",o.enabled());m.put("metadata",o.metadata());return m;}
    private static Map<String,Object> objectiveProgressMap(com.pedrodalben.bigbangeventos.objective.ObjectiveProgress p){var m=new LinkedHashMap<String,Object>();m.put("objective-id",p.objectiveId());m.put("scope",p.scope().name());m.put("player-id",p.playerId()==null?"":p.playerId().toString());m.put("current",p.current());m.put("target",p.target());m.put("status",p.status().name());m.put("updated-at",p.updatedAt()==null?"":p.updatedAt().toString());m.put("last-source",p.lastSource());m.put("metadata",p.metadata());return m;}
    private static Map<String,Object> stageProgressMap(com.pedrodalben.bigbangeventos.stage.SessionStageProgress p){var m=new LinkedHashMap<String,Object>();m.put("stage-id",p.stageId());m.put("status",p.status().name());m.put("started-at",p.startedAt()==null?"":p.startedAt().toString());m.put("completed-at",p.completedAt()==null?"":p.completedAt().toString());m.put("failed-at",p.failedAt()==null?"":p.failedAt().toString());m.put("deadline",p.deadline()==null?"":p.deadline().toString());return m;}
    private static Map<String,Object> dataMap(com.pedrodalben.bigbangeventos.data.InMemoryDataContainer data){var out=new LinkedHashMap<String,Object>();data.rawValues().forEach((key,value)->out.put(key,Map.of("type",value.type(),"value",value.value())));return out;}
    private static EventArea readArea(Map<String,Object> m){
        if("CUBOID".equals(str(m,"kind")))return new EventArea.Cuboid(str(m,"server"),str(m,"dimension"),integer(m,"min-x"),integer(m,"min-y"),integer(m,"min-z"),integer(m,"max-x"),integer(m,"max-y"),integer(m,"max-z"));
        return new EventArea.Radius(str(m,"server"),str(m,"dimension"),number(m,"center-x"),number(m,"center-y"),number(m,"center-z"),number(m,"radius"),number(m,"vertical-radius"));
    }
    private static double number(Map<String, Object> m, String key) {
        return Double.parseDouble(str(m, key));
    }
}
