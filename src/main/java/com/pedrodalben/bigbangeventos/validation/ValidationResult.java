package com.pedrodalben.bigbangeventos.validation;
import java.util.*;
public final class ValidationResult {
    private final List<ValidationIssue> issues = new ArrayList<>();
    public static ValidationResult empty(){return new ValidationResult();} public void add(ValidationLevel level,String code,String message){issues.add(new ValidationIssue(level,code,message));} public void merge(ValidationResult other){issues.addAll(other.issues);} public List<ValidationIssue> issues(){return List.copyOf(issues);} public boolean valid(){return issues.stream().noneMatch(i->i.level()==ValidationLevel.ERROR);}
}
