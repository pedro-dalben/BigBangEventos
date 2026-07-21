package com.pedrodalben.bigbangeventos.api;

import java.util.Map;

public record OperationResult(boolean success, String code, String message, Map<String, String> details) {
    public static OperationResult ok(String message) { return new OperationResult(true, "ok", message, Map.of()); }
    public static OperationResult fail(String code, String message) { return new OperationResult(false, code, message, Map.of()); }
}
