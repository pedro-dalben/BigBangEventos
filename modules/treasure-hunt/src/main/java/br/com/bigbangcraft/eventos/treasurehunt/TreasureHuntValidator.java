package br.com.bigbangcraft.eventos.treasurehunt;

import com.pedrodalben.bigbangeventos.definition.*;
import com.pedrodalben.bigbangeventos.objective.*;
import com.pedrodalben.bigbangeventos.stage.*;
import com.pedrodalben.bigbangeventos.validation.*;
import java.util.*;

public final class TreasureHuntValidator {
    private final ObjectiveTypeRegistry types;
    public TreasureHuntValidator(ObjectiveTypeRegistry types){this.types=types;}
    public ValidationResult validate(EventDefinition d){
        var r=ValidationResult.empty();
        if(d.location(LocationName.LOBBY).isEmpty())r.add(ValidationLevel.ERROR,"missing_start","Start ausente");
        if(d.location(LocationName.EXIT).isEmpty())r.add(ValidationLevel.ERROR,"missing_finish","Finish ausente");
        if(d.stages().isEmpty())r.add(ValidationLevel.ERROR,"no_stages","Nenhuma etapa configurada");
        Set<Integer> orders=new HashSet<>(); Set<String> ids=new HashSet<>();
        for(var s:d.stages()){
            if(!ids.add(s.id()))r.add(ValidationLevel.ERROR,"duplicate_stage","Etapa duplicada: "+s.id());
            if(!orders.add(s.order()))r.add(ValidationLevel.ERROR,"duplicate_order","Ordem de etapa duplicada: "+s.order());
            if(s.required()&&s.objectiveIds().isEmpty())r.add(ValidationLevel.ERROR,"stage_without_objective","Etapa obrigatória sem objetivo: "+s.id());
            if(s.nextStageId()!=null&&!s.nextStageId().isBlank()&&d.stage(s.nextStageId()).isEmpty())r.add(ValidationLevel.ERROR,"unknown_next_stage","Próxima etapa inexistente: "+s.nextStageId());
        }
        for(var o:d.objectives()){
            if(d.stage(o.stageId()).isEmpty())r.add(ValidationLevel.ERROR,"unknown_stage","Objetivo aponta para etapa inexistente: "+o.id());
            if(o.target()<0)r.add(ValidationLevel.ERROR,"invalid_target","Meta inválida: "+o.id());
            if(types.find(o.typeId()).isEmpty())r.add(ValidationLevel.ERROR,"unknown_objective_type","Tipo inexistente: "+o.typeId());
        }
        detectCycle(d,r); return r;
    }
    private void detectCycle(EventDefinition d,ValidationResult r){Set<String> done=new HashSet<>();for(var s:d.stages()){Set<String> path=new HashSet<>();String id=s.id();while(id!=null&&!id.isBlank()){if(!path.add(id)){r.add(ValidationLevel.ERROR,"stage_cycle","Ciclo de etapas detectado");break;}if(!done.add(id))break;id=d.stage(id).map(EventStageDefinition::nextStageId).orElse(null);}}}
}
