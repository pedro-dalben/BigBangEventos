package com.pedrodalben.bigbangeventos.session.round;

import java.time.Instant;
import java.util.*;

public final class SessionRound {
    private final UUID roundId;
    private final String eventId;
    private final UUID sessionId;
    private final int number;
    private RoundState state;
    private Instant startedAt;
    private Instant finishedAt;
    private Instant deadline;
    private UUID winnerParticipantId;
    private String winnerTeamDefId;
    private RoundFinishReason finishReason;
    private final Map<String, Long> participantScores = new LinkedHashMap<>();
    private final Map<String, Long> teamScores = new LinkedHashMap<>();
    private final Map<String, Object> metadata = new LinkedHashMap<>();

    public SessionRound(UUID roundId, String eventId, UUID sessionId, int number) {
        this.roundId = roundId; this.eventId = eventId; this.sessionId = sessionId;
        this.number = number; this.state = RoundState.WAITING;
    }

    public UUID roundId() { return roundId; }
    public String eventId() { return eventId; }
    public UUID sessionId() { return sessionId; }
    public int number() { return number; }
    public RoundState state() { return state; }
    public Optional<Instant> startedAt() { return Optional.ofNullable(startedAt); }
    public Optional<Instant> finishedAt() { return Optional.ofNullable(finishedAt); }
    public Optional<Instant> deadline() { return Optional.ofNullable(deadline); }
    public Optional<UUID> winnerParticipantId() { return Optional.ofNullable(winnerParticipantId); }
    public Optional<String> winnerTeamDefId() { return Optional.ofNullable(winnerTeamDefId); }
    public Optional<RoundFinishReason> finishReason() { return Optional.ofNullable(finishReason); }
    public Map<String, Long> participantScores() { return Collections.unmodifiableMap(participantScores); }
    public Map<String, Long> teamScores() { return Collections.unmodifiableMap(teamScores); }
    public Map<String, Object> metadata() { return Collections.unmodifiableMap(metadata); }
    public Object metadata(String key) { return metadata.get(key); }

    public void state(RoundState s, Instant at) {
        this.state = s;
        if (s == RoundState.ACTIVE) this.startedAt = at;
        if (s == RoundState.FINISHED || s == RoundState.CANCELLED || s == RoundState.FAILED) this.finishedAt = at;
    }
    public void deadline(Instant d) { this.deadline = d; }
    public void winnerParticipantId(UUID id) { this.winnerParticipantId = id; }
    public void winnerTeamDefId(String id) { this.winnerTeamDefId = id; }
    public void finishReason(RoundFinishReason r) { this.finishReason = r; }

    public void setParticipantScore(String playerIdStr, long score) { participantScores.put(playerIdStr, score); }
    public long participantScore(String playerIdStr) { return participantScores.getOrDefault(playerIdStr, 0L); }
    public void addParticipantScore(String playerIdStr, long delta) {
        participantScores.merge(playerIdStr, delta, Long::sum);
    }

    public void setTeamScore(String teamDefId, long score) { teamScores.put(teamDefId, score); }
    public long teamScore(String teamDefId) { return teamScores.getOrDefault(teamDefId, 0L); }
    public void addTeamScore(String teamDefId, long delta) {
        teamScores.merge(teamDefId, delta, Long::sum);
    }
}
