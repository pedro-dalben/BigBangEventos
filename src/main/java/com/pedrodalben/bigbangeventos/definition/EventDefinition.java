package com.pedrodalben.bigbangeventos.definition;

import com.pedrodalben.bigbangeventos.trigger.EventTrigger;
import com.pedrodalben.bigbangeventos.objective.ObjectiveDefinition;
import com.pedrodalben.bigbangeventos.stage.EventStageDefinition;
import java.time.Instant;
import java.util.*;

public final class EventDefinition {
    private final String id;
    private String displayName;
    private String description = "";
    private final String type;
    private boolean enabled = true;
    private final String serverId;
    private int minPlayers = 1, maxPlayers = 0;
    private int configurationVersion = 1;
    private final Instant createdAt;
    private Instant updatedAt;
    private final EnumMap<LocationName, EventLocation> locations = new EnumMap<>(LocationName.class);
    private EventArea area;
    private final Map<String, EventTrigger> triggers = new LinkedHashMap<>();
    private final Map<String, Object> typeSettings = new LinkedHashMap<>();
    private final Map<String, ObjectiveDefinition> objectives = new LinkedHashMap<>();
    private final Map<String, EventStageDefinition> stages = new LinkedHashMap<>();

    public EventDefinition(String id, String type, String serverId) {
        if (!id.matches("[a-z0-9][a-z0-9_-]{0,63}")) throw new IllegalArgumentException("ID inválido: " + id);
        this.id = id; this.type = type; this.serverId = serverId; displayName = id;
        createdAt = updatedAt = Instant.now();
    }
    public String id() { return id; } public String type() { return type; } public String serverId() { return serverId; }
    public String displayName() { return displayName; } public String description() { return description; }
    public boolean enabled() { return enabled; } public int minPlayers() { return minPlayers; } public int maxPlayers() { return maxPlayers; }
    public int configurationVersion() { return configurationVersion; } public Instant createdAt() { return createdAt; } public Instant updatedAt() { return updatedAt; }
    public Optional<EventLocation> location(LocationName name) { return Optional.ofNullable(locations.get(name)); }
    public Map<LocationName, EventLocation> locations() { return Map.copyOf(locations); }
    public Optional<EventArea> area() { return Optional.ofNullable(area); }
    public Collection<EventTrigger> triggers() { return List.copyOf(triggers.values()); }
    public Optional<EventTrigger> trigger(String id) { return Optional.ofNullable(triggers.get(id)); }
    public Map<String, Object> typeSettings() { return Map.copyOf(typeSettings); }
    public Collection<ObjectiveDefinition> objectives() { return List.copyOf(objectives.values()); }
    public Optional<ObjectiveDefinition> objective(String id) { return Optional.ofNullable(objectives.get(id)); }
    public Collection<EventStageDefinition> stages() { return List.copyOf(stages.values()); }
    public Optional<EventStageDefinition> stage(String id) { return Optional.ofNullable(stages.get(id)); }
    public void displayName(String value) { displayName = require(value); changed(); }
    public void description(String value) { description = value == null ? "" : value; changed(); }
    public void enabled(boolean value) { enabled = value; changed(); }
    public void playerLimits(int min, int max) { if (min < 0 || max < 0 || max > 0 && max < min) throw new IllegalArgumentException("limites inválidos"); minPlayers=min; maxPlayers=max; changed(); }
    public void location(LocationName name, EventLocation value) { locations.put(Objects.requireNonNull(name), Objects.requireNonNull(value)); changed(); }
    public void area(EventArea value) { area=value; changed(); }
    public void putTrigger(EventTrigger trigger) { if (triggers.putIfAbsent(trigger.id(), trigger) != null) throw new IllegalArgumentException("gatilho duplicado"); changed(); }
    public void removeTrigger(String id) { triggers.remove(id); changed(); }
    public void typeSetting(String key, Object value) { typeSettings.put(key, value); changed(); }
    public void putObjective(ObjectiveDefinition objective) { if (objectives.putIfAbsent(objective.id(), objective) != null) throw new IllegalArgumentException("objetivo duplicado"); changed(); }
    public void removeObjective(String id) { objectives.remove(id); changed(); }
    public void putStage(EventStageDefinition stage) { if (stages.putIfAbsent(stage.id(), stage) != null) throw new IllegalArgumentException("etapa duplicada"); changed(); }
    public void replaceStage(EventStageDefinition stage) { if (!stages.containsKey(stage.id())) throw new IllegalArgumentException("etapa não encontrada"); stages.put(stage.id(), stage); changed(); }
    public void removeStage(String id) { stages.remove(id); changed(); }
    private void changed() { configurationVersion++; updatedAt = Instant.now(); }
    private static String require(String value) { if (value == null || value.isBlank()) throw new IllegalArgumentException("valor obrigatório"); return value; }
}
