package com.pedrodalben.bigbangeventos.api.combat;

import java.util.UUID;

public record CombatOwnership(
    UUID ownerPlayerId,
    UUID persistentSubjectId,
    String providerId,
    String actorKind
) {}
