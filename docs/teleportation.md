# Teleportation — BigBangEventos

## Como Mundos são Resolvidos

1. `StoredLocation` contém `dimension` como ResourceLocation (ex: `minecraft:overworld`)
2. `FabricTeleportService` resolve via `server.registryAccess().registryOrThrow(Registries.DIMENSION)`
3. Busca `ServerLevel` correspondente no `MinecraftServer`
4. Se mundo não encontrado → erro `WORLD_NOT_FOUND`

## Como Erros são Tratados

| Código | Causa | Mensagem |
|--------|-------|----------|
| `player_offline` | Jogador offline | "Jogador não está online" |
| `invalid_location` | Coordenadas não-finitas | "Coordenadas inválidas" |
| `world_not_found` | Dimensão não encontrada | "Mundo X não encontrado" |
| `teleport_failed` | Exceção durante teleporte | "Não foi possível teleportar" |

## Teleporte entre Dimensões

- `ServerPlayer.teleportTo(ServerLevel, x, y, z, yaw, pitch)` lida com cross-dimension
- Minecraft gerencia carregamento de chunks automaticamente
- Yaw e pitch são preservados

## Fluxos de Teleporte

### Entrada no Evento
```
snapshot persistido → participante registrado → teleporte para LOBBY
```

### Início do Evento
```
estado → RUNNING → teleporte para ENTRANCE (todos participantes ativos)
```

### Saída / Conclusão
```
se teleport-to-exit-on-leave: true → teleporte para EXIT
senão → restaura localização original (do snapshot)
```

### Cancelamento
```
todos participantes → teleporte para EXIT (se configurada) ou localização original
```

## Limitações

- Não há sistema de "safe teleport" (verificação de bloco seguro)
- Coordenadas inválidas (NaN, Infinity) são rejeitadas
- Mundo descarregado → erro
- Não há fallback silencioso para overworld
