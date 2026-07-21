# Ciclo de Vida — Módulo Parkour

## Fases do Módulo

### ParkourModule.onLoad()

1. Ler `config.yml` do módulo.
2. Inicializar `ParkourDataStore` com o diretório de dados.
3. Inicializar serviços (`ParkourSessionService`, `ParkourCheckpointService`,
   `ParkourFallService`).
4. Validar versão da API.

### ParkourModule.onEnable()

1. Registrar `ParkourEventType` no `EventTypeRegistry`.
2. Registrar comandos via `CommandRegistrationCallback`.
3. Registrar listeners:
   - `ServerTickEvents.START_SERVER_TICK` → verificação de queda
   - `ServerPlayConnectionEvents.DISCONNECT` → limpeza de sessão
4. Iniciar salvamento periódico (a cada 5 minutos).

### ParkourModule.onDisable()

1. Salvar todos os dados pendentes (`ParkourDataStore.flushAll()`).
2. Salvar configuração.
3. Cancelar tarefas agendadas.
4. Notificar participantes ativos (opcional).

## Fases do EventType Parkour

### ParkourEventType.validate()

Valida a configuração específica do tipo parkour:

```java
@Override
public ValidationResult validate(EventDefinition definition) {
    ValidationResult r = ValidationResult.empty();
    Map<String, Object> settings = definition.typeSettings();

    // Verificar campos obrigatórios
    if (!settings.containsKey("parkour")) {
        r.add(ValidationLevel.ERROR, "missing_config",
            "Seção 'parkour' em typeSettings é obrigatória");
    }

    // Verificar ordem
    String order = settings.getOrDefault("checkpoint-order", "strict").toString();
    if (!order.equals("strict") && !order.equals("any")) {
        r.add(ValidationLevel.ERROR, "invalid_order",
            "checkpoint-order deve ser 'strict' ou 'any'");
    }

    // Verificar se há pelo menos um checkpoint
    if (definition.triggers().stream()
            .noneMatch(t -> t.id().startsWith("checkpoint_"))) {
        r.add(ValidationLevel.ERROR, "no_checkpoints",
            "Evento parkour precisa de pelo menos um checkpoint");
    }

    // Verificar se há gatilho de chegada
    if (definition.trigger("finish").isEmpty()) {
        r.add(ValidationLevel.WARNING, "no_finish",
            "Nenhum gatilho 'finish' encontrado");
    }

    return r;
}
```

### ParkourEventType.onSessionCreated()

```java
@Override
public void onSessionCreated(EventSession session) {
    // Inicializar dados da sessão parkour
    sessionService.initSession(session);
}
```

### ParkourEventType.onRegistrationOpen()

```java
@Override
public void onRegistrationOpen(EventSession session) {
    // Notificar que inscrições estão abertas
    // Broadcast no chat
}
```

### ParkourEventType.onSessionStart()

```java
@Override
public void onSessionStart(EventSession session) {
    // Iniciar cronômetros individuais
    for (EventParticipant p : session.participants()) {
        if (p.state() == ParticipantState.ACTIVE) {
            sessionService.startTimer(p.playerId(), session.id());
            p.data("checkpoint", "0");
            p.data("falls", "0");
        }
    }
    // Iniciar detecção de queda
    fallService.startMonitoring(session);
}
```

### ParkourEventType.onSessionFinish()

```java
@Override
public void onSessionFinish(EventSession session) {
    // Calcular ranking
    Rankings.TIME_ASCENDING.rank(session);

    // Parar detecção de queda
    fallService.stopMonitoring(session.id());

    // Salvar dados da sessão
    dataStore.flushSession(session.id());
}
```

### ParkourEventType.onSessionCancel()

```java
@Override
public void onSessionCancel(EventSession session, String reason) {
    fallService.stopMonitoring(session.id());
    sessionService.clearSession(session.id());
    dataStore.flushSession(session.id());
}
```

## Diagrama

```
          ┌──────────────┐
          │  CREATED     │ ParkourEventType.onSessionCreated()
          └──────┬───────┘
                 │
          ┌──────▼───────┐
          │REGISTRATION   │ ParkourEventType.onRegistrationOpen()
          │  OPEN         │
          └──────┬───────┘
                 │
          ┌──────▼───────┐
          │  RUNNING      │ ParkourEventType.onSessionStart()
          │               │ → inicia cronômetros
          │               │ → inicia detecção de queda
          │               │ → jogadores percorrem checkpoints
          └──────┬───────┘
                 │
          ┌──────▼───────┐
          │  FINISHED     │ ParkourEventType.onSessionFinish()
          │               │ → ranking calculado
          │               │ → dados salvos
          └──────────────┘

          ┌──────────────┐
          │  CANCELLED    │ ParkourEventType.onSessionCancel()
          │               │ → limpeza
          └──────────────┘
```
