package com.meumod;

import com.pedrodalben.bigbangeventos.definition.EventDefinition;
import com.pedrodalben.bigbangeventos.eventtype.EventType;
import com.pedrodalben.bigbangeventos.session.EventSession;
import com.pedrodalben.bigbangeventos.validation.ValidationLevel;
import com.pedrodalben.bigbangeventos.validation.ValidationResult;
import java.util.Map;

public class MeuTipoEvento implements EventType {
    @Override
    public String id() {
        return "meu_tipo";
    }

    @Override
    public String displayName() {
        return "Meu Tipo de Evento";
    }

    @Override
    public ValidationResult validate(EventDefinition definition) {
        ValidationResult r = ValidationResult.empty();
        Map<String, Object> settings = definition.typeSettings();
        if (!settings.containsKey("pontos")) {
            r.add(ValidationLevel.ERROR, "missing_pontos",
                "Campo 'pontos' em typeSettings é obrigatório");
        }
        return r;
    }

    @Override
    public void onSessionCreated(EventSession session) {
    }

    @Override
    public void onRegistrationOpen(EventSession session) {
    }

    @Override
    public void onSessionStart(EventSession session) {
    }

    @Override
    public void onSessionFinish(EventSession session) {
    }

    @Override
    public void onSessionCancel(EventSession session, String reason) {
    }
}
