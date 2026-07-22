package com.pedrodalben.bigbangeventos.objective;

import com.pedrodalben.bigbangeventos.validation.ValidationResult;

public interface ObjectiveTypeHandler {
    String id();
    default ValidationResult validate(ObjectiveDefinition definition) { return ValidationResult.empty(); }
    default long initialProgress(ObjectiveDefinition definition) { return 0; }
}
