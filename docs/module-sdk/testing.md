# Testes

## Como Testar Módulos

O core usa JUnit 5 para testes. Use `EventCoreTest.java` como referência
de como criar stubs e testar sem Minecraft.

## Stubs Prontos

O `EventCoreTest` contém stubs que você pode copiar para seu módulo:

| Stub | Classe | Propósito |
|------|--------|-----------|
| `MemoryStorage` | `EventCoreTest.MemoryStorage` | Implementa `EventStorage` em memória |
| `StubSnapshotGateway` | `EventCoreTest.StubSnapshotGateway` | Implementa `SnapshotGateway` em memória |
| `StubTeleportService` | `EventCoreTest.StubTeleportService` | Teleporte simulado |
| `StubPlayerService` | `EventCoreTest.StubPlayerService` | Serviço de jogador simulado |
| `StubScheduler` | `EventCoreTest.StubScheduler` | Scheduler que executa na mesma thread |
| `MutableClock` | `EventCoreTest.MutableClock` | Clock que permite avançar o tempo manualmente |

## Criando um EventEngine para Testes

```java
public class MeuModTest {
    private EventEngine engine;

    @BeforeEach
    void setUp() {
        engine = new EventEngine(
            new MemoryStorage(),
            Clock.systemUTC(),
            new StubSnapshotGateway(new HashMap<>(), new HashMap<>(), ""),
            new StubTeleportService(),
            new StubPlayerService(),
            new StubScheduler()
        );
        // Registrar tipo do módulo
        engine.types().register(new MeuTipoEvento());
    }

    @AfterEach
    void tearDown() {
        // Limpeza
    }
}
```

## Testando o EventType

```java
@Test
void meuTipoValidaConfig() {
    // Criar definição
    OperationResult r = engine.create("test", "meu_tipo", "server");
    assertTrue(r.success());

    EventDefinition d = engine.definition("test").orElseThrow();
    d.location(LocationName.LOBBY, createLocation());
    d.location(LocationName.ENTRANCE, createLocation());
    d.location(LocationName.EXIT, createLocation());
    d.typeSetting("tempo_limite", -1); // inválido
    engine.save(d);

    // Validar
    ValidationResult vr = engine.validator().validate(d);
    assertFalse(vr.valid());
    assertTrue(vr.issues().stream()
        .anyMatch(i -> i.code().equals("invalid_tempo")));
}

@Test
void meuTipoAbreSessao() {
    // Criar e abrir sessão
    assertTrue(engine.create("test", "meu_tipo", "server").success());
    EventDefinition d = engine.definition("test").orElseThrow();
    d.location(LocationName.LOBBY, createLocation());
    d.location(LocationName.ENTRANCE, createLocation());
    d.location(LocationName.EXIT, createLocation());
    engine.save(d);

    assertTrue(engine.open("test", null).success());
    var session = engine.activeSession("test");
    assertTrue(session.isPresent());
    assertEquals(SessionState.REGISTRATION_OPEN, session.get().state());
}
```

## Testando Participação

```java
@Test
void jogadorEntraESai() {
    assertTrue(engine.create("test", "meu_tipo", "server").success());
    EventDefinition d = engine.definition("test").orElseThrow();
    d.location(LocationName.LOBBY, createLocation());
    d.location(LocationName.ENTRANCE, createLocation());
    d.location(LocationName.EXIT, createLocation());
    engine.save(d);
    assertTrue(engine.open("test", null).success());

    UUID p = UUID.randomUUID();
    assertTrue(engine.join("test", p, "player", false, true).success());
    assertTrue(engine.leave(p, "test").success());
}
```

## Testando Dados do Módulo

Teste sua camada de armazenamento separadamente:

```java
@Test
void moduloSalvaECarregaDados() {
    MeuModDataStore store = new MeuModDataStore(temporaryDir);
    UUID pid = UUID.randomUUID();
    UUID sid = UUID.randomUUID();

    PlayerData data = new PlayerData();
    data.setCheckpoint(3);
    store.save(pid, sid, data);

    PlayerData loaded = store.get(pid, sid);
    assertEquals(3, loaded.getCheckpoint());
}
```

## Boas Práticas

1. Teste cada `EventType` isoladamente com stubs.
2. Teste validação de configuração.
3. Teste participação (join/leave).
4. Teste dados do módulo (save/load/cache).
5. Teste comandos (mock `CommandContext`).
6. Não dependa de Minecraft nos testes unitários.
7. Use `@BeforeEach` para criar um `EventEngine` fresco.

## Estrutura de Diretórios para Testes

```
src/test/java/com/meumod/
  MeuModTest.java
  MeuTipoEventoTest.java
  MeuModDataStoreTest.java
  MeuModCommandTest.java
```
