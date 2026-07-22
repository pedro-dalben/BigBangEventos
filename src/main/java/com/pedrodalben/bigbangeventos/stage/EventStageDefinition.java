package com.pedrodalben.bigbangeventos.stage;

import java.util.List;
import java.util.Map;

public record EventStageDefinition(String id, String displayName, String description, int order,
                                   boolean required, boolean enabled, long timeLimitSeconds,
                                   List<String> objectiveIds, String nextStageId,
                                   boolean autoCompleteWhenObjectivesComplete, Map<String, String> metadata) {
    public EventStageDefinition {
        if (id == null || !id.matches("[a-z0-9][a-z0-9_-]{0,63}")) throw new IllegalArgumentException("ID de etapa inválido");
        if (order < 0 || timeLimitSeconds < 0) throw new IllegalArgumentException("Ordem ou limite de etapa inválido");
        displayName = displayName == null ? id : displayName;
        description = description == null ? "" : description;
        objectiveIds = objectiveIds == null ? List.of() : List.copyOf(objectiveIds);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
