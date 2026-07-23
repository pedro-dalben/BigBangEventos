package com.pedrodalben.bigbangeventos.api.combat;

import java.util.Map;
import java.util.UUID;

public final class CombatActor {
    private final CombatActorRef ref;
    private final UUID ownerPlayerId;
    private final UUID persistentSubjectId;
    private final String displayName;
    private final UUID sessionId;
    private final UUID teamId;
    private final Map<String, String> metadata;

    public CombatActor(CombatActorRef ref, UUID ownerPlayerId, UUID persistentSubjectId,
                       String displayName, UUID sessionId, UUID teamId,
                       Map<String, String> metadata) {
        this.ref = ref;
        this.ownerPlayerId = ownerPlayerId;
        this.persistentSubjectId = persistentSubjectId;
        this.displayName = displayName;
        this.sessionId = sessionId;
        this.teamId = teamId;
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public CombatActorRef ref() { return ref; }
    public UUID ownerPlayerId() { return ownerPlayerId; }
    public UUID persistentSubjectId() { return persistentSubjectId; }
    public String displayName() { return displayName; }
    public UUID sessionId() { return sessionId; }
    public UUID teamId() { return teamId; }
    public Map<String, String> metadata() { return metadata; }
}
