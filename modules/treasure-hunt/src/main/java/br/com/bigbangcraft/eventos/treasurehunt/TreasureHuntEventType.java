package br.com.bigbangcraft.eventos.treasurehunt;

import com.pedrodalben.bigbangeventos.definition.EventDefinition;
import com.pedrodalben.bigbangeventos.eventtype.EventType;
import com.pedrodalben.bigbangeventos.session.EventSession;
import com.pedrodalben.bigbangeventos.validation.ValidationResult;

public final class TreasureHuntEventType implements EventType {
    private final TreasureHuntValidator validator; private final TreasureHuntSessionService sessions;
    public TreasureHuntEventType(TreasureHuntValidator validator,TreasureHuntSessionService sessions){this.validator=validator;this.sessions=sessions;}
    public String id(){return "treasure_hunt";} public String displayName(){return "Caça ao Tesouro";}
    public ValidationResult validate(EventDefinition d){return validator.validate(d);}
    public void onSessionStart(EventSession s){sessions.start(s);} public void onSessionFinish(EventSession s){sessions.finish(s);} public void onSessionCancel(EventSession s,String reason){sessions.cancel(s,reason);}
}
