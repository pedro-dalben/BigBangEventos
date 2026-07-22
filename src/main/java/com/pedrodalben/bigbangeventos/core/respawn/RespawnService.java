package com.pedrodalben.bigbangeventos.core.respawn;

import com.pedrodalben.bigbangeventos.api.OperationResult;
import com.pedrodalben.bigbangeventos.domain.CombatEvents;
import com.pedrodalben.bigbangeventos.domain.DomainEventBus;
import com.pedrodalben.bigbangeventos.model.respawn.RespawnPolicy;
import com.pedrodalben.bigbangeventos.model.respawn.RespawnRequest;
import com.pedrodalben.bigbangeventos.participant.combat.ParticipantCombatState;
import com.pedrodalben.bigbangeventos.platform.PlatformTeleportService;
import com.pedrodalben.bigbangeventos.platform.StoredLocation;
import com.pedrodalben.bigbangeventos.session.EventSession;
import com.pedrodalben.bigbangeventos.session.round.RoundState;
import com.pedrodalben.bigbangeventos.session.round.SessionRound;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class RespawnService {
    private final Clock clock;
    private final DomainEventBus events;
    private final PlatformTeleportService teleport;
    private final Map<UUID, Set<UUID>> pending = new ConcurrentHashMap<>();

    public RespawnService(Clock clock, DomainEventBus events, PlatformTeleportService teleport) {
        this.clock = clock; this.events = events; this.teleport = teleport;
    }

    public OperationResult scheduleRespawn(EventSession session, ParticipantCombatState state, String eventId,
                                            RespawnPolicy policy, long delaySeconds, int invulnerabilitySeconds,
                                            StoredLocation spawnLocation) {
        if (state.eliminated()) return OperationResult.fail("eliminated", "Jogador eliminado não pode respawnar");
        if (state.respawnPending()) return OperationResult.ok("Respawn já pendente");
        if (policy == RespawnPolicy.NONE) return OperationResult.fail("respawn_disabled", "Respawn desabilitado");

        state.respawnPending(true);

        if (policy == RespawnPolicy.IMMEDIATE || (policy == RespawnPolicy.DELAYED && delaySeconds <= 0)) {
            return executeRespawn(session, state, eventId, spawnLocation, invulnerabilitySeconds, null);
        }

        if (policy == RespawnPolicy.DELAYED) {
            RespawnRequest req = new RespawnRequest(state.participantId(), eventId, session.id(), clock.instant(), delaySeconds);
            pending.computeIfAbsent(session.id(), k -> ConcurrentHashMap.newKeySet()).add(state.participantId());
            events.publish(new CombatEvents.ParticipantRespawnScheduled(eventId, session.id(), state.participantId(), delaySeconds));
            return OperationResult.ok("Respawn agendado em " + delaySeconds + "s");
        }

        if (policy == RespawnPolicy.AT_TEAM_SPAWN || policy == RespawnPolicy.AT_PERSONAL_SPAWN) {
            return executeRespawn(session, state, eventId, spawnLocation, invulnerabilitySeconds, null);
        }

        return OperationResult.fail("unsupported_policy", "Política não implementada");
    }

    public OperationResult executeRespawn(EventSession session, ParticipantCombatState state, String eventId,
                                            StoredLocation spawnLocation, int invulnerabilitySeconds, SessionRound round) {
        if (spawnLocation == null) return OperationResult.fail("no_spawn", "Nenhum spawn disponível");
        if (session.state() != com.pedrodalben.bigbangeventos.session.SessionState.RUNNING
                && session.state() != com.pedrodalben.bigbangeventos.session.SessionState.COUNTDOWN)
            return OperationResult.fail("session_not_active", "Sessão não está ativa");

        teleport.teleport(state.participantId(), spawnLocation);
        state.respawnPending(false);
        if (invulnerabilitySeconds > 0) {
            state.invulnerableUntil(clock.instant().plusSeconds(invulnerabilitySeconds));
        }
        events.publish(new CombatEvents.ParticipantRespawned(eventId, session.id(), state.participantId()));
        pending.getOrDefault(session.id(), Collections.emptySet()).remove(state.participantId());
        return OperationResult.ok("Respawn executado");
    }

    public void onTick(EventSession session, Map<UUID, ParticipantCombatState> combatStates,
                        String eventId, StoredLocation spawnLocation, int invulnerabilitySeconds) {
        Set<UUID> sessionPending = pending.get(session.id());
        if (sessionPending == null || sessionPending.isEmpty()) return;
        if (session.state() != com.pedrodalben.bigbangeventos.session.SessionState.RUNNING) return;

        for (UUID playerId : List.copyOf(sessionPending)) {
            ParticipantCombatState state = combatStates.get(playerId);
            if (state == null || state.eliminated()) {
                sessionPending.remove(playerId);
                continue;
            }
            if (state.respawnPending()) {
                executeRespawn(session, state, eventId, spawnLocation, invulnerabilitySeconds, null);
            }
        }
    }

    public boolean hasPendingRespawn(UUID sessionId, UUID playerId) {
        Set<UUID> set = pending.get(sessionId);
        return set != null && set.contains(playerId);
    }

    public void cancelPending(UUID sessionId, UUID playerId) {
        Set<UUID> set = pending.get(sessionId);
        if (set != null) set.remove(playerId);
    }

    public void cleanupSession(UUID sessionId) { pending.remove(sessionId); }

    public void cleanupPlayer(UUID playerId) {
        pending.values().forEach(s -> s.remove(playerId));
    }
}
