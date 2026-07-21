package com.pedrodalben.bigbangeventos.snapshot;

import com.pedrodalben.bigbangeventos.platform.StoredLocation;

import java.util.*;

public final class PlayerSnapshot {
    private final UUID snapshotId;
    private final UUID playerId;
    private final UUID sessionId;
    private final StoredLocation originalLocation;
    private final Map<String, String> serializedInventory;
    private final Map<String, String> serializedArmor;
    private final String serializedOffhand;
    private final int totalExperience;
    private final int experienceLevel;
    private final float experienceProgress;
    private final double health;
    private final double absorption;
    private final int foodLevel;
    private final float saturation;
    private final String gameMode;
    private final boolean allowFlight;
    private final boolean isFlying;
    private final float flySpeed;
    private final float walkSpeed;
    private final int selectedSlot;
    private final int fireTicks;
    private final float fallDistance;
    private final String activeEffects;
    private final Map<String, String> extendedData;

    private SnapshotState state;
    private final Set<RestoreComponent> restoredComponents;
    private final long createdAtMs;
    private String exitDestination; // LOBBY, EXIT, ORIGINAL, etc.

    public PlayerSnapshot(UUID snapshotId, UUID playerId, UUID sessionId,
                          StoredLocation originalLocation, Map<String, String> serializedInventory,
                          Map<String, String> serializedArmor, String serializedOffhand,
                          int totalExperience, int experienceLevel, float experienceProgress,
                          double health, double absorption, int foodLevel, float saturation,
                          String gameMode, boolean allowFlight, boolean isFlying,
                          float flySpeed, float walkSpeed, int selectedSlot,
                          int fireTicks, float fallDistance, String activeEffects,
                          Map<String, String> extendedData) {
        this.snapshotId = snapshotId;
        this.playerId = playerId;
        this.sessionId = sessionId;
        this.originalLocation = originalLocation;
        this.serializedInventory = Map.copyOf(serializedInventory);
        this.serializedArmor = Map.copyOf(serializedArmor);
        this.serializedOffhand = serializedOffhand;
        this.totalExperience = totalExperience;
        this.experienceLevel = experienceLevel;
        this.experienceProgress = experienceProgress;
        this.health = health;
        this.absorption = absorption;
        this.foodLevel = foodLevel;
        this.saturation = saturation;
        this.gameMode = gameMode;
        this.allowFlight = allowFlight;
        this.isFlying = isFlying;
        this.flySpeed = flySpeed;
        this.walkSpeed = walkSpeed;
        this.selectedSlot = selectedSlot;
        this.fireTicks = fireTicks;
        this.fallDistance = fallDistance;
        this.activeEffects = activeEffects;
        this.extendedData = Map.copyOf(extendedData);
        this.state = SnapshotState.CAPTURED;
        this.restoredComponents = EnumSet.noneOf(RestoreComponent.class);
        this.createdAtMs = System.currentTimeMillis();
    }

    public UUID snapshotId() { return snapshotId; }
    public UUID playerId() { return playerId; }
    public UUID sessionId() { return sessionId; }
    public StoredLocation originalLocation() { return originalLocation; }
    public Map<String, String> serializedInventory() { return serializedInventory; }
    public Map<String, String> serializedArmor() { return serializedArmor; }
    public String serializedOffhand() { return serializedOffhand; }
    public int totalExperience() { return totalExperience; }
    public int experienceLevel() { return experienceLevel; }
    public float experienceProgress() { return experienceProgress; }
    public double health() { return health; }
    public double absorption() { return absorption; }
    public int foodLevel() { return foodLevel; }
    public float saturation() { return saturation; }
    public String gameMode() { return gameMode; }
    public boolean allowFlight() { return allowFlight; }
    public boolean isFlying() { return isFlying; }
    public float flySpeed() { return flySpeed; }
    public float walkSpeed() { return walkSpeed; }
    public int selectedSlot() { return selectedSlot; }
    public int fireTicks() { return fireTicks; }
    public float fallDistance() { return fallDistance; }
    public String activeEffects() { return activeEffects; }
    public Map<String, String> extendedData() { return extendedData; }
    public SnapshotState state() { return state; }
    public Set<RestoreComponent> restoredComponents() { return Collections.unmodifiableSet(restoredComponents); }
    public long createdAtMs() { return createdAtMs; }
    public Optional<String> exitDestination() { return Optional.ofNullable(exitDestination); }

    public void state(SnapshotState s) { this.state = s; }
    public void markComponentRestored(RestoreComponent c) { restoredComponents.add(c); }
    public boolean isComponentRestored(RestoreComponent c) { return restoredComponents.contains(c); }
    public void exitDestination(String dest) { this.exitDestination = dest; }
}
