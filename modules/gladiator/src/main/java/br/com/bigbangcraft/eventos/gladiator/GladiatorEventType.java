package br.com.bigbangcraft.eventos.gladiator;

import com.pedrodalben.bigbangeventos.api.OperationResult;
import com.pedrodalben.bigbangeventos.definition.EventDefinition;
import com.pedrodalben.bigbangeventos.eventtype.EventType;
import com.pedrodalben.bigbangeventos.session.EventSession;
import com.pedrodalben.bigbangeventos.trigger.EventTrigger;
import com.pedrodalben.bigbangeventos.validation.ValidationResult;
import java.util.UUID;

public final class GladiatorEventType implements EventType {
    private final GladiatorSessionService sessionService;
    private final GladiatorValidator validator;

    public GladiatorEventType(GladiatorSessionService sessionService, GladiatorValidator validator) {
        this.sessionService = sessionService; this.validator = validator;
    }

    @Override public String id() { return "gladiator"; }
    @Override public String displayName() { return "Gladiator"; }

    @Override
    public ValidationResult validate(EventDefinition definition) {
        return validator.validate(definition);
    }

    @Override
    public void onSessionCreated(EventSession session) {}

    @Override
    public void onRegistrationOpen(EventSession session) {}

    @Override
    public void onSessionStart(EventSession session) {
        sessionService.prepareSession(session);
        sessionService.startRound(session);
    }

    @Override
    public void onSessionFinish(EventSession session) {
        sessionService.cleanup(session);
    }

    @Override
    public void onSessionCancel(EventSession session, String reason) {
        sessionService.cleanup(session);
    }

    @Override
    public void onTriggerFired(EventSession session, EventTrigger trigger, UUID player) {}
}
