package com.pedrodalben.bigbangeventos.definition;

public sealed interface EventArea permits EventArea.Cuboid, EventArea.Radius {

    boolean contains(String serverId, String dimension, double x, double y, double z);

    record Cuboid(String serverId, String dimension,
                  int minX, int minY, int minZ,
                  int maxX, int maxY, int maxZ) implements EventArea {

        public Cuboid {
            if (minX > maxX || minY > maxY || minZ > maxZ)
                throw new IllegalArgumentException("area invalida: min > max");
        }

        @Override
        public boolean contains(String server, String world, double x, double y, double z) {
            return serverId.equals(server) && dimension.equals(world)
                    && x >= minX && x <= maxX + 1
                    && y >= minY && y <= maxY + 1
                    && z >= minZ && z <= maxZ + 1;
        }
    }

    record Radius(String serverId, String dimension,
                  double centerX, double centerY, double centerZ,
                  double radius, double verticalRadius) implements EventArea {

        public Radius {
            if (radius <= 0 || verticalRadius <= 0)
                throw new IllegalArgumentException("radius and verticalRadius must be positive");
        }

        @Override
        public boolean contains(String server, String world, double x, double y, double z) {
            if (!serverId.equals(server) || !dimension.equals(world)) return false;
            double dx = x - centerX;
            double dz = z - centerZ;
            if (dx * dx + dz * dz > radius * radius) return false;
            return Math.abs(y - centerY) <= verticalRadius;
        }
    }
}
