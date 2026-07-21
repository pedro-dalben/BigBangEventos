package com.pedrodalben.bigbangeventos.eventtype;

import com.pedrodalben.bigbangeventos.definition.EventDefinition;
import com.pedrodalben.bigbangeventos.session.EventSession;
import com.pedrodalben.bigbangeventos.validation.ValidationResult;

public interface EventType {
    String id();
    String displayName();
    default ValidationResult validate(EventDefinition definition) { return ValidationResult.empty(); }
    default void onSessionCreated(EventSession session) { }
    default void onRegistrationOpen(EventSession session) { }
    default void onSessionStart(EventSession session) { }
    default void onSessionFinish(EventSession session) { }
    default void onSessionCancel(EventSession session, String reason) { }
}
