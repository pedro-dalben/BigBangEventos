package com.pedrodalben.bigbangeventos.platform;

import java.util.Objects;

public record StoredLocation(String serverId, String dimension, double x, double y, double z, float yaw, float pitch) {
    public StoredLocation {
        Objects.requireNonNull(serverId, "serverId");
        Objects.requireNonNull(dimension, "dimension");
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
            throw new IllegalArgumentException("coordenadas devem ser finitas");
    }
}
