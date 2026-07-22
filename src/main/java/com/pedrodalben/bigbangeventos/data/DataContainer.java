package com.pedrodalben.bigbangeventos.data;

import java.util.*;

public interface DataContainer {
    <T> Optional<T> get(DataKey<T> key);
    <T> T getOrDefault(DataKey<T> key);
    <T> void set(DataKey<T> key, T value);
    <T> void remove(DataKey<T> key);
    boolean contains(DataKey<?> key);
    Set<String> keys();
}
