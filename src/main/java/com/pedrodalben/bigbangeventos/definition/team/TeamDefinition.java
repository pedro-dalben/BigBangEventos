package com.pedrodalben.bigbangeventos.definition.team;

import com.pedrodalben.bigbangeventos.definition.EventLocation;
import java.util.Map;

public record TeamDefinition(
    String id,
    String displayName,
    String color,
    int minimumPlayers,
    int maximumPlayers,
    EventLocation spawnLocation,
    boolean enabled,
    Map<String, String> metadata
) {}
