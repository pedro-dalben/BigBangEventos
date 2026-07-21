# Persistência

## Opções Disponíveis

### 1. Core Storage (Para Definições e Sessões)

O core gerencia persistência de:
- `EventDefinition` — configurações do evento (YAML em `config/bigbangeventos/events/`).
- `EventSession` — sessões ativas (YAML em `config/bigbangeventos/sessions/`).
- `PlayerSnapshot` — snapshots de jogador (YAML em `config/bigbangeventos/snapshots/`).

**Módulos NÃO devem usar `EventStorage` diretamente.** É um pacote interno
(`core.persistence`). Use `BigBangEventosApi` para operações em
definições e sessões.

### 2. Module-Owned YAML (Para Dados Específicos)

Cada módulo gerencia seus próprios arquivos no diretório de dados:

```java
Path dataDir = ctx.dataDirectory(); // config/bigbangeventos/modules/meumod/
Path configFile = dataDir.resolve("config.yml");
Path playersDir = dataDir.resolve("players/");
```

O diretório é criado automaticamente pelo core e isolado por módulo.

### 3. SnakeYAML (Incluso no Core)

O core já inclui `org.yaml:snakeyaml:2.2` como dependência. Módulos
podem usar SnakeYAML diretamente para ler/escrever YAML:

```java
import org.yaml.snakeyaml.Yaml;

Yaml yaml = new Yaml();
Map<String, Object> data = yaml.load(Files.newBufferedReader(configFile));

Map<String, Object> output = new LinkedHashMap<>();
output.put("version", 1);
output.put("players", playerData);
yaml.dump(output, Files.newBufferedWriter(configFile));
```

## Estrutura de Diretórios

```
config/
  bigbangeventos/
    config.yml              # Config global do core
    messages.yml            # Mensagens do core
    events/                 # Definições de evento (core)
      evento1.yml
      evento2.yml
    sessions/               # Sessões ativas (core)
      uuid1.yml
    snapshots/              # Snapshots de jogador (core)
      uuid_jogador/
        uuid_snapshot.yml
    modules/
      parkour/              # Dados do módulo parkour
        config.yml
        players/
          uuid_jogador.yml
      meumod/               # Dados do seu módulo
        config.yml
        data.yml
```

## Regras

1. **Não use `EventStorage` diretamente.** Pacote interno.
2. **Não modifique arquivos de outros módulos.** Cada módulo tem seu
   diretório.
3. **Não dependa de estrutura de arquivos do core.** Eles podem mudar
   entre versões.
4. **Sempre feche recursos** (readers, writers) adequadamente.
5. **Use escrita atômica** se possível (escrever em .tmp, renomear).

## Escrita Atômica (Recomendado)

```java
private void saveAtomic(Path target, Map<String, Object> data) {
    Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
    try (Writer w = Files.newBufferedWriter(tmp)) {
        yaml.dump(data, w);
    }
    Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE);
}
```

## Cache em Memória

Para desempenho, mantenha um cache em memória e persista:

- Em `onDisable`
- Periodicamente (ex: a cada 5 minutos via `PlatformScheduler`)
- Após mudanças críticas

```java
// Agendar salvamento periódico
ScheduledHandle handle = scheduler.scheduleRepeating(
    Duration.ofMinutes(5), () -> saveAll());
```
