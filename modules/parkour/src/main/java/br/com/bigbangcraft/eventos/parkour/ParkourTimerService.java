package br.com.bigbangcraft.eventos.parkour;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ParkourTimerService {

    private final Map<String, Instant> startedAt = new ConcurrentHashMap<>();

    private String key(String sessionId, String playerId) {
        return sessionId + ":" + playerId;
    }

    public void start(String sessionId, String playerId) {
        startedAt.put(key(sessionId, playerId), Instant.now());
    }

    public long getElapsedMillis(String sessionId, String playerId) {
        Instant start = startedAt.get(key(sessionId, playerId));
        if (start == null) return 0;
        return Duration.between(start, Instant.now()).toMillis();
    }

    public Duration getElapsed(String sessionId, String playerId) {
        return Duration.ofMillis(getElapsedMillis(sessionId, playerId));
    }

    public void stop(String sessionId, String playerId) {
        startedAt.remove(key(sessionId, playerId));
    }

    public void clearSession(String sessionId) {
        startedAt.entrySet().removeIf(e -> e.getKey().startsWith(sessionId + ":"));
    }

    public static String format(long elapsedMillis) {
        long totalMs = Math.abs(elapsedMillis);
        long minutes = totalMs / 60000;
        long seconds = (totalMs % 60000) / 1000;
        long millis = totalMs % 1000;
        return String.format("%02d:%02d.%03d", minutes, seconds, millis);
    }
}
