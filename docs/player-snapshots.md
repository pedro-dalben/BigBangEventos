# Player Snapshots — BigBangEventos

## Formato de Serialização

Itens são serializados usando o sistema NBT nativo do Minecraft 1.21.1:

```java
ItemStack.save(RegistryAccess) → Tag (CompoundTag)
Tag.toString() → String
```

Para restaurar:
```java
TagParser.parseTag(String) → Tag
ItemStack.parse(RegistryAccess, Tag) → Optional<ItemStack>
```

## Dados Capturados por Snapshot

| Campo | Tipo | Descrição |
|-------|------|-----------|
| playerId | UUID | ID do jogador |
| snapshotId | UUID | ID único do snapshot |
| sessionId | UUID | Sessão de origem |
| originalLocation | StoredLocation | Localização antes do evento |
| serializedInventory | Map<String,String> | Inventário principal (slot→NBT) |
| serializedArmor | Map<String,String> | Armadura (slot→NBT) |
| serializedOffhand | String | Mão secundária (NBT) |
| totalExperience | int | XP total |
| experienceLevel | int | Nível de XP |
| experienceProgress | float | Progresso do nível |
| health | double | Vida |
| absorption | double | Absorção |
| foodLevel | int | Nível de fome |
| saturation | float | Saturação |
| gameMode | String | Modo de jogo |
| allowFlight | boolean | Voo permitido |
| isFlying | boolean | Está voando |
| flySpeed | float | Velocidade de voo |
| walkSpeed | float | Velocidade de caminhada |
| selectedSlot | int | Slot selecionado |
| fireTicks | int | Ticks de fogo |
| fallDistance | float | Distância de queda |
| activeEffects | String | Efeitos ativos serializados (NBT) |
| extendedData | Map<String,String> | Dados estendidos |

## Estados do Snapshot

```
PENDING  → registro criado, captura pendente
CAPTURED → estado do jogador capturado e persistido
APPLIED  → estado de evento aplicado ao jogador
RESTORING → restauração em andamento
RESTORED → restauração concluída com sucesso
FAILED   → falha que exige nova tentativa
```

## Estratégia de Idempotência

- Cada snapshot tem estado explícito
- Restauração marca componentes individualmente (EnumSet<RestoreComponent>)
- Componentes: INVENTORY, ARMOR, OFFHAND, EXPERIENCE, HEALTH, HUNGER, EFFECTS, LOCATION, GAME_MODE, FLIGHT_STATE, MISC_STATE
- Restauração verifica estado atual antes de aplicar
- RESTORED → ignora chamadas repetidas
- RESTORING → bloqueia chamadas concorrentes

## Estratégia para Falha Parcial

- Cada componente restaurado é marcado individualmente
- Se inventário falha, snapshot vai para FAILED
- Componentes já restaurados não são reaplicados
- Teleporte pode falhar sem invalidar restauração de inventário

## Caminho dos Arquivos

```
config/bigbangeventos/snapshots/<player-uuid>/<snapshot-id>.yml
```

Exemplo:
```
config/bigbangeventos/snapshots/550e8400-e29b-41d4-a716-446655440000/a1b2c3d4-5678-90ab-cdef-1234567890ab.yml
```

## Compatibilidade

- Itens vanilla: suporte completo
- Itens com encantamentos: preservados via NBT
- Itens com nome customizado: preservados
- Itens com dano: preservados
- Itens de mods (Cobblemon): preservados se serializáveis via NBT
- Itens com componentes custom data: preservados

## Limitações

- Itens client-only não são capturados
- Dados de mods que usam serialização não-NBT podem ser perdidos
- Slots vazios não são serializados (otimização)
- Efeitos usam `save()`/`load()` nativos do Minecraft

## Riscos

- Atualização do Minecraft que muda formato NBT de itens pode quebrar snapshots existentes
- Mods que armazenam dados em campos não-NBT podem perder dados
- Snapshot de jogador offline não pode ser criado (jogador precisa estar online)

## Como Inspecionar Snapshot

```bash
cat config/bigbangeventos/snapshots/<uuid>/<snapshot-id>.yml
```

O arquivo YAML mostra:
- Estado do snapshot
- Componentes restaurados
- Localização original
- Inventário como strings NBT

## Como Recuperar Manualmente

1. Verificar estado:
```bash
/evento recovery status
/evento debug player <nome>
```

2. Tentar novamente:
```bash
/evento recovery retry <nome>
```

3. Se falhar:
- Verificar logs em `logs/latest.log`
- Procurar por `AUDIT restore_failed player=<uuid>`
- Inspecionar arquivo YAML do snapshot
- Em último caso, restaurar manualmente via backup do inventário
