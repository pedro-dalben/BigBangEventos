package com.pedrodalben.bigbangeventos.core;

import com.pedrodalben.bigbangeventos.api.OperationResult;
import com.pedrodalben.bigbangeventos.participant.EventParticipant;
import com.pedrodalben.bigbangeventos.participant.ParticipantState;
import com.pedrodalben.bigbangeventos.platform.AuditLogger;
import com.pedrodalben.bigbangeventos.session.EventSession;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public final class DisconnectService {
    private final Clock clock;
    private final Duration gracePeriod;
    private final Map<UUID, DisconnectRecord> disconnected = new HashMap<>();

    public DisconnectService(Clock clock, Duration gracePeriod) {
        this.clock = clock;
        this.gracePeriod = gracePeriod;
    }

    public synchronized OperationResult onDisconnect(UUID playerId, EventSession session) {
        EventParticipant p = session.participant(playerId).orElse(null);
        if (p == null) return OperationResult.ok("Não é participante");

        p.disconnected(clock.instant());
        disconnected.put(playerId, new DisconnectRecord(session.id(), clock.instant()));
        AuditLogger.playerDisconnected(playerId, session.eventId(), session.id());
        return OperationResult.ok("Desconexão registrada");
    }

    public synchronized Optional<ReconnectResult> onReconnect(UUID playerId, EventSession session) {
        DisconnectRecord record = disconnected.get(playerId);
        if (record == null) return Optional.empty();

        EventParticipant p = session.participant(playerId).orElse(null);
        if (p == null) {
            disconnected.remove(playerId);
            return Optional.empty();
        }

        boolean expired = clock.instant().isAfter(record.disconnectTime().plus(gracePeriod));
        if (expired) {
            p.state(ParticipantState.DISQUALIFIED);
            disconnected.remove(playerId);
            AuditLogger.gracePeriodExpired(playerId, session.id());
            return Optional.of(new ReconnectResult(false, "grace_period_expired"));
        }

        if (session.state() == com.pedrodalben.bigbangeventos.session.SessionState.RUNNING)
            p.state(ParticipantState.ACTIVE);
        else
            p.state(ParticipantState.REGISTERED);

        disconnected.remove(playerId);
        AuditLogger.playerReconnected(playerId, session.eventId());
        return Optional.of(new ReconnectResult(true, "reconnected"));
    }

    public synchronized boolean isInGracePeriod(UUID playerId) {
        DisconnectRecord record = disconnected.get(playerId);
        return record != null && !clock.instant().isAfter(record.disconnectTime().plus(gracePeriod));
    }

    public synchronized void expireGracePeriod(UUID playerId) {
        disconnected.remove(playerId);
    }

    public synchronized Collection<Map.Entry<UUID, DisconnectRecord>> pendingExpirations() {
        List<Map.Entry<UUID, DisconnectRecord>> result = new ArrayList<>();
        Instant now = clock.instant();
        for (var entry : disconnected.entrySet()) {
            if (now.isAfter(entry.getValue().disconnectTime().plus(gracePeriod)))
                result.add(entry);
        }
        return result;
    }

    public record DisconnectRecord(UUID sessionId, Instant disconnectTime) {}
    public record ReconnectResult(boolean success, String reason) {}
}
