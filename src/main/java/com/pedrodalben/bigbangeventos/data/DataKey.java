package com.pedrodalben.bigbangeventos.data;

import java.util.Objects;

public record DataKey<T>(String namespace, String id, DataCodec<T> codec, T defaultValue) {
    public DataKey {
        if (namespace == null || !namespace.matches("[a-z0-9][a-z0-9_-]{0,31}")) throw new IllegalArgumentException("Namespace inválido");
        if (id == null || !id.matches("[a-z0-9][a-z0-9_-]{0,63}")) throw new IllegalArgumentException("ID de dado inválido");
        Objects.requireNonNull(codec);
    }
    public String key() { return namespace + ":" + id; }
}
