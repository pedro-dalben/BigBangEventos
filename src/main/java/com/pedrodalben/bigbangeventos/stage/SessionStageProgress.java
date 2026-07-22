package com.pedrodalben.bigbangeventos.stage;

import java.time.Instant;

public final class SessionStageProgress {
    private final String stageId;
    private StageStatus status;
    private Instant startedAt, completedAt, failedAt, deadline, updatedAt;
    public SessionStageProgress(String stageId, StageStatus status, Instant updatedAt) { this.stageId=stageId;this.status=status;this.updatedAt=updatedAt; }
    public String stageId(){return stageId;} public StageStatus status(){return status;} public Instant startedAt(){return startedAt;} public Instant completedAt(){return completedAt;} public Instant failedAt(){return failedAt;} public Instant deadline(){return deadline;} public Instant updatedAt(){return updatedAt;}
    public void activate(Instant now, Instant deadline){if(startedAt==null)startedAt=now;status=StageStatus.ACTIVE;this.deadline=deadline;updatedAt=now;}
    public void complete(Instant now){status=StageStatus.COMPLETED;completedAt=completedAt==null?now:completedAt;updatedAt=now;}
    public void fail(Instant now){status=StageStatus.FAILED;failedAt=failedAt==null?now:failedAt;updatedAt=now;}
    public void skip(Instant now){status=StageStatus.SKIPPED;updatedAt=now;}
}
