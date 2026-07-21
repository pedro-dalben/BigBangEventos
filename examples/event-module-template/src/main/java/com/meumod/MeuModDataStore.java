package com.meumod;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MeuModDataStore {
    private final Path dataDir;
    private final Map<String, Integer> cache = new ConcurrentHashMap<>();

    public MeuModDataStore(Path dataDir) {
        this.dataDir = dataDir;
    }

    public void save(UUID playerId, UUID sessionId, int pontos) {
        cache.put(key(playerId, sessionId), pontos);
    }

    public int get(UUID playerId, UUID sessionId) {
        return cache.getOrDefault(key(playerId, sessionId), 0);
    }

    public void flushAll() {
        // persistir em YAML
    }

    private static String key(UUID playerId, UUID sessionId) {
        return playerId + ":" + sessionId;
    }
}
