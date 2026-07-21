package br.com.bigbangcraft.eventos.parkour.model;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class ParkourParticipantData {
    private String currentCheckpointId;
    private final Set<String> completedCheckpointIds;
    private Instant startedAt;
    private Instant finishedAt;
    private long elapsedMillis;
    private int falls;
    private int attempts;
    private boolean resetInProgress;

    public ParkourParticipantData() {
        this.completedCheckpointIds = new LinkedHashSet<>();
    }

    public String currentCheckpointId() { return currentCheckpointId; }
    public void currentCheckpointId(String id) { this.currentCheckpointId = id; }

    public Set<String> completedCheckpointIds() { return Collections.unmodifiableSet(completedCheckpointIds); }

    public boolean addCompletedCheckpoint(String id) { return completedCheckpointIds.add(id); }

    public boolean hasCompletedCheckpoint(String id) { return completedCheckpointIds.contains(id); }

    public int completedCount() { return completedCheckpointIds.size(); }

    public void clearCompletedCheckpoints() { completedCheckpointIds.clear(); }

    public Instant startedAt() { return startedAt; }

    public void startedAt(Instant v) { this.startedAt = v; }

    public Instant finishedAt() { return finishedAt; }

    public void finishedAt(Instant v) { this.finishedAt = v; }

    public long elapsedMillis() { return elapsedMillis; }

    public void elapsedMillis(long v) { this.elapsedMillis = v; }

    public int falls() { return falls; }

    public void incrementFalls() { this.falls++; }

    public void falls(int v) { this.falls = v; }

    public int attempts() { return attempts; }

    public void incrementAttempts() { this.attempts++; }

    public void attempts(int v) { this.attempts = v; }

    public boolean resetInProgress() { return resetInProgress; }

    public void resetInProgress(boolean v) { this.resetInProgress = v; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParkourParticipantData that)) return false;
        return Objects.equals(currentCheckpointId, that.currentCheckpointId)
                && completedCheckpointIds.equals(that.completedCheckpointIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentCheckpointId, completedCheckpointIds);
    }
}
