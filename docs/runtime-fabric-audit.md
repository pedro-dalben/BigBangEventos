# Runtime Fabric Audit — BigBangEventos

## Estado Atual (2026-07-21)

### Funcionalidades Reais
- Bootstrap Fabric (`BigBangEventosMod`) — inicialização mínima, apenas SERVER_STARTING e registro de comandos
- Máquina de estados de sessão (`SessionLifecycle`) — transições validadas, cancelamento
- Definições de eventos com persistência YAML (`LocalEventStorage`)
- Serviço de participação básico — join/leave sem snapshot, sem teleporte
- Sistema de gatilhos com interação de placas (SIGN_INTERACT via `UseBlockCallback`)
- Comandos básicos (`/evento create/list/info/open/close/start/finish/cancel/entrar/sair/set/trigger`)
- Timer baseado em Clock (`SessionTimer`) — pausa/resume/expiração
- Ranking básico (`COMPLETION_ORDER`)
- 4 testes unitários

### Funcionalidades Simuladas / Não Implementadas
- **SnapshotGateway** — interface pura, sem implementação Fabric. Mock nos testes.
- **Snapshot** — `PlayerSnapshot` armazena `Map<String,String>`, sem serialização real de inventário
- **Teleporte** — nenhuma implementação. Comandos de join não teleportam
- **Serviço de jogadores** — não existe. Código acessa `ServerPlayer` diretamente nos comandos/eventos
- **Scheduler** — não existe. Nenhuma operação usa thread do servidor explicitamente
- **Desconexão/Reconexão** — sem listeners, sem tratamento
- **Grace period** — config definido mas não implementado
- **Recuperação pós-reinício** — `unfinishedSessions()` existe no storage mas não há serviço de recovery
- **Encerramento limpo** — sem callback de shutdown
- **Persistência de snapshots** — não existe
- **Persistência de participantes** — `saveSession()` salva apenas metadados da sessão, não participantes
- **Restauração idempotente** — `SnapshotService.restore()` básico, sem estados, sem persistência
- **Limpeza de inventário** — `SnapshotGateway.clearInventory()` sem implementação
- **Bind de placa** — funcional, mas sem timeout, sem cancelamento, sem validação de bloco suportado
- **Deduplicação de interação** — não implementada

### Abstrações Existentes
| Classe | Tipo | Estado |
|--------|------|--------|
| `EventEngine` | Domínio/Facade | Funcional, sem integração com runtime |
| `EventStorage` | Interface de persistência | Mínima — sem snapshots, sem participantes individuais |
| `LocalEventStorage` | Implementação Fabric | YAML, salva definições e metadados de sessão apenas |
| `SnapshotGateway` | Interface de plataforma | Definida, sem implementação Fabric |
| `SnapshotService` | Domínio | Básico, sem persistência, sem estados |
| `PlayerSnapshot` | Domínio | Simplificado (Map<String,String>), sem estados |
| `ParticipationService` | Domínio | Básico, sem snapshot/teleporte/rollback |
| `SessionLifecycle` | Domínio | Completo |
| `PermissionChecker` | Interface de plataforma | Definida, implementada inline nos comandos |
| `TriggerEffects` | Interface de plataforma | Definida, implementada inline nos comandos |
| `TriggerService` | Domínio | Funcional |
| `EventValidator` | Domínio | Funcional |

### Problemas Encontrados
1. **Sem snapshot real**: Nenhum dado de jogador é capturado ao entrar no evento
2. **Sem teleporte**: Jogador não é movido ao lobby/entrada/saída
3. **Persistência incompleta**: Participantes não são salvos, snapshots não existem em disco
4. **Sem recuperação**: Reinício do servidor perde sessões ativas sem restaurar jogadores
5. **Sem tratamento de desconexão**: Jogador que desconecta durante evento fica em estado inconsistente
6. **Sem concorrência**: `synchronized` nos métodos mas sem travas por player/session
7. **ParticipantState.DISCONNECTED** e **RESTORE_PENDING** definidos mas nunca usados
8. **FabricEvents** acessa `BigBangEventos.engine()` diretamente, sem abstração
9. **Sem validação de inventário**: Modos `EVENT_KIT` e `ISOLATED` retornam erro, mas o sistema não bloqueia eventos que os usam
10. **Sem relógio injetável nos serviços Fabric**: `System.currentTimeMillis()` ainda não é problema pois não há implementação Fabric

### Plano de Adaptação

#### Fase 1: Abstrações de Plataforma (domínio)
1. Criar `PlatformPlayerService` — interface para operações de jogador
2. Criar `PlatformTeleportService` — interface para teleporte
3. Criar `PlatformScheduler` — interface para scheduling
4. Criar `StoredLocation` — modelo de localização persistente
5. Revampar `PlayerSnapshot` com `SnapshotState`
6. Criar `PlayerRestoreService` — restauração idempotente
7. Criar `EventPlayerStateService` — aplicar estado de evento
8. Criar `DisconnectService` — grace period, reconexão
9. Criar `SessionRecoveryService` — recuperação pós-reinício

#### Fase 2: Implementações Fabric
1. `FabricPlayerService` implementa `PlatformPlayerService`
2. `FabricTeleportService` implementa `PlatformTeleportService`
3. `FabricScheduler` implementa `PlatformScheduler`
4. `FabricSnapshotGateway` implementa `SnapshotGateway` com serialização NBT
5. `FabricLocationAdapter` para converter posições Minecraft

#### Fase 3: Integração
1. `EventEngine` recebe todos os serviços via construtor
2. `ParticipationService` integra snapshot + teleporte + rollback
3. `BigBangEventosMod` registra listeners de conexão/desconexão/shutdown
4. `FabricEvents` ganha deduplicação
5. `EventoCommand` ganha comandos de recovery/debug

### Riscos de Perda ou Duplicação de Dados

| Risco | Gravidade | Mitigação |
|-------|-----------|-----------|
| Snapshot capturado após limpar inventário | CRÍTICO | Ordem estrita: snapshot → persistir → limpar |
| Restauração aplicada 2x (duplica itens) | CRÍTICO | Estado RESTORED + lock por snapshot |
| Jogador offline perde snapshot | ALTO | Snapshot persistido em disco, restauração pendente |
| Servidor reinicia com snapshot em memória | ALTO | Persistir snapshot imediatamente após captura |
| Teleporte parcial (inventário limpo, não teleportado) | ALTO | Rollback na falha de teleporte |
| Concorrência join/leave | MÉDIO | Lock por playerId + sessionId |
| Grace period expira sem restaurar | MÉDIO | Restauração pendente persiste, tentada no próximo login |
| Sessão cancelada não restaura offline | MÉDIO | Marcar todos para restauração pendente, tentar no login |
