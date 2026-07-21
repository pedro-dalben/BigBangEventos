# BigBangEventos

Motor de eventos server-side Fabric para Minecraft 1.21.1. Suporta tipos de
evento extensíveis via módulos Fabric separados.

## Estrutura

```
BigBangEventos/
├── src/                               # Core — motor de eventos
├── modules/parkour/                   # Módulo oficial Parkour (referência)
├── docs/                              # Documentação do core e runtime
├── docs/module-sdk/                   # SDK para desenvolvimento de módulos
├── examples/event-module-template/    # Template copiável para novos módulos
└── build.gradle                       # Build do core
```

### Core (`bigbangeventos-0.1.0.jar`)

Gerencia eventos, sessões, participantes, gatilhos, ranking, snapshots,
persistência YAML e ciclo de vida. Não depende de BigBangEssentials ou
Cobblemon.

### Module SDK

Framework para criar módulos de evento externos. Registre tipos de evento
personalizados via entrypoint Fabric `bigbangeventos:event_module`.

| Recurso | Como usar |
|---------|-----------|
| `BigBangEventModule` | Implementar interface no pacote `api` |
| `EventType` | Implementar e registrar via `ctx.typeRegistry()` |
| Comandos | `CommandRegistrationCallback` do Fabric |
| Gatilhos | `EventTrigger`, `ConditionType`, `ActionType` |
| Dados por participante | `EventParticipant.data()` ou storage próprio |
| Persistência | YAML próprio em `config/bigbangeventos/modules/<id>/` |
| Lifecycle | Hooks `onLoad` → `onEnable` → `onDisable` |

### Módulo Parkour (`bigbangeventos-parkour-0.1.0.jar`)

Tipo de evento `parkour` com checkpoints, cronômetro individual, quedas,
ranking por tempo. Módulo de referência para desenvolvimento de novos
eventos.

Ver `modules/parkour/README.md` para detalhes.

## Compilar

```bash
# Core + módulos + testes
./gradlew clean test build

# Apenas o core
./gradlew :build

# Apenas o módulo parkour
./gradlew :modules:parkour:build
```

## Artefatos

| Artefato | Caminho |
|----------|---------|
| Core | `build/libs/bigbangeventos-0.1.0.jar` |
| Parkour | `modules/parkour/build/libs/bigbangeventos-parkour-0.1.0.jar` |

## Instalar

1. Pare o servidor.
2. Coloque `bigbangeventos-0.1.0.jar` em `mods/`.
3. Opcional: coloque `bigbangeventos-parkour-0.1.0.jar` em `mods/`.
4. Confirme Fabric Loader 0.18.4, Fabric API 0.116.13+1.21.1, Java 21.
5. Inicie e verifique os logs: `[BigBangEventos] Module 'parkour' loaded`.
6. Execute `/evento` para ver os comandos do core.

## Dependências

| Dependência | Versão |
|-------------|--------|
| Minecraft | 1.21.1 |
| Fabric Loader | >=0.18.4 |
| Fabric API | >=0.116.13+1.21.1 |
| Java | >=21 |
| Gradle | 8.14.3 |

## Documentação

### Module SDK (`docs/module-sdk/`)

| Documento | Descrição |
|-----------|-----------|
| `README.md` | Visão geral do SDK |
| `architecture.md` | Arquitetura core vs módulos |
| `module-lifecycle.md` | Ciclo de vida: onLoad, onEnable, onDisable |
| `module-entrypoint.md` | Entrypoint Fabric e interfaces |
| `event-type-development.md` | Como criar um EventType |
| `commands.md` | Registro de comandos |
| `triggers-conditions-actions.md` | Gatilhos, condições e ações |
| `participant-data.md` | Dados por participante |
| `persistence.md` | Opções de persistência |
| `threading.md` | Regras de threading |
| `testing.md` | Como testar módulos |
| `compatibility.md` | Versionamento de API |
| `troubleshooting.md` | Problemas comuns |
| `ai-context.md` | Contexto otimizado para IA |
| `module-api-audit.md` | Auditoria da API |
| `prompts/create-event-module.md` | Prompt base para criação de módulos |
| `prompts/parkour-example.md` | Exemplo preenchido com Parkour |

### Parkour (`modules/parkour/docs/`)

| Documento | Descrição |
|-----------|-----------|
| `architecture.md` | Arquitetura do módulo Parkour |
| `configuration.md` | Configuração global e por evento |
| `commands.md` | Comandos `/parkour` |
| `lifecycle.md` | Ciclo de vida do evento Parkour |
| `checkpoints.md` | Checkpoints e validação de ordem |
| `triggers.md` | Gatilhos usados no Parkour |
| `testing.md` | Testes do módulo Parkour |
| `extending.md` | Como estender o Parkour |
| `ai-context.md` | Contexto IA para o Parkour |

## Template para Novos Módulos

Copie `examples/event-module-template/` para começar:

```bash
cp -r examples/event-module-template meu-novo-evento
cd meu-novo-evento
```

Siga as instruções no `README.md` do template e consulte
`docs/module-sdk/` para documentação completa.

## Criar um Evento Parkour

```bash
/evento create meu_parkour parkour
/evento edit meu_parkour
/evento set lobby     # fique no local
/evento set entrance
/evento set exit
/evento trigger create checkpoint_1 sign_interact
/evento trigger bind checkpoint_1   # clique na placa
# ... mais checkpoints ...
/evento validate meu_parkour
/evento open meu_parkour
/evento start meu_parkour
```

Ver `modules/parkour/README.md` para o guia completo.

## Testar

```bash
# Todos os testes
./gradlew clean test

# Testes do core
./gradlew :test

# Testes do Parkour
./gradlew :modules:parkour:test
```

## Smoke Test

```bash
./gradlew runServer
```

O servidor inicia com core e módulo Parkour carregados. Verifique os logs.

## Limitações Atuais

- Tipos de prova, GUI e recompensas não implementados.
- Snapshot real de inventário e teleportes em desenvolvimento.
- Checkpoints Parkour usam placas (SIGN_INTERACT); região (REGION_ENTER)
  não está implementada.
- Ranking é por sessão, não há recordes globais.

O teste completo do modpack Cobbleverse exige staging; não instale
diretamente em produção.

## Licença

All Rights Reserved.
