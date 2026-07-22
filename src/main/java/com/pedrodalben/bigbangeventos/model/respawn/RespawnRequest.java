package com.pedrodalben.bigbangeventos.model.respawn;

import java.time.Instant;
import java.util.*;

public final class RespawnRequest {
    private final UUID playerId;
    private final String eventId;
    private final UUID sessionId;
    private final Instant scheduledAt;
    private final Instant deadline;
    private boolean completed;
    private UUID assignedRoundId;

    public RespawnRequest(UUID playerId, String eventId, UUID sessionId, Instant scheduledAt, long delaySeconds) {
        this.playerId = playerId; this.eventId = eventId; this.sessionId = sessionId;
        this.scheduledAt = scheduledAt; this.deadline = scheduledAt.plusSeconds(delaySeconds);
    }

    public UUID playerId() { return playerId; }
    public String eventId() { return eventId; }
    public UUID sessionId() { return sessionId; }
    public Instant scheduledAt() { return scheduledAt; }
    public Instant deadline() { return deadline; }
    public boolean completed() { return completed; }
    public Optional<UUID> assignedRoundId() { return Optional.ofNullable(assignedRoundId); }
    public void completed(boolean v) { this.completed = v; }
    public void assignedRoundId(UUID id) { this.assignedRoundId = id; }

    public boolean isDue(Instant now) { return !completed && now.isAfter(deadline); }
}
