package com.pedrodalben.bigbangeventos.objective;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class ObjectiveProgress {
    private final String objectiveId;
    private final ObjectiveScope scope;
    private final UUID playerId;
    private final long target;
    private long current;
    private ObjectiveStatus status;
    private Instant startedAt, completedAt, failedAt, updatedAt;
    private String lastSource = "";
    private Map<String, String> metadata = Map.of();

    public ObjectiveProgress(String objectiveId, ObjectiveScope scope, UUID playerId, long target,
                             long current, ObjectiveStatus status, Instant updatedAt) {
        this.objectiveId = objectiveId; this.scope = scope; this.playerId = playerId;
        this.target = target; this.current = current; this.status = status; this.updatedAt = updatedAt;
    }
    public String objectiveId() { return objectiveId; }
    public ObjectiveScope scope() { return scope; }
    public UUID playerId() { return playerId; }
    public long current() { return current; }
    public long target() { return target; }
    public ObjectiveStatus status() { return status; }
    public Instant startedAt() { return startedAt; }
    public Instant completedAt() { return completedAt; }
    public Instant failedAt() { return failedAt; }
    public Instant updatedAt() { return updatedAt; }
    public String lastSource() { return lastSource; }
    public Map<String, String> metadata() { return metadata; }
    public void start(Instant now, String source) { if (startedAt == null) startedAt = now; status = status == ObjectiveStatus.LOCKED ? ObjectiveStatus.AVAILABLE : status; updatedAt = now; lastSource = source == null ? "" : source; }
    public void progress(long value, Instant now, String source) { current = Math.max(0, Math.min(target, value)); if (current > 0 && status == ObjectiveStatus.AVAILABLE) status = ObjectiveStatus.IN_PROGRESS; updatedAt = now; lastSource = source == null ? "" : source; }
    public void complete(Instant now, String source) { current = target; status = ObjectiveStatus.COMPLETED; completedAt = completedAt == null ? now : completedAt; updatedAt = now; lastSource = source == null ? "" : source; }
    public void fail(Instant now, String source) { status = ObjectiveStatus.FAILED; failedAt = failedAt == null ? now : failedAt; updatedAt = now; lastSource = source == null ? "" : source; }
    public void skip(Instant now, String source) { status = ObjectiveStatus.SKIPPED; updatedAt = now; lastSource = source == null ? "" : source; }
    public void metadata(Map<String, String> value) { metadata = value == null ? Map.of() : Map.copyOf(value); }
}
