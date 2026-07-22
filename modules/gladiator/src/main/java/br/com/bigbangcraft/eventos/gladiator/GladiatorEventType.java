package br.com.bigbangcraft.eventos.gladiator;

import com.pedrodalben.bigbangeventos.api.OperationResult;
import com.pedrodalben.bigbangeventos.eventtype.EventType;
import com.pedrodalben.bigbangeventos.session.EventSession;
import java.util.UUID;

public final class GladiatorEventType implements EventType {
    @Override public String id() { return "gladiator"; }
    @Override public String displayName() { return "Gladiator"; }

    @Override
    public void onSessionCreated(EventSession session) {}

    @Override
    public void onRegistrationOpen(EventSession session) {}

    @Override
    public void onSessionStart(EventSession session) {}

    @Override
    public void onSessionFinish(EventSession session) {}

    @Override
    public void onSessionCancel(EventSession session, String reason) {}

    @Override
    public void onTriggerFired(EventSession session, com.pedrodalben.bigbangeventos.trigger.EventTrigger trigger, UUID player) {}
}
