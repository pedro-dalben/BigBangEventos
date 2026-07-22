package com.pedrodalben.bigbangeventos.participant.combat;

import java.time.Instant;
import java.util.UUID;

public final class ParticipantCombatState {
    private final UUID participantId;
    private final UUID sessionId;
    private int livesRemaining;
    private int roundKills, roundDeaths;
    private int sessionKills, sessionDeaths;
    private boolean eliminated;
    private Instant eliminatedAt;
    private EliminationReason eliminationReason;
    private UUID lastKillerId;
    private Instant lastDamageAt;
    private boolean respawnPending;
    private Instant invulnerableUntil;

    public ParticipantCombatState(UUID participantId, UUID sessionId, int initialLives) {
        this.participantId = participantId; this.sessionId = sessionId;
        this.livesRemaining = initialLives;
    }

    public UUID participantId() { return participantId; }
    public UUID sessionId() { return sessionId; }
    public int livesRemaining() { return livesRemaining; }
    public int roundKills() { return roundKills; }
    public int roundDeaths() { return roundDeaths; }
    public int sessionKills() { return sessionKills; }
    public int sessionDeaths() { return sessionDeaths; }
    public boolean eliminated() { return eliminated; }
    public Instant eliminatedAt() { return eliminatedAt; }
    public EliminationReason eliminationReason() { return eliminationReason; }
    public UUID lastKillerId() { return lastKillerId; }
    public Instant lastDamageAt() { return lastDamageAt; }
    public boolean respawnPending() { return respawnPending; }
    public Instant invulnerableUntil() { return invulnerableUntil; }

    public void livesRemaining(int v) { this.livesRemaining = Math.max(0, v); }
    public void removeLife() { if (livesRemaining > 0) livesRemaining--; }
    public void addLife() { livesRemaining++; }

    public void kill() { roundKills++; sessionKills++; }
    public void death() { roundDeaths++; sessionDeaths++; }
    public void death(UUID killerId) { roundDeaths++; sessionDeaths++; lastKillerId = killerId; lastDamageAt = Instant.now(); }

    public void eliminated(boolean v, EliminationReason reason, Instant at) {
        this.eliminated = v; this.eliminationReason = reason; this.eliminatedAt = at;
    }

    public void respawnPending(boolean v) { this.respawnPending = v; }
    public void invulnerableUntil(Instant until) { this.invulnerableUntil = until; }
    public void lastDamageAt(Instant at) { this.lastDamageAt = at; }
    public void lastKillerId(UUID id) { this.lastKillerId = id; }

    public void resetRoundStats() { roundKills = 0; roundDeaths = 0; }
    public boolean isInvulnerable(Instant now) { return invulnerableUntil != null && now.isBefore(invulnerableUntil); }
}
