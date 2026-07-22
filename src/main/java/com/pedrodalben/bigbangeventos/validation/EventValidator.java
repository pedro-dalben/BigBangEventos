package com.pedrodalben.bigbangeventos.validation;
import com.pedrodalben.bigbangeventos.definition.*;
import com.pedrodalben.bigbangeventos.eventtype.EventTypeRegistry;
import com.pedrodalben.bigbangeventos.objective.*;
import java.util.*;
public final class EventValidator {
    private final EventTypeRegistry types; private final ObjectiveTypeRegistry objectiveTypes;
    public EventValidator(EventTypeRegistry types){this(types,new ObjectiveTypeRegistry());}
    public EventValidator(EventTypeRegistry types,ObjectiveTypeRegistry objectiveTypes){this.types=types;this.objectiveTypes=objectiveTypes;}
    public ValidationResult validate(EventDefinition d) { ValidationResult r=ValidationResult.empty(); if(types.find(d.type()).isEmpty())r.add(ValidationLevel.ERROR,"unknown_type","Tipo não registrado: "+d.type()); if(!d.enabled())r.add(ValidationLevel.WARNING,"disabled","Evento está desabilitado"); for(LocationName required: new LocationName[]{LocationName.LOBBY,LocationName.ENTRANCE,LocationName.EXIT}) if(d.location(required).isEmpty())r.add(ValidationLevel.ERROR,"missing_location","Localização obrigatória ausente: "+required); if(d.maxPlayers()>0&&d.maxPlayers()<d.minPlayers())r.add(ValidationLevel.ERROR,"invalid_limits","Limites de jogadores inválidos"); validateProgress(d,r); types.find(d.type()).ifPresent(t->r.merge(t.validate(d))); return r; }
    private void validateProgress(EventDefinition d,ValidationResult r){Set<Integer> orders=new HashSet<>();for(var s:d.stages()){if(!orders.add(s.order()))r.add(ValidationLevel.ERROR,"duplicate_stage_order","Ordem de etapa duplicada");if(s.nextStageId()!=null&&!s.nextStageId().isBlank()&&d.stage(s.nextStageId()).isEmpty())r.add(ValidationLevel.ERROR,"unknown_next_stage","Próxima etapa inexistente: "+s.nextStageId());}for(var o:d.objectives()){if(d.stage(o.stageId()).isEmpty())r.add(ValidationLevel.ERROR,"unknown_stage","Etapa inexistente: "+o.stageId());if(objectiveTypes.find(o.typeId()).isEmpty())r.add(ValidationLevel.ERROR,"unknown_objective_type","Tipo de objetivo inexistente: "+o.typeId());if(o.scope()==ObjectiveScope.TEAM)r.add(ValidationLevel.ERROR,"unsupported_scope","TEAM ainda não suportado");}detectCycles(d,r);}
    private void detectCycles(EventDefinition d,ValidationResult r){for(var stage:d.stages()){Set<String> path=new HashSet<>();String id=stage.id();while(id!=null&&!id.isBlank()){if(!path.add(id)){r.add(ValidationLevel.ERROR,"stage_cycle","Ciclo de etapas detectado");break;}id=d.stage(id).map(com.pedrodalben.bigbangeventos.stage.EventStageDefinition::nextStageId).orElse(null);}}}
}
