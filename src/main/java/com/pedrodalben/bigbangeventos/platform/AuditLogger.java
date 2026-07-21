package com.pedrodalben.bigbangeventos.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public final class AuditLogger {
    private static final Logger LOG = LoggerFactory.getLogger("BigBangEventos.Audit");

    private AuditLogger() {}

    public static void playerJoined(UUID playerId, String eventId, UUID sessionId) {
        LOG.info("AUDIT player_joined player={} event={} session={}", playerId, eventId, sessionId);
    }

    public static void playerLeft(UUID playerId, String eventId, String reason) {
        LOG.info("AUDIT player_left player={} event={} reason={}", playerId, eventId, reason);
    }

    public static void playerKicked(UUID actorId, UUID playerId, String eventId) {
        LOG.info("AUDIT player_kicked actor={} player={} event={}", actorId, playerId, eventId);
    }

    public static void playerDisconnected(UUID playerId, String eventId, UUID sessionId) {
        LOG.info("AUDIT player_disconnected player={} event={} session={}", playerId, eventId, sessionId);
    }

    public static void playerReconnected(UUID playerId, String eventId) {
        LOG.info("AUDIT player_reconnected player={} event={}", playerId, eventId);
    }

    public static void gracePeriodExpired(UUID playerId, UUID sessionId) {
        LOG.info("AUDIT grace_period_expired player={} session={}", playerId, sessionId);
    }

    public static void snapshotCreated(UUID playerId, UUID snapshotId, UUID sessionId) {
        LOG.info("AUDIT snapshot_created player={} snapshot={} session={}", playerId, snapshotId, sessionId);
    }

    public static void snapshotRestored(UUID playerId, UUID snapshotId) {
        LOG.info("AUDIT snapshot_restored player={} snapshot={}", playerId, snapshotId);
    }

    public static void restoreFailed(UUID playerId, UUID snapshotId) {
        LOG.info("AUDIT restore_failed player={} snapshot={}", playerId, snapshotId);
    }

    public static void sessionRecovered(UUID sessionId, String eventId) {
        LOG.info("AUDIT session_recovered session={} event={}", sessionId, eventId);
    }

    public static void eventCancelled(String eventId, String reason, UUID actor) {
        LOG.info("AUDIT event_cancelled event={} reason={} actor={}", eventId, reason, actor);
    }
}
