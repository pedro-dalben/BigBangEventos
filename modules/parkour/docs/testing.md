# Testes — Módulo Parkour

## Como Testar

```bash
# No diretório raiz do BigBangEventos
./gradlew :modules:parkour:test
```

## Estrutura de Testes

```
src/test/java/com/pedrodalben/bigbangeventos/modules/parkour/
  ParkourModuleTest.java            # Testes de integração do módulo
  ParkourDataStoreTest.java         # Testes de persistência
  ParkourCheckpointServiceTest.java # Testes de lógica de checkpoint
  ParkourSessionServiceTest.java    # Testes de sessão
  ParkourFallServiceTest.java       # Testes de detecção de queda
```

## Stubs

Copie os stubs de `EventCoreTest.java`:

| Stub | Origem | Uso |
|------|--------|-----|
| `MemoryStorage` | `EventCoreTest` | Storage em memória |
| `StubSnapshotGateway` | `EventCoreTest` | Snapshot simulado |
| `StubTeleportService` | `EventCoreTest` | Teleporte simulado |
| `StubPlayerService` | `EventCoreTest` | Player service simulado |
| `StubScheduler` | `EventCoreTest` | Scheduler síncrono |
| `MutableClock` | `EventCoreTest` | Clock ajustável |

## Testes Recomendados

### 1. Criação e Validação de Evento

```java
@Test
void criarEventoParkourValidaConfiguracao() {
    EventEngine engine = createEngine();
    assertTrue(engine.create("parkour_test", "parkour", "server").success());

    EventDefinition d = engine.definition("parkour_test").orElseThrow();
    d.location(LocationName.LOBBY, location());
    d.location(LocationName.ENTRANCE, location());
    d.location(LocationName.EXIT, location());

    // Adicionar checkpoint
    d.putTrigger(new EventTrigger("checkpoint_1", TriggerType.SIGN_INTERACT));
    d.putTrigger(new EventTrigger("finish", TriggerType.SIGN_INTERACT));

    d.typeSetting("parkour", Map.of(
        "checkpoint-order", "strict",
        "fall-distance-threshold", 10
    ));
    engine.save(d);

    ValidationResult vr = engine.validator().validate(d);
    assertTrue(vr.valid(), vr.issues().toString());
}
```

### 2. Participação e Checkpoints

```java
@Test
void jogadorCompletaCheckpointsEConclui() {
    EventEngine engine = createEngine();
    criarEventoBasico(engine);

    UUID player = UUID.randomUUID();
    assertTrue(engine.join("parkour_test", player, "player", false, true).success());
    assertTrue(engine.start("parkour_test").success());

    // Simular ativação de checkpoint
    OperationResult r = engine.activateTrigger(
        "parkour_test", "checkpoint_1", player, "player",
        (id, perm) -> true, (id, msg) -> {});
    assertTrue(r.success());

    // Simular conclusão
    r = engine.activateTrigger(
        "parkour_test", "finish", player, "player",
        (id, perm) -> true, (id, msg) -> {});
    assertTrue(r.success());

    // Verificar estado do jogador
    var session = engine.activeSession("parkour_test");
    assertTrue(session.isPresent());
    session.get().participant(player).ifPresent(p -> {
        assertEquals(ParticipantState.FINISHED, p.state());
    });
}
```

### 3. DataStore

```java
@Test
void dataStoreSalvaECarregaDados() {
    Path tempDir = createTempDir();
    ParkourDataStore store = new ParkourDataStore(tempDir);

    UUID pid = UUID.randomUUID();
    UUID sid = UUID.randomUUID();

    ParkourPlayerData data = new ParkourPlayerData(pid, sid);
    data.setLastCheckpoint(3);
    data.setFalls(2);
    data.addCompletedCheckpoint("checkpoint_1");
    data.addCompletedCheckpoint("checkpoint_2");
    data.addCompletedCheckpoint("checkpoint_3");
    data.setStartedAt(System.currentTimeMillis() - 30000);
    data.setFinishedAt(System.currentTimeMillis());

    store.save(pid, sid, data);

    ParkourPlayerData loaded = store.get(pid, sid);
    assertEquals(3, loaded.getLastCheckpoint());
    assertEquals(2, loaded.getFalls());
    assertTrue(loaded.getElapsedMillis() > 0);
}
```

### 4. Queda

```java
@Test
void jogadorCaiEVoltaAoCheckpoint() {
    ParkourSessionService sessionService = mockSessionService();
    ParkourFallService fallService = new ParkourFallService(
        sessionService, teleportService, scheduler, 10);

    UUID sid = UUID.randomUUID();
    UUID pid = UUID.randomUUID();
    ParkourPlayerData data = new ParkourPlayerData(pid, sid);
    data.setLastCheckpoint(2);
    sessionService.addPlayer(sid, pid, data);

    // Simular queda
    fallService.handleFall(pid, sid);

    assertEquals(1, data.getFalls()); // aumentou 1
    assertEquals(2, data.getLastCheckpoint()); // voltou ao último
}
```

### 5. Ordem Estrita

```java
@Test
void ordemEstritaImpedeCheckpointPulado() {
    ParkourCheckpointService service = new ParkourCheckpointService(dataStore);

    UUID pid = UUID.randomUUID();
    UUID sid = UUID.randomUUID();
    ParkourPlayerData data = new ParkourPlayerData(pid, sid);
    data.setLastCheckpoint(0); // nenhum checkpoint
    dataStore.save(pid, sid, data);

    // Tentar checkpoint 2 sem ter o 1
    boolean result = service.validateCheckpoint(pid, sid, "checkpoint_2", "strict");
    assertFalse(result);

    // Completar checkpoint 1
    data.setLastCheckpoint(1);
    dataStore.save(pid, sid, data);

    // Agora checkpoint 2 funciona
    result = service.validateCheckpoint(pid, sid, "checkpoint_2", "strict");
    assertTrue(result);
}
```

## Boas Práticas

1. Crie um `EventEngine` fresco em cada teste (`@BeforeEach`).
2. Use `MutableClock` para testes com tempo.
3. Teste validação com configurações inválidas.
4. Teste casos de borda: 0 checkpoints, ordem any, sem finish.
5. Teste persistência: salvar, carregar, sobrescrever.
6. Teste concorrência (teoricamente — single thread no servidor).
7. Teste em memória primeiro, depois com arquivos temporários.
