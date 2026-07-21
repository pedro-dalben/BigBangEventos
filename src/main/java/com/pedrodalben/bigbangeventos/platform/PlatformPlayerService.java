package com.pedrodalben.bigbangeventos.platform;

import com.pedrodalben.bigbangeventos.api.OperationResult;
import java.util.Optional;
import java.util.UUID;

public interface PlatformPlayerService {
    Optional<UUID> findOnlineUuidByName(String playerName);
    boolean isOnline(UUID playerId);
    OperationResult sendMessage(UUID playerId, String message);
    OperationResult sendTitle(UUID playerId, String title, String subtitle);
    OperationResult sendActionBar(UUID playerId, String message);
    Optional<StoredLocation> captureLocation(UUID playerId);
    Optional<String> captureGameMode(UUID playerId);
    boolean hasFlyEnabled(UUID playerId);
}
