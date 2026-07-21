# AI Context — BigBangEventos Module SDK

BigBangEventos = server-side Fabric mod (1.21.1) for Minecraft events.
Modules = external Fabric mods that register custom EventTypes using
public APIs only.

## Module structure per Fabric:

```
meumod/
  build.gradle
  src/main/java/com/meumod/
    MeuModInitializer.java     (ModInitializer)
    MeuEventModule.java        (BigBangEventModule)
    MeuTipoEvento.java         (EventType)
  src/main/resources/
    fabric.mod.json
  src/test/java/com/meumod/
    MeuModTest.java
```

## Public APIs allowed (use these packages):

- `com.pedrodalben.bigbangeventos.api` — BigBangEventosApi,
  BigBangEventModule, EventModuleContext, OperationResult
- `com.pedrodalben.bigbangeventos.eventtype` — EventType, EventTypeRegistry,
  GenericEventType
- `com.pedrodalben.bigbangeventos.definition` — EventDefinition,
  EventLocation, EventArea, LocationName
- `com.pedrodalben.bigbangeventos.session` — EventSession, SessionState
- `com.pedrodalben.bigbangeventos.participant` — EventParticipant,
  ParticipantState, CompletionMode
- `com.pedrodalben.bigbangeventos.trigger` — EventTrigger, TriggerType,
  ConditionType, ActionType, TriggerAction, TriggerExecutionContext,
  PermissionChecker, TriggerEffects
- `com.pedrodalben.bigbangeventos.ranking` — RankingStrategy, Rankings
- `com.pedrodalben.bigbangeventos.validation` — ValidationResult,
  ValidationLevel, ValidationIssue
- `com.pedrodalben.bigbangeventos.platform` — PlatformScheduler,
  PlatformPlayerService, PlatformTeleportService, StoredLocation
- `com.pedrodalben.bigbangeventos.snapshot` — SnapshotService,
  PlayerSnapshot, InventoryMode, SnapshotState

## Internal packages FORBIDDEN:

- `com.pedrodalben.bigbangeventos.core` (EventEngine etc.)
- `com.pedrodalben.bigbangeventos.fabric` (Fabric implementations)
- `com.pedrodalben.bigbangeventos.persistence` (EventStorage)
- `com.pedrodalben.bigbangeventos.lifecycle` (SessionLifecycle)
- `com.pedrodalben.bigbangeventos.timer` (SessionTimer)
- `com.pedrodalben.bigbangeventos.BigBangEventos` (static engine()
  access — use BigBangEventosApi instead)

## Lifecycle:

1. onLoad(EventModuleContext) — read config, init structures
2. onEnable(EventModuleContext) — register EventType, commands, listeners
3. onDisable(EventModuleContext) — save all, cleanup

BigBangEventModule interface:
```java
String moduleId();
int apiVersion();
void onLoad(EventModuleContext ctx);
void onEnable(EventModuleContext ctx);
void onDisable(EventModuleContext ctx);
```

EventModuleContext provides:
```java
int apiVersion();
EventTypeRegistry typeRegistry();
PlatformScheduler scheduler();
BigBangEventosApi api();
Path dataDirectory();
<T> T loadConfig(String filename, Class<T> type);
Path resolveConfig(String filename);
```

## EventType interface:

```java
String id();
String displayName();
default ValidationResult validate(EventDefinition d) { return ValidationResult.empty(); }
default void onSessionCreated(EventSession s) {}
default void onRegistrationOpen(EventSession s) {}
default void onSessionStart(EventSession s) {}
default void onSessionFinish(EventSession s) {}
default void onSessionCancel(EventSession s, String reason) {}
```

Register: `ctx.typeRegistry().register(new MeuTipo());`

## Threading:

- Server thread ONLY for Minecraft API access (ServerPlayer, ServerLevel, etc.)
- Use `PlatformScheduler.executeOnServerThread(Runnable)` to cross threads
- Use `scheduler.schedule(delay, task)` and `scheduleRepeating(interval, task)`
- `isServerThread()` to check
- Heavy IO/compute: run on separate thread, then call back to server thread

## Persistence:

- Core storage (EventStorage) for definitions/sessions — DO NOT USE DIRECTLY
- Module-owned YAML files at `ctx.dataDirectory()` — use SnakeYAML
- Path: `config/bigbangeventos/modules/{moduleId}/`
- Atomic writes: write .tmp, rename
- Save on onDisable + periodically

## Register EventType:

```java
ctx.typeRegistry().register(new ParkourEventType());
```

## Register Commands:

```java
CommandRegistrationCallback.EVENT.register((dispatcher, ra, env) -> {
    dispatcher.register(Commands.literal("parkour")
        .executes(ctx -> { /* ... */ return 1; }));
});
```

## Create Triggers:

```java
EventTrigger t = new EventTrigger("id", TriggerType.SIGN_INTERACT);
t.addCondition(ConditionType.EVENT_IS_RUNNING);
t.addAction(new TriggerAction(ActionType.ADD_POINTS, Map.of("amount", "100")));
t.maxUses(1);
definition.putTrigger(t);
```

## Store Participant Data:

- Simple: `participant.data("key", "value")` and `participant.data().get("key")`
- Complex: module-owned storage keyed by `playerId + ":" + sessionId`

## Complete Player:

```java
engine.complete(eventId, playerId, CompletionMode.MANUAL_FINISH);
```

Or from trigger: add `TriggerAction(ActionType.PLAYER_COMPLETE, Map.of())`.

## Ranking:

```java
Rankings.COMPLETION_ORDER.rank(session);     // by finish order
Rankings.TIME_ASCENDING.rank(session);        // by finish time
Rankings.SCORE_DESCENDING.rank(session);      // by score desc
```

Custom ranking: implement `RankingStrategy`:
```java
List<EventParticipant> rank(EventSession session);
```

## Tests:

- Copy stubs from EventCoreTest: MemoryStorage, StubSnapshotGateway,
  StubTeleportService, StubPlayerService, StubScheduler, MutableClock
- Create EventEngine with stubs: `new EventEngine(memoryStorage, clock, ...)`
- Test registration, validation, lifecycle, participation in isolation
- No Minecraft dependency in unit tests

## Rules Checklist:

1. Fabric server-side mod only (no client code)
2. fabric.mod.json: depends on bigbangeventos >=0.1.0
3. Entrypoint: `bigbangeventos:event_module` -> BigBangEventModule impl
4. Only use packages listed as public above
5. Do NOT use internal core classes
6. Register EventType in onEnable (not onLoad)
7. Use typeSettings() for type-specific configuration
8. Validate config in EventType.validate()
9. Use PlatformScheduler for threading
10. Own persistence for complex data, not core storage
11. Test with stubs, not with real Minecraft server
12. Handle exceptions in lifecycle hooks

## CRITICAL

Nao altere o BigBangEventos Core para implementar regras especificas do
evento. Todo codigo de regra de evento deve ficar no modulo, nunca no
core. (Translation: Do NOT modify BigBangEventos Core to implement
event-specific rules. All event rule code must stay in the module,
never in the core.)
