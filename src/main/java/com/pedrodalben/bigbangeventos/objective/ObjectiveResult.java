package com.pedrodalben.bigbangeventos.objective;

import com.pedrodalben.bigbangeventos.api.OperationResult;

public record ObjectiveResult(OperationResult result, ObjectiveProgress progress) {
    public boolean success() { return result.success(); }
    public static ObjectiveResult ok(String message, ObjectiveProgress progress) { return new ObjectiveResult(OperationResult.ok(message), progress); }
    public static ObjectiveResult fail(String code, String message, ObjectiveProgress progress) { return new ObjectiveResult(OperationResult.fail(code, message), progress); }
}
