package com.pedrodalben.bigbangeventos.core;

import com.pedrodalben.bigbangeventos.api.OperationResult;
import com.pedrodalben.bigbangeventos.definition.*;
import com.pedrodalben.bigbangeventos.eventtype.*;
import com.pedrodalben.bigbangeventos.lifecycle.SessionLifecycle;
import com.pedrodalben.bigbangeventos.participant.*;
import com.pedrodalben.bigbangeventos.persistence.EventStorage;
import com.pedrodalben.bigbangeventos.platform.*;
import com.pedrodalben.bigbangeventos.session.*;
import com.pedrodalben.bigbangeventos.snapshot.*;
import com.pedrodalben.bigbangeventos.trigger.*;
import com.pedrodalben.bigbangeventos.validation.*;
import java.time.*;
import java.util.*;
import com.pedrodalben.bigbangeventos.domain.*;
import com.pedrodalben.bigbangeventos.objective.*;
import com.pedrodalben.bigbangeventos.stage.*;
import com.pedrodalben.bigbangeventos.data.TypedDataService;
import com.pedrodalben.bigbangeventos.core.team.TeamService;
import com.pedrodalben.bigbangeventos.core.round.RoundService;
import com.pedrodalben.bigbangeventos.core.combat.CombatService;
import com.pedrodalben.bigbangeventos.core.combat.LifeService;
import com.pedrodalben.bigbangeventos.core.combat.EliminationService;
import com.pedrodalben.bigbangeventos.core.respawn.RespawnService;
import com.pedrodalben.bigbangeventos.core.spectator.SpectatorService;
import org.slf4j.LoggerFactory;

public final class EventEngine {
    private final EventStorage storage;
    private final Clock clock;
    private final EventTypeRegistry types = new EventTypeRegistry();
    private final EventValidator validator;
    private final SessionLifecycle lifecycle;
    private final ParticipationService participation;
    private final ParticipantCompletionService completion;
    private final TriggerService triggers;
    private final SnapshotService snapshots;
    private final PlayerRestoreService restore;
    private final DisconnectService disconnect;
    private final SessionRecoveryService recovery;
    private final PlatformTeleportService teleport;
    private final PlatformPlayerService players;
    private final Map<String, EventSession> active = new HashMap<>();
    private RegionTriggerService regionTriggers;
    private final ObjectiveTypeRegistry objectiveTypes = new ObjectiveTypeRegistry();
    private final DomainEventBus events;
    private final ObjectiveService objectives;
    private final StageService stages;
    private final TypedDataService data = new TypedDataService();
    private final TeamService teams;
    private final RoundService rounds;
    private final LifeService lifeService;
    private final EliminationService eliminationService;
    private final CombatService combat;
    private final RespawnService respawn;
    private final SpectatorService spectator;

    public EventEngine(EventStorage storage, Clock clock,
                       SnapshotGateway snapshotGateway,
                       PlatformTeleportService teleport, PlatformPlayerService players,
                       PlatformScheduler scheduler) {
        this.storage = storage;
        this.clock = clock;
        this.teleport = teleport;
        this.players = players;
        this.snapshots = new SnapshotService(snapshotGateway);
        this.restore = new PlayerRestoreService(snapshots, snapshotGateway, teleport);
        this.lifecycle = new SessionLifecycle(clock);
        this.disconnect = new DisconnectService(clock, Duration.ofSeconds(120));
        this.participation = new ParticipationService(clock, snapshots, teleport, players,
                restore, InventoryMode.CLEAR_AND_RESTORE);
        this.completion = new ParticipantCompletionService(clock);
        this.triggers = new TriggerService(clock, completion);
        this.recovery = new SessionRecoveryService(storage, snapshots, restore, players);
        this.regionTriggers = new RegionTriggerService(this, players, 2);
        this.events = new DomainEventBus(scheduler, LoggerFactory.getLogger("BigBangEventos.DomainEvents"));
        this.teams = new TeamService(clock, events);
        this.rounds = new RoundService(clock, events);
        this.lifeService = new LifeService(events);
        this.eliminationService = new EliminationService(clock, events);
        this.combat = new CombatService(clock, events, lifeService, eliminationService);
        this.respawn = new RespawnService(clock, events, teleport);
        this.spectator = new SpectatorService(events, teleport);
        this.validator = new EventValidator(types, objectiveTypes);
        this.objectives = new ObjectiveService(clock, objectiveTypes, events);
        this.objectives.teams(teams);
        this.stages = new StageService(clock, objectives, events);
        this.objectives.stages(stages);
        this.triggers.services(objectives, stages);
        types.register(new GenericEventType());
    }

    public EventTypeRegistry types() { return types; }
    public EventValidator validator() { return validator; }
    public ParticipationService participation() { return participation; }
    public ParticipantCompletionService completion() { return completion; }
    public SnapshotService snapshots() { return snapshots; }
    public PlayerRestoreService restore() { return restore; }
    public DisconnectService disconnect() { return disconnect; }
    public SessionRecoveryService recovery() { return recovery; }
    public PlatformTeleportService teleport() { return teleport; }
    public PlatformPlayerService players() { return players; }
    public RegionTriggerService regionTriggers() { return regionTriggers; }
    public ObjectiveTypeRegistry objectiveTypes() { return objectiveTypes; }
    public ObjectiveService objectives() { return objectives; }
    public StageService stages() { return stages; }
    public DomainEventBus events() { return events; }
    public TypedDataService data() { return data; }
    public TeamService teams() { return teams; }
    public RoundService rounds() { return rounds; }
    public LifeService lifeService() { return lifeService; }
    public EliminationService eliminationService() { return eliminationService; }
    public CombatService combat() { return combat; }
    public RespawnService respawn() { return respawn; }
    public SpectatorService spectator() { return spectator; }

    public synchronized void onTick() { regionTriggers.onTick(); stages.onTick(active.values().stream().map(s -> new StageService.SessionContext(definition(s.eventId()).orElse(null), s)).filter(c -> c.definition()!=null).toList()); }

    public synchronized void recoverOnStartup() { recovery.recoverOnStartup(); }

    public synchronized void save(EventDefinition definition) { storage.saveDefinition(definition); }
    public synchronized OperationResult create(String id, String type, String server) {
        if (storage.findDefinition(id).isPresent()) return OperationResult.fail("duplicate_event", "Evento já existe");
        if (types.find(type).isEmpty()) return OperationResult.fail("unknown_type", "Tipo não encontrado");
        try { EventDefinition d = new EventDefinition(id, type, server); storage.saveDefinition(d); return OperationResult.ok("Evento criado"); }
        catch (IllegalArgumentException e) { return OperationResult.fail("invalid_id", e.getMessage()); }
    }

    public Optional<EventDefinition> definition(String id) { return storage.findDefinition(id); }
    public Collection<EventDefinition> definitions() { return storage.findDefinitions(); }
    public Optional<EventSession> activeSession(String eventId) { return Optional.ofNullable(active.get(eventId)); }
    public Optional<EventSession> sessionByPlayer(UUID player) {
        return participation.sessionFor(player).flatMap(id -> active.values().stream().filter(s -> s.id().equals(id)).findFirst());
    }

    public synchronized OperationResult delete(String id) {
        if (active.containsKey(id)) return OperationResult.fail("session_active", "Não exclua evento com sessão ativa");
        if (storage.findDefinition(id).isEmpty()) return OperationResult.fail("not_found", "Evento não encontrado");
        storage.deleteDefinition(id); return OperationResult.ok("Evento excluído");
    }

    public synchronized OperationResult open(String id, UUID actor) {
        EventDefinition d = storage.findDefinition(id).orElse(null);
        if (d == null) return OperationResult.fail("not_found", "Evento não encontrado");
        if (active.containsKey(id)) return OperationResult.fail("session_active", "Já existe sessão ativa");
        ValidationResult v = validator.validate(d);
        if (!v.valid()) return OperationResult.fail("invalid_configuration", "Configuração possui erros críticos");
        EventSession s = new EventSession(UUID.randomUUID(), id, d.configurationVersion(), clock.instant(), actor);
        types.find(d.type()).ifPresent(t -> t.onSessionCreated(s));
        OperationResult r = lifecycle.transition(s, SessionState.REGISTRATION_OPEN);
        if (r.success()) { active.put(id, s); storage.saveSession(s); types.find(d.type()).ifPresent(t -> t.onRegistrationOpen(s)); }
        return r;
    }

    public synchronized OperationResult close(String id) { return transition(id, SessionState.REGISTRATION_CLOSED); }
    public synchronized OperationResult start(String id) {
        EventSession s = active.get(id);
        if (s == null) return OperationResult.fail("no_session", "Nenhuma sessão ativa");
        if (s.state() == SessionState.REGISTRATION_OPEN || s.state() == SessionState.REGISTRATION_CLOSED) {
            OperationResult countdown = lifecycle.transition(s, SessionState.COUNTDOWN);
            if (!countdown.success()) return countdown;
        }
        OperationResult started = transition(id, SessionState.RUNNING);
        if (started.success()) {
            s.participants().stream().filter(p -> p.state() == ParticipantState.REGISTERED || p.state() == ParticipantState.WAITING)
                .forEach(p -> p.start(clock.instant()));
            EventDefinition d = definition(id).orElse(null);
            if (d != null) {
                d.location(LocationName.ENTRANCE).ifPresent(entrance -> {
                    for (EventParticipant p : s.participants()) {
                        if (p.state() == ParticipantState.ACTIVE) {
                            StoredLocation dest = new StoredLocation(entrance.serverId(), entrance.dimension(),
                                    entrance.x(), entrance.y(), entrance.z(), entrance.yaw(), entrance.pitch());
                            teleport.teleport(p.playerId(), dest);
                        }
                    }
                });
            }
            types.find(d.type()).ifPresent(t -> t.onSessionStart(s));
        }
        return started;
    }

    public synchronized OperationResult pause(String id) { return transition(id, SessionState.PAUSED); }
    public synchronized OperationResult resume(String id) { return transition(id, SessionState.RUNNING); }

    public synchronized OperationResult finish(String id) {
        EventSession s = active.get(id);
        if (s == null) return OperationResult.fail("no_session", "Nenhuma sessão ativa");
        OperationResult r = lifecycle.transition(s, SessionState.FINISHING);
        if (!r.success()) return r;
        r = lifecycle.transition(s, SessionState.FINISHED);
        if (r.success()) {
            regionTriggers.cleanupSession(id);
            restoreAllParticipants(s);
            storage.saveSession(s);
            active.remove(id);
            definition(id).flatMap(d -> types.find(d.type())).ifPresent(t -> t.onSessionFinish(s));
        }
        return r;
    }

    public synchronized OperationResult cancel(String id, String reason) {
        EventSession s = active.get(id);
        if (s == null) return OperationResult.fail("no_session", "Nenhuma sessão ativa");
        OperationResult r = lifecycle.cancel(s, reason);
        if (r.success()) {
            regionTriggers.cleanupSession(id);
            restoreAllParticipants(s);
            storage.saveSession(s);
            active.remove(id);
            AuditLogger.eventCancelled(id, reason, null);
            definition(id).flatMap(d -> types.find(d.type())).ifPresent(t -> t.onSessionCancel(s, reason));
        }
        return r;
    }

    public synchronized OperationResult join(String id, UUID player, String name, boolean forced, boolean allowed) {
        EventDefinition d = definition(id).orElse(null);
        EventSession s = active.get(id);
        if (d == null || s == null) return OperationResult.fail("not_open", "Evento não está aberto");
        OperationResult r = participation.join(d, s, player, name, forced, allowed);
        if (r.success()) { storage.saveSession(s); events.publish(new ObjectiveEvents.ParticipantJoined(id,s.id(),player)); }
        return r;
    }

    public synchronized OperationResult leave(UUID player, String reason) {
        EventSession s = sessionByPlayer(player).orElse(null);
        if (s == null) return OperationResult.ok("Jogador já não participa");
        OperationResult r = participation.leave(s, player, reason);
        if (r.success()) {
            regionTriggers.cleanupPlayer(player);
            storage.saveSession(s);
            events.publish(new ObjectiveEvents.ParticipantLeft(s.eventId(),s.id(),player));
        }
        return r;
    }

    public synchronized OperationResult complete(String eventId, UUID player, CompletionMode mode) {
        EventSession s = active.get(eventId);
        if (s == null) return OperationResult.fail("no_session", "Nenhuma sessão ativa");
        OperationResult r = completion.complete(s, player, mode);
        if (r.success()) { storage.saveSession(s); events.publish(new ObjectiveEvents.ParticipantCompleted(eventId,s.id(),player)); }
        return r;
    }

    public synchronized OperationResult activateTrigger(String eventId, String triggerId, UUID player,
                                                         String name, PermissionChecker permissions, TriggerEffects effects) {
        EventDefinition d = definition(eventId).orElse(null);
        EventSession s = active.get(eventId);
        if (d == null || s == null) return OperationResult.fail("no_session", "Nenhuma sessão ativa");
        var t = d.trigger(triggerId).orElse(null);
        if (t == null) return OperationResult.fail("trigger_not_found", "Gatilho não encontrado");
        OperationResult r = triggers.execute(t, new TriggerExecutionContext(s, player, name, permissions, effects, d));
        if (r.success()) {
            storage.saveSession(s);
            var triggerRef = t;
            types.find(d.type()).ifPresent(et -> et.onTriggerFired(s, triggerRef, player));
        }
        return r;
    }

    public synchronized OperationResult onPlayerDisconnect(UUID playerId) {
        EventSession s = sessionByPlayer(playerId).orElse(null);
        if (s == null) return OperationResult.ok("Não está em evento");
        OperationResult r = disconnect.onDisconnect(playerId, s);
        return r;
    }

    public synchronized Optional<DisconnectService.ReconnectResult> onPlayerReconnect(UUID playerId) {
        EventSession s = sessionByPlayer(playerId).orElse(null);
        if (s == null) {
            restore.restorePending(playerId);
            return Optional.empty();
        }
        return disconnect.onReconnect(playerId, s);
    }

    public synchronized OperationResult onServerShutdown() {
        for (EventSession s : active.values()) {
            storage.saveSession(s);
        }
        return OperationResult.ok("Sessões persistidas");
    }

    private void restoreAllParticipants(EventSession s) {
        EventDefinition d = definition(s.eventId()).orElse(null);
        StoredLocation exit = null;
        if (d != null) {
            exit = d.location(LocationName.EXIT)
                    .map(e -> new StoredLocation(e.serverId(), e.dimension(),
                            e.x(), e.y(), e.z(), e.yaw(), e.pitch()))
                    .orElse(null);
        }
        for (EventParticipant p : s.participants()) {
            if (p.state() != ParticipantState.LEFT && p.state() != ParticipantState.RESTORED) {
                restore.restore(p.playerId(), exit);
            }
        }
    }

    private OperationResult transition(String id, SessionState target) {
        EventSession s = active.get(id);
        if (s == null) return OperationResult.fail("no_session", "Nenhuma sessão ativa");
        OperationResult r = lifecycle.transition(s, target);
        if (r.success()) { storage.saveSession(s); }
        return r;
    }
}
