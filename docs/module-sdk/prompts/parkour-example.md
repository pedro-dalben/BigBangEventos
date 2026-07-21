# Template Preenchido: Módulo Parkour

Este é o template `create-event-module.md` preenchido com as
características reais do módulo Parkour.

## Instruções para IA

Leia `docs/module-sdk/ai-context.md` e `modules/parkour/README.md` antes
de gerar código. Não altere o BigBangEventos Core.

## Template

Crie um módulo Fabric server-side para BigBangEventos com as seguintes
características:

### Identidade

- ID do módulo: `parkour_module`
- Nome do evento: Parkour
- Descrição: Evento de parkour com checkpoints, cronômetro individual,
  contagem de quedas e ranking por tempo.
- Grupo do módulo: `com.pedrodalben.bigbangeventos.modules.parkour`

### Regras do Evento

1. Jogador começa no ENTRANCE quando o evento inicia.
2. Jogador percorre um trajeto com checkpoints numerados.
3. Cada checkpoint é uma placa (SIGN_INTERACT) que o jogador deve clicar.
4. Se o jogador cair do trajeto (cair no vazio ou sair da área), ele
   volta ao último checkpoint Visitado.
5. Cada queda incrementa o contador de falls.
6. O jogador completa ao clicar na placa de chegada.
7. O ranking é ordenado por tempo de conclusão (menor tempo = melhor).
8. Desconexão = grace period de 120 segundos. Se não reconectar, 
   é desqualificado.
9. Opcional: o jogador pode usar `/parkour leave` para sair.

### Comandos

| Comando | Descrição | Permissão |
|---------|-----------|-----------|
| `/parkour` | Status do parkour atual | Jogador |
| `/parkour leave` | Sair do parkour | Jogador |
| `/parkour top` | Top 10 ranking | Jogador |
| `/parkour checkpoints` | Listar checkpoints | Jogador |
| `/parkour admin reset <player>` | Resetar jogador | Admin (op 2) |
| `/parkour admin tp <checkpoint>` | Teleportar para checkpoint | Admin (op 2) |
| `/parkour admin info` | Info detalhada | Admin (op 2) |

### Configuração (typeSettings)

```yaml
parkour:
  checkpoint-fall-teleport: true        # Voltar ao checkpoint ao cair
  count-falls: true                     # Contar quedas
  fall-distance-threshold: 10           # Distância para considerar queda
  exit-area-teleport: true              # Teleportar ao sair da área
  allowed-checkpoint-order: "strict"    # strict|any
  allow-flight: false                   # Permitir voo durante evento
  auto-start: true                      # Iniciar cronômetro ao entrar
```

### Gatilhos (Triggers)

- `checkpoint_N` (SIGN_INTERACT) — cada checkpoint numerado
- `finish` (SIGN_INTERACT) — placa de chegada
- `checkpoint_N` (REGION_ENTER) — alternativa a placas baseadas em região

### Condições

- `EVENT_IS_RUNNING`
- `PLAYER_IS_PARTICIPANT`
- `PLAYER_NOT_FINISHED`
- `PLAYER_HAS_CHECKPOINT(N-1)` (para checkpoint N, se strict order)

### Ações

- `ADD_POINTS` (opcional, para checkpoint intermediário)
- `COMPLETE_CHECKPOINT` (marca checkpoint como completo)
- `PLAYER_COMPLETE` (para o checkpoint de chegada)
- `SEND_MESSAGE` (feedback visual)
- `SEND_TITLE` (feedback visual chamativo)

### Ranking

- `Rankings.TIME_ASCENDING` — vence quem completa em menos tempo.
- Empates resolvidos por menos quedas.

### Estrutura de Arquivos

```
parkour_module/
  build.gradle
  src/main/java/com/pedrodalben/bigbangeventos/modules/parkour/
    ParkourInitializer.java     (ModInitializer)
    ParkourModule.java          (BigBangEventModule)
    ParkourEventType.java       (EventType)
    ParkourDataStore.java       (persistência player)
    ParkourCommand.java         (comandos)
    ParkourSessionService.java  (lógica de sessão parkour)
    ParkourCheckpointService.java (checkpoints)
    ParkourFallService.java     (detecção de queda)
  src/main/resources/
    fabric.mod.json
  src/test/java/com/pedrodalben/bigbangeventos/modules/parkour/
    ParkourModuleTest.java
    ParkourDataStoreTest.java
    ParkourCheckpointServiceTest.java
```

### Testes

1. Criar evento parkour, validar configuração.
2. Jogador entra, evento inicia, cronômetro começa.
3. Jogador clica checkpoint 1, checkpoint registrado.
4. Jogador cai, volta ao último checkpoint, falls incrementa.
5. Jogador clica chegada, evento completa, ranking atualizado.
6. Jogador desconecta, grace period expira, jogador desqualificado.
7. Jogador completa, não pode completar novamente.
8. Ordem estrita: checkpoint 2 não pode ser ativado sem checkpoint 1.
9. Admin reset: jogador volta ao início.
10. Persistência: dados do jogador salvos e carregados.

### Observações

- Minecraft 1.21.1
- Fabric Loader >=0.18.4
- Fabric API >=0.116.13+1.21.1
- Java >=21
- Dependência: bigbangeventos >=0.1.0
- Não altere o core. Todo código de regra fica no módulo.
