package com.pedrodalben.bigbangeventos.trigger;

import com.pedrodalben.bigbangeventos.api.OperationResult;
import com.pedrodalben.bigbangeventos.objective.*;
import com.pedrodalben.bigbangeventos.participant.*;
import com.pedrodalben.bigbangeventos.session.*;
import com.pedrodalben.bigbangeventos.stage.*;
import java.time.*;
import java.util.*;

public final class TriggerService {
    private final Clock clock; private final ParticipantCompletionService completion;
    private ObjectiveService objectives; private StageService stages;
    private final Map<String,Integer> uses=new HashMap<>(); private final Map<String,Instant> lastUse=new HashMap<>();
    public TriggerService(Clock clock,ParticipantCompletionService completion){this.clock=clock;this.completion=completion;}
    public void services(ObjectiveService objectives,StageService stages){this.objectives=objectives;this.stages=stages;}
    public synchronized OperationResult execute(EventTrigger t,TriggerExecutionContext c){
        if(!t.enabled())return OperationResult.fail("trigger_disabled","Gatilho desabilitado");
        String key=c.session().id()+":"+t.id(); if(t.maxUses()>0&&uses.getOrDefault(key,0)>=t.maxUses())return OperationResult.fail("max_uses","Limite de usos atingido");
        Instant previous=lastUse.get(key);if(previous!=null&&previous.plus(t.cooldown()).isAfter(clock.instant()))return OperationResult.fail("cooldown","Gatilho em cooldown");
        for(ConditionType condition:t.conditions()){OperationResult r=condition(condition,c,t.conditionArguments(condition));if(!r.success())return r;}
        for(TriggerAction action:t.actions()){OperationResult r=action(action,c);if(!r.success())return r;}
        uses.merge(key,1,Integer::sum);lastUse.put(key,clock.instant());return OperationResult.ok("Gatilho executado");
    }
    private OperationResult condition(ConditionType x,TriggerExecutionContext c,Map<String,String> a){
        EventParticipant p=c.session().participant(c.playerId()).orElse(null);
        if(c.session().state()!=SessionState.RUNNING&&x!=ConditionType.PLAYER_HAS_PERMISSION)return OperationResult.fail("condition_failed","Evento não está em execução");
        return switch(x){
            case PLAYER_IS_PARTICIPANT -> p==null?fail("Jogador não participa"):ok();
            case EVENT_IS_RUNNING -> ok();
            case PLAYER_NOT_FINISHED -> p==null||p.state()==ParticipantState.FINISHED?fail("Jogador já concluiu"):ok();
            case PLAYER_HAS_PERMISSION -> c.permissions().has(c.playerId(),"bigbangeventos.player.trigger")?ok():fail("Sem permissão");
            case VARIABLE_EXISTS -> p!=null&&p.dataValue(a.getOrDefault("key","")).isPresent()?ok():fail("Variável ausente");
            case VARIABLE_EQUALS -> p!=null&&Objects.equals(p.dataValue(a.getOrDefault("key","")).orElse(null),a.get("value"))?ok():fail("Variável diferente");
            case VARIABLE_AT_LEAST -> numberCondition(p,a,"Variável insuficiente");
            case SCORE_AT_LEAST -> scoreCondition(p,a);
            case OBJECTIVE_IS_ACTIVE, OBJECTIVE_IS_COMPLETED, OBJECTIVE_IS_FAILED -> objectiveState(x,c,a);
            case OBJECTIVE_PROGRESS_AT_LEAST -> objectiveAmount(c,a);
            case STAGE_IS_ACTIVE, STAGE_IS_COMPLETED, STAGE_IS_FAILED -> stageState(x,c,a);
            default -> ok();
        };
    }
    private OperationResult objectiveState(ConditionType x,TriggerExecutionContext c,Map<String,String> a){
        if(objectives==null)return fail("Serviço de objetivos indisponível");
        var progress=objectives.getProgress(c.session(),a.getOrDefault("objective",""),c.playerId());
        ObjectiveStatus expected=x==ConditionType.OBJECTIVE_IS_ACTIVE?ObjectiveStatus.IN_PROGRESS:x==ConditionType.OBJECTIVE_IS_COMPLETED?ObjectiveStatus.COMPLETED:ObjectiveStatus.FAILED;
        return progress.map(v->{boolean matches=v.status()==expected||(x==ConditionType.OBJECTIVE_IS_ACTIVE&&v.status()==ObjectiveStatus.AVAILABLE);return matches?ok():fail("Estado do objetivo não corresponde");}).orElse(fail("Objetivo sem progresso"));
    }
    private OperationResult objectiveAmount(TriggerExecutionContext c,Map<String,String> a){try{long n=Long.parseLong(a.getOrDefault("amount","0"));return objectives==null?fail("Serviço de objetivos indisponível"):objectives.getProgress(c.session(),a.getOrDefault("objective",""),c.playerId()).map(v->v.current()>=n?ok():fail("Progresso insuficiente")).orElse(fail("Objetivo sem progresso"));}catch(NumberFormatException e){return OperationResult.fail("invalid_condition","Quantidade inválida");}}
    private OperationResult stageState(ConditionType x,TriggerExecutionContext c,Map<String,String> a){if(stages==null)return fail("Serviço de etapas indisponível");var p=c.session().stageProgress().get(a.getOrDefault("stage",""));StageStatus expected=x==ConditionType.STAGE_IS_ACTIVE?StageStatus.ACTIVE:x==ConditionType.STAGE_IS_COMPLETED?StageStatus.COMPLETED:StageStatus.FAILED;return p!=null&&p.status()==expected?ok():fail("Estado da etapa não corresponde");}
    private OperationResult numberCondition(EventParticipant p,Map<String,String> a,String message){try{long n=Long.parseLong(a.getOrDefault("amount","0"));return p!=null&&p.dataValue(a.getOrDefault("key","")).map(v->Long.parseLong(v)>=n).orElse(false)?ok():fail(message);}catch(NumberFormatException e){return OperationResult.fail("invalid_condition","Quantidade inválida");}}
    private OperationResult scoreCondition(EventParticipant p,Map<String,String> a){try{return p!=null&&p.score()>=Long.parseLong(a.getOrDefault("amount","0"))?ok():fail("Pontuação insuficiente");}catch(NumberFormatException e){return OperationResult.fail("invalid_condition","Quantidade inválida");}}
    private OperationResult action(TriggerAction a,TriggerExecutionContext c){
        EventParticipant p=c.session().participant(c.playerId()).orElse(null);
        return switch(a.type()){
            case SEND_MESSAGE->{c.effects().message(c.playerId(),a.arguments().getOrDefault("message",""));yield ok();}
            case EXECUTE_COMMAND->{c.effects().executeConsole(a.arguments().getOrDefault("command","").replace("{player}",c.playerName()));yield ok();}
            case ADD_POINTS->{if(p==null)yield fail("Jogador não participa");try{p.score(Integer.parseInt(a.arguments().getOrDefault("amount","0")));yield ok();}catch(NumberFormatException e){yield OperationResult.fail("invalid_action","Quantidade inválida");}}
            case ADD_SCORE,REMOVE_SCORE,SET_SCORE->{if(p==null)yield fail("Jogador não participa");try{int v=Integer.parseInt(a.arguments().getOrDefault("amount",a.arguments().getOrDefault("value","0")));if(a.type()==ActionType.REMOVE_SCORE)v=-v;if(a.type()==ActionType.SET_SCORE)p.restoreScore(v);else p.score(v);yield ok();}catch(NumberFormatException e){yield OperationResult.fail("invalid_action","Pontuação inválida");}}
            case SET_VARIABLE,INCREMENT_VARIABLE,REMOVE_VARIABLE->{if(p==null)yield fail("Jogador não participa");String key=a.arguments().getOrDefault("key","");if(key.isBlank())yield fail("Chave obrigatória");if(a.type()==ActionType.REMOVE_VARIABLE)p.removeData(key);else try{long v=a.type()==ActionType.INCREMENT_VARIABLE?Long.parseLong(p.dataValue(key).orElse("0"))+Long.parseLong(a.arguments().getOrDefault("amount","1")):Long.parseLong(a.arguments().getOrDefault("value","0"));p.data(key,Long.toString(v));}catch(NumberFormatException e){p.data(key,a.arguments().getOrDefault("value",""));}yield ok();}
            case PLAYER_COMPLETE->completion.complete(c.session(),c.playerId(),CompletionMode.MANUAL_FINISH);
            case ACTIVATE_OBJECTIVE,ADD_OBJECTIVE_PROGRESS,SET_OBJECTIVE_PROGRESS,COMPLETE_OBJECTIVE,FAIL_OBJECTIVE,SKIP_OBJECTIVE->{if(objectives==null||c.definition()==null)yield fail("Serviço de objetivos indisponível");String id=a.arguments().getOrDefault("objective","");try{yield switch(a.type()){case ACTIVATE_OBJECTIVE->objectives.activate(c.definition(),c.session(),id,c.playerId(),"trigger").result();case ADD_OBJECTIVE_PROGRESS->objectives.addProgress(c.definition(),c.session(),id,c.playerId(),Long.parseLong(a.arguments().getOrDefault("amount","1")),"trigger").result();case SET_OBJECTIVE_PROGRESS->objectives.setProgress(c.definition(),c.session(),id,c.playerId(),Long.parseLong(a.arguments().getOrDefault("amount","0")),"trigger").result();case COMPLETE_OBJECTIVE->objectives.complete(c.definition(),c.session(),id,c.playerId(),"trigger").result();case FAIL_OBJECTIVE->objectives.fail(c.definition(),c.session(),id,c.playerId(),"trigger").result();default->objectives.skip(c.definition(),c.session(),id,c.playerId(),"trigger").result();};}catch(NumberFormatException e){yield OperationResult.fail("invalid_action","Progresso inválido");}}
            case ACTIVATE_STAGE->stages==null||c.definition()==null?fail("Serviço de etapas indisponível"):stages.activateStage(c.definition(),c.session(),a.arguments().getOrDefault("stage","" )).result();
            case ADVANCE_STAGE->stages==null||c.definition()==null?fail("Serviço de etapas indisponível"):stages.advance(c.definition(),c.session()).result();
            case COMPLETE_STAGE_GENERIC->stages==null||c.definition()==null?fail("Serviço de etapas indisponível"):stages.completeStage(c.definition(),c.session(),a.arguments().getOrDefault("stage","" )).result();
            case FAIL_STAGE->stages==null||c.definition()==null?fail("Serviço de etapas indisponível"):stages.failStage(c.definition(),c.session(),a.arguments().getOrDefault("stage","" )).result();
            case ENABLE_TRIGGER,DISABLE_TRIGGER,TELEPORT->ok();
            default->OperationResult.fail("not_implemented","Ação ainda não implementada: "+a.type());
        };
    }
    private static OperationResult ok(){return OperationResult.ok("ok");} private static OperationResult fail(String m){return OperationResult.fail("condition_failed",m);} private static OperationResult fail(String c,String m){return OperationResult.fail(c,m);}
}
