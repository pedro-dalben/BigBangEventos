package com.pedrodalben.bigbangeventos.snapshot;

import com.pedrodalben.bigbangeventos.platform.StoredLocation;

import java.util.*;

public interface SnapshotGateway {
    PlayerSnapshot capture(UUID playerId, UUID snapshotId, UUID sessionId);
    boolean restoreState(UUID playerId, PlayerSnapshot snapshot);
    boolean restoreInventory(UUID playerId, PlayerSnapshot snapshot);
    boolean restoreArmor(UUID playerId, PlayerSnapshot snapshot);
    void clearInventory(UUID playerId);
    Optional<StoredLocation> captureLocation(UUID playerId);

    /** Serialize an item from an inventory slot. Key: slot index, Value: serialized NBT. */
    Map<String, String> serializeInventoryItems(UUID playerId);
    Map<String, String> serializeArmorItems(UUID playerId);
    String serializeOffhandItem(UUID playerId);

    /** Capture non-inventory state from the player. */
    String captureGameMode(UUID playerId);
    int captureTotalExperience(UUID playerId);
    int captureExperienceLevel(UUID playerId);
    float captureExperienceProgress(UUID playerId);
    double captureHealth(UUID playerId);
    double captureAbsorption(UUID playerId);
    int captureFoodLevel(UUID playerId);
    float captureSaturation(UUID playerId);
    boolean captureAllowFlight(UUID playerId);
    boolean captureIsFlying(UUID playerId);
    float captureFlySpeed(UUID playerId);
    float captureWalkSpeed(UUID playerId);
    int captureSelectedSlot(UUID playerId);
    int captureFireTicks(UUID playerId);
    float captureFallDistance(UUID playerId);
    String captureActiveEffects(UUID playerId);
}
