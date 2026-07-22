package com.pedrodalben.bigbangeventos.objective;

import java.util.*;

public final class ObjectiveTypeRegistry {
    private final Map<String, ObjectiveTypeHandler> handlers = new TreeMap<>();
    public ObjectiveTypeRegistry() {
        register(new Basic("boolean")); register(new Basic("counter"));
        register(new Basic("manual")); register(new Basic("trigger"));
    }
    public synchronized void register(ObjectiveTypeHandler handler) {
        Objects.requireNonNull(handler);
        if (handler.id().isBlank() || handlers.putIfAbsent(handler.id(), handler) != null)
            throw new IllegalArgumentException("tipo de objetivo duplicado ou inválido: " + handler.id());
    }
    public Optional<ObjectiveTypeHandler> find(String id) { return Optional.ofNullable(handlers.get(id)); }
    public Collection<ObjectiveTypeHandler> all() { return List.copyOf(handlers.values()); }
    private record Basic(String id) implements ObjectiveTypeHandler { }
}
