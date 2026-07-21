package com.pedrodalben.bigbangeventos.validation;
public record ValidationIssue(ValidationLevel level, String code, String message) { }
