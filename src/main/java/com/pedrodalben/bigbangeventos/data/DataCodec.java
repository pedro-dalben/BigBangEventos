package com.pedrodalben.bigbangeventos.data;

public interface DataCodec<T> {
    String type();
    Object encode(T value);
    T decode(Object raw);
}
