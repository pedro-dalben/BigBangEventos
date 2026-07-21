package com.pedrodalben.bigbangeventos.definition;

public record EventLocation(String serverId, String dimension, double x, double y, double z, float yaw, float pitch) {
    public EventLocation { if (serverId == null || serverId.isBlank() || dimension == null || dimension.isBlank()) throw new IllegalArgumentException("servidor e dimensão são obrigatórios"); }
}
