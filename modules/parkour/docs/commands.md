# Comandos — Módulo Parkour

## Visão Geral

O módulo Parkour registra comandos sob `/parkour` para jogadores e
administradores.

## Comandos de Jogador

### `/parkour`

Mostra o status do parkour atual do jogador.

```
Exemplo:
> /parkour
[Parkour] Evento: meu_parkour
[Parkour] Checkpoint: 2/5 | Quedas: 3 | Tempo: 45s
```

### `/parkour leave`

Sai do parkour atual. O jogador é teleportado ao EXIT e o inventário
restaurado.

```
Exemplo:
> /parkour leave
[Parkour] Você saiu do parkour.
```

### `/parkour top`

Mostra o top 10 do ranking do parkour atual.

```
Exemplo:
> /parkour top
[Parkour] Top 10:
  1. jogador1 - 45.2s (0 quedas)
  2. jogador2 - 52.1s (2 quedas)
  3. jogador3 - 1m05s (1 queda)
```

### `/parkour checkpoints`

Lista todos os checkpoints do parkour atual com status.

```
Exemplo:
> /parkour checkpoints
[Parkour] Checkpoints:
  ✓ Checkpoint 1 (100, 65, 10)
  ✗ Checkpoint 2 (110, 70, 10)
  ✗ Checkpoint 3 (120, 75, 10)
  ✗ Finish (130, 80, 10)
```

## Comandos de Administrador (op level 2)

### `/parkour admin reset <jogador>

Reseta o progresso de um jogador no parkour atual.

- Volta o checkpoint para 0
- Zera quedas
- Teleporta para o ENTRANCE
- Reinicia cronômetro

```
Exemplo:
> /parkour admin reset jogador1
[Parkour] jogador1 foi resetado.
```

### `/parkour admin tp <jogador> <checkpoint>

Teleporta um jogador para um checkpoint específico.

```
Exemplo:
> /parkour admin tp jogador1 3
[Parkour] jogador1 teleportado para Checkpoint 3.
```

### `/parkour admin info`

Mostra informações detalhadas sobre a sessão atual.

```
Exemplo:
> /parkour admin info
[Parkour] Sessão: meu_parkour
[Parkour] Estado: RUNNING
[Parkour] Participantes: 5
[Parkour] Jogadores ativos: 3
[Parkour] Concluídos: 2
[Parkour] Quedas totais: 12
```

## Permissões

| Comando | Permissão |
|---------|-----------|
| `/parkour` | Nenhuma (jogador) |
| `/parkour leave` | Nenhuma (jogador) |
| `/parkour top` | Nenhuma (jogador) |
| `/parkour checkpoints` | Nenhuma (jogador) |
| `/parkour admin *` | `source.hasPermission(2)` (op) |

## Registro

Os comandos são registrados em `ParkourModule.onEnable()` via
`CommandRegistrationCallback`:

```java
CommandRegistrationCallback.EVENT.register((dispatcher, ra, env) -> {
    ParkourCommand.register(dispatcher, sessionService, checkpointService);
});
```

## Implementação de Referência

Veja `EventoCommand.java` no core (`command/EventoCommand.java`) para
padrões de implementação com Brigadier.
