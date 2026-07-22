package com.pedrodalben.bigbangeventos.stage;

import com.pedrodalben.bigbangeventos.api.OperationResult;

public record StageTransitionResult(OperationResult result, SessionStageProgress progress) {
    public boolean success(){return result.success();}
    public static StageTransitionResult ok(String message, SessionStageProgress progress){return new StageTransitionResult(OperationResult.ok(message),progress);}
    public static StageTransitionResult fail(String code,String message,SessionStageProgress progress){return new StageTransitionResult(OperationResult.fail(code,message),progress);}
}
