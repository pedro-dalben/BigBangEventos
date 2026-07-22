package com.pedrodalben.bigbangeventos.session.team;

import com.pedrodalben.bigbangeventos.data.InMemoryDataContainer;
import java.time.Instant;
import java.util.*;

public final class SessionTeam {
    private final UUID teamId;
    private final String eventId;
    private final UUID sessionId;
    private final String teamDefinitionId;
    private final List<UUID> members = new ArrayList<>();
    private final InMemoryDataContainer data = new InMemoryDataContainer();
    private long score;
    private TeamStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public SessionTeam(UUID teamId, String eventId, UUID sessionId, String teamDefinitionId, Instant createdAt) {
        this.teamId = teamId; this.eventId = eventId; this.sessionId = sessionId;
        this.teamDefinitionId = teamDefinitionId; this.createdAt = createdAt;
        this.updatedAt = createdAt; this.status = TeamStatus.ACTIVE;
    }

    public UUID teamId() { return teamId; }
    public String eventId() { return eventId; }
    public UUID sessionId() { return sessionId; }
    public String teamDefinitionId() { return teamDefinitionId; }
    public List<UUID> members() { return Collections.unmodifiableList(members); }
    public long score() { return score; }
    public TeamStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public InMemoryDataContainer data() { return data; }

    public void addMember(UUID player) { if (!members.contains(player)) members.add(player); updatedAt = Instant.now(); }
    public void removeMember(UUID player) { members.remove(player); updatedAt = Instant.now(); }
    public boolean hasMember(UUID player) { return members.contains(player); }
    public int memberCount() { return members.size(); }

    public void addScore(long delta) { this.score += delta; updatedAt = Instant.now(); }
    public void setScore(long score) { this.score = score; updatedAt = Instant.now(); }
    public void status(TeamStatus s) { this.status = s; updatedAt = Instant.now(); }
}
