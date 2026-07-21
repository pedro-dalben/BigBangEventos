# Parkour MVP — Relatorio Final de Homologacao

Data: 2026-07-21
Branch: feat/parkour-runtime-uat

---

## 1. Auditoria Inicial

### Estado do Core

| Componente | Status | Detalhes |
|-----------|--------|----------|
| EventEngine | COMPLETO | Sessoes, participantes, transicoes |
| SessionLifecycle | COMPLETO | Maquina de estados com transicoes validas |
| ParticipationService | COMPLETO | Join/leave com double-lock, snapshots |
| SnapshotService | COMPLETO | KEEP + CLEAR_AND_RESTORE, estados |
| PlayerRestoreService | COMPLETO | Restauro idempotente, componentes parciais |
| DisconnectService | COMPLETO | Grace period 120s, estado DISCONNECTED |
| SessionRecoveryService | COMPLETO | Recupera sessoes apos restart |
| TriggerService | COMPLETO | Condicoes, acoes, limites, cooldowns |
| TriggerType.REGION_ENTER | **NOVO** | Implementado nesta fase |
| EventArea (sealed) | **NOVO** | CUBOID + RADIUS |
| RegionTriggerService | **NOVO** | Scanner periodico |

### Estado do Parkour

| Componente | Status | Detalhes |
|-----------|--------|----------|
| ParkourEventType | COMPLETO | Registro, lifecycle, onTriggerFired |
| ParkourSessionService | COMPLETO | Start/finish/cancel, periodic tick |
| ParkourCheckpointService | COMPLETO | CRUD, ordem, completacao, JSON serialization |
| ParkourFallService | COMPLETO | Y_LEVEL detection, cooldown, resets |
| ParkourTimerService | COMPLETO | Clock absoluto, formatacao MM:SS.mmm |
| ParkourCompletionService | COMPLETO | Idempotente, teleport, ranking |
| ParkourRankingService | COMPLETO | TIME_ASCENDING, finalizados primeiro |
| ParkourConfiguration | COMPLETO | typeSettings getters/setters |
| ParkourCommandRegistrar | COMPLETO | /evento parkour ... comandos |
| ParkourValidator | COMPLETO | Validacao de configuracao |

### Divergencias Documentais Corrigidas

| Doc | Antes | Depois |
|-----|-------|--------|
| REGION_ENTER | Dizia "implementado" | Implementado de fato |
| Checkpoints por regiao | Dizia "suportado" | Implementado via trigger system |
| EventArea | Apenas CUBOID | CUBOID + RADIUS (sealed interface) |

---

## 2. Implementacao Realizada

### Phase 1: REGION_ENTER Core

1. `EventArea` refatorado para sealed interface com `Cuboid` e `Radius` records
2. `EventTrigger` ganhou campo `area: Optional<EventArea>`
3. `RegionTriggerService` criado: scanner periodico (2 ticks) que verifica posicao de participantes ativos contra triggers REGION_ENTER/REGION_EXIT
4. Inside/outside state tracking por trigger+player
5. Cleanup automatico em finish, cancel, leave

### Phase 2: Bridge Core → Parkour

1. `EventType.onTriggerFired()` callback adicionado
2. `EventEngine.activateTrigger()` chama o callback apos execucao
3. `ParkourEventType.onTriggerFired()` implementado
4. `ParkourSessionService.onTriggerFired()` roteia trigger para checkpoint ou finish

### Phase 3: Fixes e Testes

1. 10 novos testes no core (cobrindo REGION_ENTER, areas, triggers, snapshots)
2. Total: 53 testes (18 core + 35 parkour), todos passando

### Arquivos Alterados

```
src/main/java/com/pedrodalben/bigbangeventos/
  definition/EventArea.java          (refatorado: sealed interface)
  trigger/EventTrigger.java           (adicionado: area field)
  trigger/RegionTriggerService.java   (NOVO: scanner periodico)
  core/EventEngine.java              (adicionado: regionTriggers, onTick, cleanup)
  eventtype/EventType.java           (adicionado: onTriggerFired callback)
  fabric/BigBangEventosMod.java      (adicionado: onTick call)

modules/parkour/src/main/java/br/com/bigbangcraft/eventos/parkour/
  ParkourEventType.java              (adicionado: onTriggerFired impl)
  ParkourSessionService.java         (adicionado: onTriggerFired handler)

src/test/java/com/pedrodalben/bigbangeventos/
  EventCoreTest.java                 (adicionado: +10 testes)

docs/uat/
  parkour-pre-uat-audit.md           (NOVO)
  parkour-mvp-uat.md                 (NOVO)
```

---

## 3. Build

```
./gradlew clean test build  →  BUILD SUCCESSFUL in 12s
```

| Metrica | Valor |
|---------|-------|
| Testes Core | 18 (8 originais + 10 novos) |
| Testes Parkour | 35 |
| Testes Totais | 53 |
| Resultado | Todos passando |
| Core JAR | build/libs/bigbangeventos-0.1.0.jar (144 KB) |
| Parkour JAR | modules/parkour/build/libs/bigbangeventos-parkour-0.1.0.jar (64 KB) |

---

## 4. Artefatos

| Arquivo | Tamanho | Descricao |
|---------|---------|-----------|
| `build/libs/bigbangeventos-0.1.0.jar` | 144 KB | Core + Fabric runtime |
| `build/libs/bigbangeventos-0.1.0-sources.jar` | 61 KB | Fontes do Core |
| `modules/parkour/build/libs/bigbangeventos-parkour-0.1.0.jar` | 64 KB | Modulo Parkour |

Core contem:
- fabric.mod.json com entrypoint `main`
- Codigo do Core e Fabric runtime
- Nao contem Parkour, nao contem BigBangRegions

Parkour contem:
- fabric.mod.json com entrypoint `bigbangeventos:event_module`
- Codigo do Parkour
- Dependencia declarada: `bigbangeventos >= 0.1.0`
- Nao empacota o Core, nao contem BigBangRegions

---

## 5. UAT

Documento: `docs/uat/parkour-mvp-uat.md`

23 cenarios documentados:

| Status | Count |
|--------|-------|
| NOT_RUN | 23 |
| PASS | 0 |
| FAIL | 0 |
| BLOCKED | 0 |

Homologacao requer execucao em staging Cobbleverse com cliente real.

---

## 6. Limites Conhecidos

### L-01: Teleporte e cronometro
Timer inicia antes da confirmacao do teleporte. Se teleporte falhar,
cronometro ja esta rodando. Risco: BAIXO — teleporte raramente falha.

### L-02: Estado de snapshot nao persistido
SnapshotState (RESTORING, RESTORED) existe apenas em memoria.
Apos restart, snapshot pode ser restaurado novamente se carregado via YAML.
SessionRecoveryService tenta mitigar verificando estado em memoria.
Risco: MEDIO — snapshot carregado do YAML nao tem estado persistido.

### L-03: EventArea config apenas via API
Nao ha comando para configurar area de um trigger (set-area).
Admin precisa usar API ou editar YAML manualmente.
Risco: BAIXO — implementavel com comando futuro.

---

## 7. Estado Final

### Funcionando e Testado (com testes automatizados)

- [x] SessionLifecycle (4 transicoes)
- [x] ParticipationService (join/leave/full)
- [x] SessionTimer (clock absoluto)
- [x] SnapshotService (idempotente, KEEP, CLEAR_AND_RESTORE)
- [x] PlayerRestoreService (idempotente, concorrente)
- [x] TriggerService (condicoes, maxUses, cooldown, disabled)
- [x] DisconnectService (grace period)
- [x] Teleporte (offline)
- [x] EventArea (CUBOID e RADIUS containment)
- [x] ParkourConfiguration (validacao, setters/getters)
- [x] ParkourCheckpoint (ordem, repeticao, radius containment)
- [x] ParkourTimerService (formatacao, start/stop)
- [x] ParkourFallService (contador, tentativas)
- [x] ParkourParticipantData (CRUD, service)

### Implementado mas Nao Homologado (requer cliente real)

- [ ] REGION_ENTER scanning (core implementado, testado unitariamente)
- [ ] Parkour checkpoint via SIGN_INTERACT (bridge implementado)
- [ ] Parkour checkpoint via REGION_ENTER (bridge implementado)
- [ ] Parkour finish via SIGN_INTERACT (bridge implementado)
- [ ] Parkour finish via REGION_ENTER (bridge implementado)
- [ ] Snapshot com itens modded (FabricSnapshotGateway implementado)
- [ ] Recovery apos restart (SessionRecoveryService implementado)
- [ ] Grace period na pratica (DisconnectService implementado)
- [ ] Dois jogadores simultaneos (ConcurrentHashMap, testado unitariamente)

### Pendente

- [ ] Comando para set-area em trigger REGION_ENTER
- [ ] Persistencia do SnapshotState no YAML
- [ ] Teleporte-confirm antes de iniciar timer

### Pronto para Producao?

NAO. Criterios nao atendidos:
- Snapshot com itens modded (Cobblemon) nao testado em staging
- Desconexao/reconexao nao testada com cliente real
- Dois jogadores nao testado com clientes reais
- Reinicio nao testado em staging
- REGION_ENTER nao testado com jogador real

---

## 8. Proximos Passos

1. Instalar JARs no staging Cobbleverse
2. Executar UAT-01 a UAT-23
3. Corrigir bugs encontrados
4. Re-executar testes
5. Atualizar este relatorio
6. Somente apos todos PASS → liberar para producao

---

## 9. Git

Commits pendentes (ainda nao realizados):

```
feat(triggers): implement generic REGION_ENTER detection with EventArea types
feat(triggers): add EventTrigger area field and RegionTriggerService scanner
feat(api): add onTriggerFired callback to EventType interface
feat(parkour): support trigger-based checkpoints and finish via onTriggerFired
test(triggers): cover REGION_ENTER, EventArea, trigger limits and snapshot edge cases
docs(uat): document Parkour staging validation and pre-UAT audit
```
