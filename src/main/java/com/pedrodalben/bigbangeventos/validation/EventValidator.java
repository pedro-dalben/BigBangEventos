package com.pedrodalben.bigbangeventos.validation;
import com.pedrodalben.bigbangeventos.definition.*;
import com.pedrodalben.bigbangeventos.eventtype.EventTypeRegistry;
public final class EventValidator {
    private final EventTypeRegistry types; public EventValidator(EventTypeRegistry types){this.types=types;}
    public ValidationResult validate(EventDefinition d) { ValidationResult r=ValidationResult.empty(); if(types.find(d.type()).isEmpty())r.add(ValidationLevel.ERROR,"unknown_type","Tipo não registrado: "+d.type()); if(!d.enabled())r.add(ValidationLevel.WARNING,"disabled","Evento está desabilitado"); for(LocationName required: new LocationName[]{LocationName.LOBBY,LocationName.ENTRANCE,LocationName.EXIT}) if(d.location(required).isEmpty())r.add(ValidationLevel.ERROR,"missing_location","Localização obrigatória ausente: "+required); if(d.maxPlayers()>0&&d.maxPlayers()<d.minPlayers())r.add(ValidationLevel.ERROR,"invalid_limits","Limites de jogadores inválidos"); types.find(d.type()).ifPresent(t->r.merge(t.validate(d))); return r; }
}
