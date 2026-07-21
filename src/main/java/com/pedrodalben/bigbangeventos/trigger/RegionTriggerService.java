package com.pedrodalben.bigbangeventos.trigger;

import com.pedrodalben.bigbangeventos.core.EventEngine;
import com.pedrodalben.bigbangeventos.definition.EventArea;
import com.pedrodalben.bigbangeventos.definition.EventDefinition;
import com.pedrodalben.bigbangeventos.participant.EventParticipant;
import com.pedrodalben.bigbangeventos.participant.ParticipantState;
import com.pedrodalben.bigbangeventos.platform.PlatformPlayerService;
import com.pedrodalben.bigbangeventos.platform.StoredLocation;
import com.pedrodalben.bigbangeventos.session.EventSession;
import com.pedrodalben.bigbangeventos.session.SessionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class RegionTriggerService {
    private static final Logger LOG = LoggerFactory.getLogger(RegionTriggerService.class);

    private final EventEngine engine;
    private final PlatformPlayerService players;
    private final Map<String, Set<UUID>> insidePlayers = new HashMap<>();
    private int tickCounter;
    private final int checkIntervalTicks;

    public RegionTriggerService(EventEngine engine, PlatformPlayerService players, int checkIntervalTicks) {
        this.engine = engine;
        this.players = players;
        this.checkIntervalTicks = Math.max(1, checkIntervalTicks);
    }

    public void onTick() {
        tickCounter++;
        if (tickCounter % checkIntervalTicks != 0) return;

        Set<String> activeKeys = new HashSet<>();

        for (EventDefinition def : engine.definitions()) {
            EventSession session = engine.activeSession(def.id()).orElse(null);
            if (session == null || session.state() != SessionState.RUNNING) continue;

            for (EventParticipant participant : session.participants()) {
                if (participant.state() != ParticipantState.ACTIVE) continue;
                UUID playerId = participant.playerId();
                if (!players.isOnline(playerId)) continue;

                StoredLocation loc = players.captureLocation(playerId).orElse(null);
                if (loc == null) continue;

                for (EventTrigger trigger : def.triggers()) {
                    if (trigger.type() != TriggerType.REGION_ENTER
                            && trigger.type() != TriggerType.REGION_EXIT) continue;
                    if (!trigger.enabled()) continue;
                    EventArea area = trigger.area().orElse(null);
                    if (area == null) continue;

                    String key = def.id() + ":" + trigger.id();
                    activeKeys.add(key);

                    boolean inside = area.contains(loc.serverId(), loc.dimension(),
                            loc.x(), loc.y(), loc.z());
                    boolean wasInside = insidePlayers.getOrDefault(key, Set.of()).contains(playerId);

                    if (trigger.type() == TriggerType.REGION_ENTER && inside && !wasInside) {
                        try {
                            engine.activateTrigger(def.id(), trigger.id(), playerId,
                                    participant.knownName(),
                                    (id, perm) -> true,
                                    new TriggerEffects() {
                                        @Override
                                        public void message(UUID player, String message) {
                                            players.sendMessage(player, message);
                                        }
                                    });
                        } catch (Exception e) {
                            LOG.warn("Falha ao ativar trigger {} para player {}: {}",
                                    trigger.id(), playerId, e.getMessage());
                        }
                    }

                    Set<UUID> set = insidePlayers.computeIfAbsent(key, k -> new HashSet<>());
                    if (inside) {
                        set.add(playerId);
                    } else {
                        set.remove(playerId);
                    }
                }
            }
        }

        insidePlayers.keySet().removeIf(k -> !activeKeys.contains(k));
    }

    public synchronized void cleanupSession(String eventId) {
        insidePlayers.keySet().removeIf(k -> k.startsWith(eventId + ":"));
    }

    public synchronized void cleanupPlayer(UUID playerId) {
        for (Set<UUID> set : insidePlayers.values()) {
            set.remove(playerId);
        }
    }

    public synchronized void cleanupTrigger(String eventId, String triggerId) {
        insidePlayers.remove(eventId + ":" + triggerId);
    }
}
