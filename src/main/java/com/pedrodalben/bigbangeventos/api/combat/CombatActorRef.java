package com.pedrodalben.bigbangeventos.api.combat;

import java.util.UUID;

public record CombatActorRef(
    String actorId,
    UUID entityId,
    CombatActorType actorType,
    String providerId,
    String actorKind
) {}
