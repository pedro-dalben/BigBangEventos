package com.pedrodalben.bigbangeventos.trigger;

import com.pedrodalben.bigbangeventos.definition.EventArea;
import java.time.Duration;
import java.util.*;

public final class EventTrigger {
    private final String id;
    private final TriggerType type;
    private boolean enabled = true;
    private int maxUses;
    private Duration cooldown = Duration.ZERO;
    private final List<ConditionType> conditions = new ArrayList<>();
    private final Map<ConditionType, Map<String, String>> conditionArguments = new EnumMap<>(ConditionType.class);
    private final List<TriggerAction> actions = new ArrayList<>();
    private String binding;
    private EventArea area;

    public EventTrigger(String id, TriggerType type) {
        if (!id.matches("[a-z0-9][a-z0-9_-]{0,63}"))
            throw new IllegalArgumentException("ID de gatilho invalido");
        this.id = id;
        this.type = type;
    }

    public String id() { return id; }
    public TriggerType type() { return type; }
    public boolean enabled() { return enabled; }
    public void enabled(boolean v) { enabled = v; }
    public int maxUses() { return maxUses; }
    public void maxUses(int v) { maxUses = v; }
    public Duration cooldown() { return cooldown; }
    public void cooldown(Duration v) { cooldown = v; }
    public List<ConditionType> conditions() { return List.copyOf(conditions); }
    public List<TriggerAction> actions() { return List.copyOf(actions); }
    public void addCondition(ConditionType v) { conditions.add(v); }
    public void addCondition(ConditionType v, Map<String,String> arguments) { conditions.add(v); conditionArguments.put(v, Map.copyOf(arguments)); }
    public Map<String,String> conditionArguments(ConditionType v) { return conditionArguments.getOrDefault(v, Map.of()); }
    public void addAction(TriggerAction v) { actions.add(v); }
    public Optional<String> binding() { return Optional.ofNullable(binding); }
    public void binding(String value) { binding = value; }
    public Optional<EventArea> area() { return Optional.ofNullable(area); }
    public void area(EventArea value) { area = value; }
}
