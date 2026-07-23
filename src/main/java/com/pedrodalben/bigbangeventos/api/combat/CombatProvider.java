package com.pedrodalben.bigbangeventos.api.combat;

import java.util.*;

public interface CombatProvider {

    String id();

    String displayName();

    Set<ProviderCapability> capabilities();

    default CombatOwnership resolveOwnership(UUID entityId) {
        throw new UnsupportedOperationException(id() + " não suporta resolveOwnership");
    }

    default CombatActorRef resolveActor(UUID entityId) {
        throw new UnsupportedOperationException(id() + " não suporta resolveActor");
    }

    default List<CombatActorRef> listActiveActors(UUID sessionId) {
        return List.of();
    }

    default void clearTarget(UUID actorId) {
        throw new UnsupportedOperationException(id() + " não suporta clearTarget");
    }

    default void clearAllTargets(UUID sessionId) {
        throw new UnsupportedOperationException(id() + " não suporta clearAllTargets");
    }

    default void recallEntity(UUID actorId) {
        throw new UnsupportedOperationException(id() + " não suporta recallEntity");
    }

    default void recallAllInSession(UUID sessionId) {
        throw new UnsupportedOperationException(id() + " não suporta recallAllInSession");
    }

    default Map<String, Object> captureState(UUID actorId) {
        throw new UnsupportedOperationException(id() + " não suporta captureState");
    }

    default void restoreState(UUID actorId, Map<String, Object> state) {
        throw new UnsupportedOperationException(id() + " não suporta restoreState");
    }

    default boolean isAvailable() { return true; }

    default void onSessionStart(UUID sessionId) {}

    default void onSessionFinish(UUID sessionId) {}

    default void cleanup(UUID sessionId) {}
}
