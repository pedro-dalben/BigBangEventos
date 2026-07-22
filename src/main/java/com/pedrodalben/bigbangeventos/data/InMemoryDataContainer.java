package com.pedrodalben.bigbangeventos.data;

import java.util.*;

public final class InMemoryDataContainer implements DataContainer {
    private final Map<String, Object> values = new LinkedHashMap<>();
    private final Map<String, RawValue> unknown = new LinkedHashMap<>();
    @Override public synchronized <T> Optional<T> get(DataKey<T> key) {
        Object value = values.get(key.key());
        if (value == null) return Optional.empty();
        try { return Optional.ofNullable(key.codec().decode(value)); }
        catch (RuntimeException e) { return Optional.empty(); }
    }
    @Override public synchronized <T> T getOrDefault(DataKey<T> key) { return get(key).orElse(key.defaultValue()); }
    @Override public synchronized <T> void set(DataKey<T> key, T value) {
        if (value == null) { remove(key); return; }
        values.put(key.key(), key.codec().encode(value)); unknown.remove(key.key());
    }
    @Override public synchronized <T> void remove(DataKey<T> key) { values.remove(key.key()); unknown.remove(key.key()); }
    @Override public synchronized boolean contains(DataKey<?> key) { return values.containsKey(key.key()) || unknown.containsKey(key.key()); }
    @Override public synchronized Set<String> keys() { var out = new LinkedHashSet<>(values.keySet()); out.addAll(unknown.keySet()); return Set.copyOf(out); }
    public synchronized Map<String, RawValue> rawValues() {
        Map<String, RawValue> out = new LinkedHashMap<>();
        values.forEach((key, value) -> out.put(key, new RawValue("raw", value)));
        out.putAll(unknown); return Map.copyOf(out);
    }
    public synchronized void loadRaw(String key, String type, Object value) { values.put(key, value); unknown.remove(key); }
    public record RawValue(String type, Object value) {}
}
