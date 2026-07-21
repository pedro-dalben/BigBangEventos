package br.com.bigbangcraft.eventos.parkour;

import com.pedrodalben.bigbangeventos.definition.EventDefinition;
import com.pedrodalben.bigbangeventos.eventtype.EventType;
import com.pedrodalben.bigbangeventos.session.EventSession;
import com.pedrodalben.bigbangeventos.validation.ValidationResult;

public final class ParkourEventType implements EventType {

    private final ParkourSessionService sessionService;
    private final ParkourValidator validator;

    public ParkourEventType(ParkourSessionService sessionService, ParkourValidator validator) {
        this.sessionService = sessionService;
        this.validator = validator;
    }

    @Override
    public String id() {
        return "parkour";
    }

    @Override
    public String displayName() {
        return "Parkour";
    }

    @Override
    public ValidationResult validate(EventDefinition definition) {
        return validator.validate(definition);
    }

    @Override
    public void onSessionCreated(EventSession session) {
    }

    @Override
    public void onRegistrationOpen(EventSession session) {
    }

    @Override
    public void onSessionStart(EventSession session) {
        sessionService.onSessionStart(session);
    }

    @Override
    public void onSessionFinish(EventSession session) {
        sessionService.onSessionFinish(session);
    }

    @Override
    public void onSessionCancel(EventSession session, String reason) {
        sessionService.onSessionCancel(session, reason);
    }
}
