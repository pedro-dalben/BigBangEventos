package br.com.bigbangcraft.eventos.parkour;

import br.com.bigbangcraft.eventos.parkour.model.ParkourParticipantData;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ParkourParticipantService {

    private final Map<String, ParkourParticipantData> storage = new ConcurrentHashMap<>();

    private String key(String sessionId, String playerId) {
        return sessionId + ":" + playerId;
    }

    public ParkourParticipantData getOrCreate(String sessionId, String playerId) {
        return storage.computeIfAbsent(key(sessionId, playerId), k -> new ParkourParticipantData());
    }

    public Optional<ParkourParticipantData> get(String sessionId, String playerId) {
        return Optional.ofNullable(storage.get(key(sessionId, playerId)));
    }

    public void remove(String sessionId, String playerId) {
        storage.remove(key(sessionId, playerId));
    }

    public boolean hasCompletedCheckpoint(String sessionId, String playerId, String checkpointId) {
        ParkourParticipantData data = storage.get(key(sessionId, playerId));
        return data != null && data.hasCompletedCheckpoint(checkpointId);
    }

    public int getCompletedCount(String sessionId, String playerId) {
        ParkourParticipantData data = storage.get(key(sessionId, playerId));
        return data != null ? data.completedCount() : 0;
    }

    public ParkourParticipantData createSnapshot(String sessionId, String playerId) {
        ParkourParticipantData data = storage.get(key(sessionId, playerId));
        if (data == null) return new ParkourParticipantData();
        ParkourParticipantData copy = new ParkourParticipantData();
        copy.currentCheckpointId(data.currentCheckpointId());
        copy.startedAt(data.startedAt());
        copy.finishedAt(data.finishedAt());
        copy.elapsedMillis(data.elapsedMillis());
        copy.falls(data.falls());
        copy.attempts(data.attempts());
        copy.clearCompletedCheckpoints();
        for (String id : data.completedCheckpointIds()) {
            copy.addCompletedCheckpoint(id);
        }
        return copy;
    }

    public void clearSession(String sessionId) {
        storage.entrySet().removeIf(e -> e.getKey().startsWith(sessionId + ":"));
    }
}
