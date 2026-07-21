package br.com.bigbangcraft.eventos.parkour.model;

import com.pedrodalben.bigbangeventos.definition.EventLocation;
import java.time.Instant;

public final class ParkourCheckpoint {
    private final String id;
    private final int order;
    private final EventLocation location;
    private final double radius;
    private boolean enabled;
    private final Instant createdAt;

    public ParkourCheckpoint(String id, int order, EventLocation location, double radius) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("checkpoint ID obrigatorio");
        if (order < 0) throw new IllegalArgumentException("ordem nao pode ser negativa");
        if (radius <= 0) throw new IllegalArgumentException("raio deve ser positivo");
        this.id = id;
        this.order = order;
        this.location = location;
        this.radius = radius;
        this.enabled = true;
        this.createdAt = Instant.now();
    }

    public String id() { return id; }
    public int order() { return order; }
    public EventLocation location() { return location; }
    public double radius() { return radius; }
    public boolean enabled() { return enabled; }
    public void enabled(boolean v) { enabled = v; }
    public Instant createdAt() { return createdAt; }

    public boolean contains(String serverId, String dimension, double x, double y, double z) {
        if (!location.serverId().equals(serverId)) return false;
        if (!location.dimension().equals(dimension)) return false;
        double dx = x - location.x();
        double dy = y - location.y();
        double dz = z - location.z();
        return (dx * dx + dy * dy + dz * dz) <= (radius * radius);
    }
}
