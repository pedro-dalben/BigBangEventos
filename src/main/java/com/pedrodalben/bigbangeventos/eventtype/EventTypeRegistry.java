package com.pedrodalben.bigbangeventos.eventtype;

import java.util.*;

public final class EventTypeRegistry {
    private final Map<String, EventType> types = new TreeMap<>();
    public synchronized void register(EventType type) { if (types.putIfAbsent(type.id(), type) != null) throw new IllegalArgumentException("tipo duplicado: "+type.id()); }
    public Optional<EventType> find(String id) { return Optional.ofNullable(types.get(id)); }
    public Collection<EventType> all() { return List.copyOf(types.values()); }
}
