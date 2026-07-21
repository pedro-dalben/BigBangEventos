# AI Context — Módulo Parkour

Modulo Fabric (1.21.1) que registra tipo `parkour` no BigBangEventos.
Evento de percurso com checkpoints, cronometro individual, quedas,
ranking por tempo.

## Estrutura

```
modules/parkour/
  src/main/java/com/pedrodalben/bigbangeventos/modules/parkour/
    ParkourModule.java         (BigBangEventModule)
    ParkourEventType.java      (EventType)
    ParkourDataStore.java      (persistencia YAML)
    ParkourSessionService.java (sessao parkour)
    ParkourCheckpointService.java (ordem checkpoints)
    ParkourFallService.java    (deteccao queda)
    ParkourCommand.java        (comandos /parkour)
  src/test/java/...parkour/
    ParkourModuleTest.java
```

## BigBangEventModule

```java
public class ParkourModule implements BigBangEventModule {
    public String moduleId() { return "parkour_module"; }
    public int apiVersion() { return 1; }
    public void onLoad(EventModuleContext ctx) { /* init */ }
    public void onEnable(EventModuleContext ctx) {
        ctx.typeRegistry().register(new ParkourEventType());
        CommandRegistrationCallback.EVENT.register(...);
    }
    public void onDisable(EventModuleContext ctx) { /* saveAll */ }
}
```

## EventType

```java
public class ParkourEventType implements EventType {
    public String id() { return "parkour"; }
    public String displayName() { return "Parkour"; }
    public ValidationResult validate(EventDefinition d) { ... }
    public void onSessionStart(EventSession s) { /* inicia timers */ }
    public void onSessionFinish(EventSession s) { /* ranking */ }
}
```

## Config (typeSettings)

```yaml
parkour:
  checkpoint-order: strict|any
  fall-distance-threshold: 10
  count-falls: true
  lives: 0
  max-time-seconds: 0
  allow-flight: false
```

## Checkpoints

Gatilhos SIGN_INTERACT vinculados a placas. Prefixo `checkpoint_1`,
`checkpoint_2`, ..., `finish`. Ordem strict = sequencial, any = livre.

Criar:
```bash
/evento trigger create checkpoint_1 sign_interact
/evento trigger bind checkpoint_1
```

## Quedas

`ParkourFallService` escuta tick do servidor. Se jogador cai > threshold
(10 blocos padrao), teleporta ao ultimo checkpoint, incrementa falls.

## Dados jogador (ParkourPlayerData)

```java
class ParkourPlayerData {
    UUID playerId, sessionId;
    long startedAt, finishedAt;
    int lastCheckpoint, falls;
    List<String> completedCheckpoints;
    long elapsedMillis() { return finishedAt - startedAt; }
}
```

Persistido em: `config/bigbangeventos/modules/parkour/players/{pid}_{sid}.yml`

## Servicos chave

- `ParkourSessionService` — gerencia estado da sessao parkour
- `ParkourCheckpointService` — valida ordem (strict/any), extrai numero
- `ParkourFallService` — detecta queda no tick, teleporta de volta
- `ParkourDataStore` — save/load YAML, cache ConcurrentHashMap

## Ranking

`Rankings.TIME_ASCENDING.rank(session)` no finish.

## Comandos

```
/parkour                  — status
/parkour leave            — sair
/parkour top              — top 10
/parkour checkpoints      — listar
/parkour admin reset <p>  — resetar jogador (op)
/parkour admin tp <p> <n> — teleportar checkpoint (op)
/parkour admin info       — detalhes sessao (op)
```

## Regras

1. Nao altere BigBangEventos Core
2. Regras especificas do parkour ficam no modulo, nunca no core
3. Use só APIs publicas (EventType, EventTrigger, Rankings, etc)
4. Core packages internos proibidos (core, fabric, persistence, lifecycle, timer)
5. Servidor thread para Minecraft, PlatformScheduler para cross-thread
6. Persistencia propria do modulo via YAML. Nao use EventStorage
7. Testes com stubs (copiar de EventCoreTest)
8. fabric.mod.json com depends bigbangeventos >=0.1.0
