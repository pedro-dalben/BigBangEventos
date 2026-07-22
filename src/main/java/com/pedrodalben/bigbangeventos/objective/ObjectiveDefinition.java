package com.pedrodalben.bigbangeventos.objective;

import java.util.Map;
import java.util.Objects;

public record ObjectiveDefinition(
        String id, String displayName, String description, String typeId, String stageId,
        boolean required, int order, long target, ObjectiveScope scope, boolean enabled,
        Map<String, String> metadata) {
    public ObjectiveDefinition {
        if (id == null || !id.matches("[a-z0-9][a-z0-9_-]{0,63}")) throw new IllegalArgumentException("ID de objetivo inválido");
        if (typeId == null || typeId.isBlank()) throw new IllegalArgumentException("Tipo de objetivo obrigatório");
        if (stageId == null || stageId.isBlank()) throw new IllegalArgumentException("Etapa obrigatória");
        if (order < 0 || target < 0) throw new IllegalArgumentException("Ordem ou meta inválida");
        scope = Objects.requireNonNull(scope);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        displayName = displayName == null ? id : displayName;
        description = description == null ? "" : description;
    }
}
