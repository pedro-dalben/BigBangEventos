package br.com.bigbangcraft.eventos.pokegladiator;

import com.pedrodalben.bigbangeventos.api.module.EventModuleContext;
import com.pedrodalben.bigbangeventos.definition.EventDefinition;
import com.pedrodalben.bigbangeventos.eventtype.EventType;
import com.pedrodalben.bigbangeventos.session.EventSession;
import com.pedrodalben.bigbangeventos.validation.ValidationResult;

public class PokeGladiatorEventType implements EventType {

    private final PokeGladiatorSessionService sessionService;
    private final PokeGladiatorValidator validator;

    public PokeGladiatorEventType(EventModuleContext ctx) {
        this.sessionService = new PokeGladiatorSessionService(ctx);
        this.validator = new PokeGladiatorValidator();
    }

    @Override public String id() { return "poke_gladiator"; }
    @Override public String displayName() { return "PokeGladiator"; }

    @Override
    public ValidationResult validate(EventDefinition definition) {
        return validator.validate(definition);
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
        sessionService.onSessionCancel(session);
    }
}
