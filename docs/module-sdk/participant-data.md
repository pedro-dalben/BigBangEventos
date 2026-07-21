# Dados do Participante

## Dados Simples: Map<String, String>

Cada `EventParticipant` tem um `Map<String, String>` (acessado via
`EventParticipant.data()`) para dados simples.

```java
EventParticipant p = session.participant(playerId).orElse(null);
if (p != null) {
    // Escrever
    p.data("checkpoint", "3");
    p.data("tempo_inicio", String.valueOf(System.currentTimeMillis()));

    // Ler
    String checkpoint = p.data().get("checkpoint");
    String tempoInicio = p.data().get("tempo_inicio");
}
```

### Limitações do Map<String, String>

- Apenas strings como valores
- Persistido como parte da sessão no YAML do core
- Dados complexos (listas, objetos) precisam de serialização manual
- Não há tipagem ou validação de schema

## Dados Complexos: Armazenamento Próprio

Para dados complexos, o módulo deve gerenciar seu próprio armazenamento
(ver `persistence.md`). A chave deve ser composta pelo par
`playerId + sessionId`.

```java
public class MeuModDataStore {
    private final Path dataDir;
    private final Map<String, PlayerData> cache = new HashMap<>();

    public MeuModDataStore(Path dataDir) {
        this.dataDir = dataDir;
    }

    public PlayerData get(UUID playerId, UUID sessionId) {
        String key = playerId + ":" + sessionId;
        return cache.computeIfAbsent(key, k -> load(playerId, sessionId));
    }

    public void save(UUID playerId, UUID sessionId, PlayerData data) {
        String key = playerId + ":" + sessionId;
        cache.put(key, data);
        persist(playerId, sessionId, data);
    }

    private PlayerData load(UUID playerId, UUID sessionId) {
        Path file = dataDir.resolve(playerId + "_" + sessionId + ".yml");
        if (Files.exists(file)) {
            // usar SnakeYAML para carregar
        }
        return new PlayerData();
    }

    private void persist(UUID playerId, UUID sessionId, PlayerData data) {
        Path file = dataDir.resolve(playerId + "_" + sessionId + ".yml");
        // usar SnakeYAML para salvar
    }
}
```

## Exemplo: Dados de Parkour

```java
public class ParkourPlayerData {
    private long startedAt;
    private long finishedAt;
    private int falls;
    private List<String> completedCheckpoints = new ArrayList<>();

    public long elapsedMillis() {
        return finishedAt - startedAt;
    }
    // getters/setters
}
```

## Boas Práticas

1. **Dados simples** → use `EventParticipant.data()` (checkpoint atual, estado).
2. **Dados estruturados** → armazenamento próprio do módulo.
3. **Sempre use playerId + sessionId como chave composta.**
4. **Não armazene objetos grandes no `data()` do core.** O YAML da sessão
   ficará enorme e o desempenho será prejudicado.
5. **Dados do módulo são responsabilidade do módulo.** O core não limpa
   dados do módulo quando um evento termina.
6. **Considere concorrência.** A thread do servidor é single-thread, mas
   acessos assíncronos podem ocorrer.

## Persistência Automática do Core

O `data()` do participante é persistido automaticamente quando a sessão
é salva (`EventStorage.saveSession`). Dados do módulo em arquivos
próprios precisam de salvamento explícito.
