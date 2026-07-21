package com.pedrodalben.bigbangeventos.definition;

public record EventArea(String serverId, String dimension, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    public EventArea { if (minX > maxX || minY > maxY || minZ > maxZ) throw new IllegalArgumentException("área inválida"); }
    public boolean contains(String server, String world, double x, double y, double z) { return serverId.equals(server) && dimension.equals(world) && x >= minX && x <= maxX + 1 && y >= minY && y <= maxY + 1 && z >= minZ && z <= maxZ + 1; }
}
