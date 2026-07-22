# Auditoria de Integração: Cobblemon e Fight or Flight

**Versão do documento:** 1.0  
**Data:** Julho 2026  
**Idioma:** Português (Brasil)  
**Fase:** 4.0 — Auditoria Técnica

---

## 1. Versões Instaladas

### 1.1 Ambiente de Desenvolvimento

| Componente | Versão | SHA-256 |
|---|---|---|
| Minecraft | 1.21.1 | — |
| Java | 21 | — |
| Fabric Loader | 0.18.4 | — |
| Fabric API | 0.116.13+1.21.1 | — |
| **Cobblemon (Fabric)** | **1.7.3+1.21.1** | `f7c25955176badc444ad6211fc556514fedbdba776227f105fe899f8819d74e3` |
| **Fight or Flight (Fabric)** | **0.10.9** | `7eee034d17281b43324cbdcc41a3836b6c44a93798d40a608538ed12a72279c1` |
| **Architectury** | **13.0.8** | — |
| **Cloth Config** | **15.0.140** (embutido no JAR do FoF) | — |

### 1.2 Artefatos

| Arquivo | Tamanho | Caminho |
|---|---|---|
| `Cobblemon-fabric-1.7.3+1.21.1.jar` | 135.801.550 bytes | `mods/` da instância COBBLEVERSE |
| `fightorflight-fabric-0.10.9.jar` | 1.424.525 bytes | `mods/` da instância COBBLEVERSE |
| `architectury-13.0.8-fabric.jar` | 600.915 bytes | `mods/` da instância COBBLEVERSE |

### 1.3 Identificadores de Mod

| Mod | Mod ID (fabric.mod.json) | Entrypoint Principal |
|---|---|---|
| Cobblemon | `cobblemon` | `com.cobblemon.mod.fabric.FabricBootstrap` |
| Fight or Flight | `fightorflight` | `me.rufia.fightorflight.CobblemonFightOrFlightFabric` |
| Architectury | `architectury` | — |

### 1.4 Dependências Declaradas

**Cobblemon** depende de: `fabricloader >= 0.17.2`, `fabric-api >= 0.116.6+1.21.1`, `minecraft 1.21.1`, `java 21`.

**Fight or Flight** depende de: `java >= 21`, `minecraft 1.21.1`, `fabricloader >= 0.16.9`, `fabric *`, `cobblemon >= 1.7.2`, `architectury >= 13.0.8`, `cloth-config >= 15.0.140`.

### 1.5 Código Fonte

- **Cobblemon:** repositório em `https://gitlab.com/cable-mc/cobblemon`, tag correspondente à versão 1.7.3+1.21.1.
- **Fight or Flight:** repositório em `https://github.com/rufia/FightOrFlight`, tag correspondente à versão 0.10.9.
- O código fonte não foi inspecionado diretamente; a auditoria foi baseada nas APIs públicas extraídas dos JARs compilados com mapeamentos oficiais do Mojang.

---

## 2. Auditoria da API do Cobblemon

### 2.1 Classes Principais

| Classe | Caminho | Função |
|---|---|---|
| `PokemonEntity` | `com.cobblemon.mod.common.entity.pokemon.PokemonEntity` | Entidade viva no mundo (estende `Mob`) |
| `Pokemon` | `com.cobblemon.mod.common.pokemon.Pokemon` | Modelo de dados do Pokémon (UUID, espécie, forma, nível, stats, NBT) |
| `Cobblemon` | `com.cobblemon.mod.common.Cobblemon` | Singleton principal (`Cobblemon.INSTANCE`) |
| `PartyStore` | `com.cobblemon.mod.common.api.storage.party.PartyStore` | Armazenamento do time do jogador |
| `PCStore` | `com.cobblemon.mod.common.api.storage.pc.PCStore` | Armazenamento do PC do jogador |
| `PokemonStore<T>` | `com.cobblemon.mod.common.api.storage.PokemonStore` | Contrato genérico de armazenamento |
| `CobblemonEvents` | `com.cobblemon.mod.common.api.events.CobblemonEvents` | Registro central de eventos (contém `SimpleEventHandler<T>` como campos estáticos) |
| `Species` | `com.cobblemon.mod.common.api.pokemon.PokemonSpecies` (ou via `Pokemon.getSpecies()`) | Espécie do Pokémon |
| `PokemonProperties` | `com.cobblemon.mod.common.api.pokemon.PokemonProperties` | Propriedades para spawn/filtro |

### 2.2 Como Identificar um `PokemonEntity`

- `PokemonEntity` estende `net.minecraft.world.entity.Mob`.
- Obtido via eventos do Cobblemon (ex: `PokemonSentEvent.getPokemonEntity()`).
- Pode ser obtido do mundo via `level.getEntities()` filtrando por tipo `PokemonEntity.class`.
- Cada `PokemonEntity` contém uma referência ao modelo `Pokemon`.

### 2.3 Métodos da Interface `PokemonEntity`

| Método | Retorno | Descrição |
|---|---|---|
| `getPokemon()` | `Pokemon` | Obtém o modelo Pokémon subjacente |
| `getOwnerUUID()` | `UUID?` | UUID do treinador dono (null para selvagem) |
| `getOwner()` | `LivingEntity?` | Entidade do treinador dono |
| `isWild()` | `boolean` | Se o Pokémon é selvagem |
| `getBehaviour()` | ... | Flags de comportamento |
| `getServerDelegate()` | `PokemonServerDelegate` | Delegate de servidor |

### 2.4 Métodos do Modelo `Pokemon`

| Método | Retorno | Descrição |
|---|---|---|
| `getUuid()` | `UUID` | UUID persistente do Pokémon |
| `getSpecies()` | `Species` | Espécie |
| `getForm()` | `Form` | Forma |
| `getLevel()` | `int` | Nível |
| `getShiny()` / `isShiny()` | `boolean` | Se é shiny |
| `getCurrentHealth()` | `int` | HP atual |
| `getMaxHealth()` | `int` | HP máximo |
| `getIvs()` | `Map<Stat, Integer>` | IVs |
| `getCaughtBall()` | `PokeBall` | Pokébola de captura |
| `getPersistentData()` | `CompoundTag` | Dados persistentes customizados |
| `getStatus()` | `Status?` | Condição de status (null = sem status) |
| `isFainted()` | `boolean` | Se está desmaiado |
| `getHeldItem()` | `ItemStack` | Item segurado |
| `saveToNBT(registry, tag)` | — | Serializa para NBT |
| `Companion.loadFromNBT(registry, tag)` | `Pokemon` | Desserializa de NBT |
| `getAspects()` | `Collection<Aspect>` | Aspectos (tamanho, etc.) |

### 2.5 Métodos do `PartyStore`

| Método | Retorno | Descrição |
|---|---|---|
| `get(uuid)` | `Pokemon?` | Busca Pokémon por UUID |
| `get(slot)` | `Pokemon?` | Busca Pokémon por slot (0-5) |
| `size()` | `int` | Quantidade de Pokémon no time |
| `add(pokemon)` | — | Adiciona Pokémon ao time |
| `remove(pokemon)` | — | Remove Pokémon do time |
| `iterator()` | `Iterator<Pokemon>` | Itera sobre o time |

### 2.6 Eventos Cobblemon Relevantes

| Evento | Caminho | Pré/Pós | Cancelável |
|---|---|---|---|
| **PokemonSentEvent** | `api.events.pokemon.PokemonSentEvent` | Pre, Post | Pre é cancelável |
| **PokemonRecallEvent** | `api.events.pokemon.PokemonRecallEvent` | Pre, Post | Pre é cancelável |
| **PokemonFaintedEvent** | `api.events.pokemon.PokemonFaintedEvent` | — | Não |
| **BattleStartedEvent** | `api.events.battles.BattleStartedEvent` | Pre, Post | Pre é cancelável |
| **BattleVictoryEvent** | `api.events.battles.BattleVictoryEvent` | — | Não |
| **BattleFledEvent** | `api.events.battles.BattleFledEvent` | — | Não |
| **PokemonCapturedEvent** | `api.events.pokemon.PokemonCapturedEvent` | — | Não |
| **ExperienceGainedEvent** | `api.events.pokemon.ExperienceGainedEvent` | Pre, Post | Pre é cancelável |
| **EvGainedEvent** | `api.events.pokemon.EvGainedEvent` | Pre, Post | Pre é cancelável |
| **EvolutionAcceptedEvent** | `api.events.pokemon.evolution.EvolutionAcceptedEvent` | — | Não |
| **EvolutionCompleteEvent** | `api.events.pokemon.evolution.EvolutionCompleteEvent` | — | Não |
| **LootDroppedEvent** | `api.events.drops.LootDroppedEvent` | — | Não |
| **LevelUpEvent** | `api.events.pokemon.LevelUpEvent` | — | Não |
| **HeldItemEvent** | `api.events.pokemon.HeldItemEvent` | Pre, Post | Pre é cancelável |
| **PokemonHealedEvent** | `api.events.pokemon.healing.PokemonHealedEvent` | — | Não |
| **PokemonEntityLoadEvent** | `api.events.entity.PokemonEntityLoadEvent` | — | Não |
| **HatchEggEvent** | `api.events.pokemon.HatchEggEvent` | Pre, Post | — |
| **TradeEvent** | `api.events.pokemon.TradeEvent` | Pre, Post | — |

### 2.7 Batalha Tradicional

- `PokemonBattle` contém a instância da batalha.
- `BattleActor` representa um participante na batalha.
- `ActorType` (enum): tipo do ator (player, wild, trainer, etc.).
- Para verificar se um Pokémon está em batalha: verificar se `pokemon.getBattle()` não é null (método a confirmar no código fonte).
- `BattleStartedEvent.Pre` é cancelável — pode ser usado para bloquear início de batalhas durante o evento.

### 2.8 Fluxo de Envio e Recall

1. **Envio:** O jogador seleciona um Pokémon → `PokemonSentEvent.Pre` dispara (cancelável) → entidade `PokemonEntity` é spawnada no mundo → `PokemonSentEvent.Post` dispara.
2. **Recall:** O Pokémon é recolhido → `PokemonRecallEvent.Pre` dispara (cancelável) → entidade é removida do mundo → `PokemonRecallEvent.Post` dispara.

### 2.9 Fluxo de Desmaio

1. `PokemonEntity` recebe dano fatal → `PokemonFaintedEvent` dispara → entidade pode ser removida ou permanecer no mundo dependendo da configuração.
2. O dano que causa o faint pode vir de várias fontes: ataque de entidade, dano ambiental, etc.

---

## 3. Auditoria do Fight or Flight

### 3.1 Classes Principais

| Classe | Caminho | Função |
|---|---|---|
| `CobblemonFightOrFlight` | `me.rufia.fightorflight.CobblemonFightOrFlight` | Singleton principal do mod |
| `PokemonInterface` | `me.rufia.fightorflight.PokemonInterface` | Interface injetada via Mixin em `PokemonEntity` |
| `EntityFightOrFlight` | `me.rufia.fightorflight.entity.EntityFightOrFlight` | Dados de combate por entidade |

### 3.2 Sistema de Alvos (Targeting)

| Classe | Caminho | Função |
|---|---|---|
| `PokemonCommandedTargetGoal` | `goals.targeting.PokemonCommandedTargetGoal` | Alvo comandado pelo treinador |
| `PokemonNearestAttackableTargetGoal` | `goals.targeting.PokemonNearestAttackableTargetGoal` | Alvo hostil mais próximo |
| `PokemonOwnerHurtByTargetGoal` | `goals.targeting.PokemonOwnerHurtByTargetGoal` | Alvo que feriu o dono |
| `PokemonOwnerHurtTargetGoal` | `goals.targeting.PokemonOwnerHurtTargetGoal` | Alvo que o dono está atacando |
| `PokemonProactiveTargetGoal` | `goals.targeting.PokemonProactiveTargetGoal` | Alvo proativo (selvagem agressivo) |
| `PokemonTauntedTargetGoal` | `goals.targeting.PokemonTauntedTargetGoal` | Alvo de provocação |
| `CaughtByTargetGoal` | `goals.targeting.CaughtByTargetGoal` | Alvo de captura |

### 3.3 Sistema de Ataque

| Classe | Caminho | Função |
|---|---|---|
| `PokemonAttackGoal` | `goals.PokemonAttackGoal` | Goal principal de ataque |
| `PokemonAttackPosGoal` | `goals.PokemonAttackPosGoal` | Ataque a posição |
| `FOFPokemonAttackTask` | `entity.ai.tasks.FOFPokemonAttackTask` | Task de ataque (sistema de brain) |
| `FOFPokemonMeleeTask` | `entity.ai.tasks.FOFPokemonMeleeTask` | Task de ataque corpo a corpo |
| `FOFPokemonRangeTask` | `entity.ai.tasks.FOFPokemonRangeTask` | Task de ataque à distância |
| `FOFMoveToAttackTargetTask` | `entity.ai.tasks.FOFMoveToAttackTargetTask` | Task de movimento ao alvo |
| `FOFDefendOwnerTask` | `entity.ai.tasks.FOFDefendOwnerTask` | Task de defesa do dono |
| `FOFDefendSelfTask` | `entity.ai.tasks.FOFDefendSelfTask` | Task de autodefesa |
| `FOFFleeFromAttackerTask` | `entity.ai.tasks.FOFFleeFromAttackerTask` | Task de fuga |

### 3.4 Projéteis

| Classe | Caminho | Tipo |
|---|---|---|
| `AbstractPokemonProjectile` | `entity.projectile.AbstractPokemonProjectile` | Projétil base |
| `PokemonArrow` | `entity.projectile.PokemonArrow` | Flecha |
| `PokemonBullet` | `entity.projectile.PokemonBullet` | Projétil balístico |
| `PokemonTracingBullet` | `entity.projectile.PokemonTracingBullet` | Projétil teleguiado |
| `AbstractPokemonSpike` | `entity.projectile.AbstractPokemonSpike` | Espinho base |
| `PokemonSpike` | `entity.projectile.PokemonSpike` | Espinho |
| `PokemonFloatingSpike` | `entity.projectile.PokemonFloatingSpike` | Espinho flutuante |
| `PokemonStickyWeb` | `entity.projectile.PokemonStickyWeb` | Teia |
| `ExplosivePokemonProjectile` | `entity.projectile.ExplosivePokemonProjectile` | Projétil explosivo |

### 3.5 Efeitos de Área

| Classe | Caminho | Função |
|---|---|---|
| `AbstractPokemonAreaEffect` | `entity.areaeffect.AbstractPokemonAreaEffect` | Efeito de área base |
| `PokemonAreaEffectMagic` | `entity.areaeffect.PokemonAreaEffectMagic` | Magia/psíquico |
| `PokemonTornado` | `entity.areaeffect.PokemonTornado` | Tornado |
| `PokemonWhirlPool` | `entity.areaeffect.PokemonWhirlPool` | Redemoinho |

### 3.6 Mixins

| Mixin | Caminho | Propósito |
|---|---|---|
| `PokemonEntityMixin` | `mixin.PokemonEntityMixin` | Injeta `PokemonInterface` no `PokemonEntity` |
| `PokemonBrainMixin` | `mixin.PokemonBrainMixin` | Modifica o brain do Pokémon |
| `PokemonServerDelegateMixin` | `mixin.PokemonServerDelegateMixin` | Modifica delegate do servidor |
| `MeleeAttackTaskMixin` | `mixin.MeleeAttackTaskMixin` | Modifica ataque corpo a corpo |
| `MoveToAttackTargetTaskMixin` | `mixin.MoveToAttackTargetTaskMixin` | Modifica movimento ao alvo |
| `DefendOwnerTaskMixin` | `mixin.DefendOwnerTaskMixin` | Modifica defesa do dono |
| `LivingEntityMixin` | `mixin.LivingEntityMixin` | Injeta lógica de dano em entidades vivas |
| `EmptyPokeBallEntityMixin` | `mixin.EmptyPokeBallEntityMixin` | Intercepta pokébolas |
| `EnderDragonMixin` | `mixin.EnderDragonMixin` | Compatibilidade com Ender Dragon |

### 3.7 Utilitários de Dano

| Classe | Função |
|---|---|
| `FOFMove` | Execução de movimento (dano, efeitos) |
| `FOFUtils` | Utilitários gerais |
| `PokemonUtils` | Utilitários de Pokémon |
| `FOFExpCalculator` | Cálculo de experiência |
| `FOFEVCalculator` | Cálculo de EV |
| `FOFAggressionCalculator` | Cálculo de agressividade |
| `FOFExplosion` | Explosão customizada |
| `TypeEffectiveness` | Efetividade de tipo |
| `TargetingWhitelist` | Lista branca de alvos |
| `RayTrace` | Ray tracing para ataques |
| `FOFHeldItemManager` | Gerenciador de itens segurados |

### 3.8 Configuração

| Classe | Caminho |
|---|---|
| `FightOrFlightCommonConfigModel` | `config.FightOrFlightCommonConfigModel` |
| `FightOrFlightMoveConfigModel` | `config.FightOrFlightMoveConfigModel` |
| `FightOrFlightVisualEffectConfigModel` | `config.FightOrFlightVisualEffectConfigModel` |

Arquivos de configuração: `config/fightorflight.json5`, `config/fightorflight_moves.json5`, `config/fightorflight_visual_effect.json5`.

---

## 4. Matriz de Capacidades

### 4.1 Cobblemon

| Capacidade | API Pública | Compilação Direta | Mixin Necessário | Não Disponível |
|---|---|---|---|---|
| Resolver entidade Pokémon | Sim (`PokemonEntity`) | Sim | Não | — |
| Resolver dono/treinador | Sim (`getOwnerUUID()`) | Sim | Não | — |
| Resolver UUID persistente | Sim (`Pokemon.getUuid()`) | Sim | Não | — |
| Resolver espécie/forma/nível | Sim | Sim | Não | — |
| Resolver HP atual/máximo | Sim | Sim | Não | — |
| Resolver estado de faint | Sim (`isFainted()`) | Sim | Não | — |
| Resolver status condition | Sim (`getStatus()`) | Sim | Não | — |
| Listar Pokémon ativos (time) | Sim (`PartyStore`) | Sim | Não | — |
| Listar Pokémon ativos (PC) | Sim (`PCStore`) | Sim | Não | — |
| Enviar Pokémon (send-out) | API não exposta diretamente | Talvez via comandos internos | Possível | A verificar |
| Recolher Pokémon (recall) | Sim (`PokemonRecallEvent`) | Via evento | Não | — |
| Capturar estado do Pokémon | Sim (`saveToNBT`) | Sim | Não | — |
| Restaurar estado do Pokémon | Sim (`loadFromNBT`) | Sim | Não | — |
| Obter slot do time | Sim (`PartyStore.get(slot)`) | Sim | Não | — |
| Verificar batalha tradicional | Sim (verificar `getBattle()`) | Sim | Não | — |
| Bloquear início de batalha | Sim (`BattleStartedEvent.Pre`) | Sim | Não | — |
| Observar faint | Sim (`PokemonFaintedEvent`) | Sim | Não | — |
| Observar envio | Sim (`PokemonSentEvent`) | Sim | Não | — |
| Observar recall | Sim (`PokemonRecallEvent`) | Sim | Não | — |
| Cancelar envio | Sim (`PokemonSentEvent.Pre`) | Sim | Não | — |
| Cancelar recall | Sim (`PokemonRecallEvent.Pre`) | Sim | Não | — |

### 4.2 Fight or Flight

| Capacidade | API Pública | Compilação Direta | Mixin Necessário | Não Disponível |
|---|---|---|---|---|
| Resolver dono do Pokémon | Sim (via Cobblemon) | Indireto | Não | — |
| Resolver alvo atual | Limitado (via brain/entity data) | Não | Sim (PokemonInterface) | — |
| Limpar alvo | Não diretamente | Não | Sim | Parcial |
| Resolver dono do projétil | Sim (via `AbstractPokemonProjectile`) | Sim | Não | — |
| Bloquear alvo inválido | Não diretamente | Não | Sim | Parcial |
| Suprimir XP (experiência) | Sim (`ExperienceGainedEvent.Pre`) | Sim | Não | — |
| Suprimir EV | Sim (`EvGainedEvent.Pre`) | Sim | Não | — |
| Suprimir evolução | Parcial (`EvolutionAcceptedEvent`) | Sim | Não | — |
| Detectar ataque corpo a corpo | Sim (via dano da entidade) | Sim | Não | — |
| Detectar ataque à distância | Sim (via projétil) | Sim | Não | — |
| Atribuir dano ao treinador | Indireto (via dono do Pokémon) | Sim | Não | — |
| Prevenir dano a espectadores | Implementação própria | Sim | Não | — |
| Validar alvo antes do ataque | Limitado | Parcial | Parcial | — |
| Bloquear Pokémon selvagens | Parcial (via targeting) | Não | Parcial | — |
| Prevenir ataques durante pausa | Implementação própria | Sim | Não | — |

### 4.3 Resumo de Riscos

| Risco | Severidade | Mitigação |
|---|---|---|
| Fight or Flight usa Mixins em `PokemonEntity` — possível conflito | Média | Usar eventos Cobblemon quando possível, testar compatibilidade |
| Limpar alvo do FoF requer acesso ao brain/entity data | Alta | Usar `PokemonInterface` injetado pelo Mixin, com fallback |
| AT_TEAM_SPAWN do respawn não usa localização do time | Baixa | Implementar no PokéGladiator |
| Não há API pública para "recall all Pokémon" | Média | Iterar `PartyStore` e chamar recall via evento ou comando |
| Projéteis podem impactar após fim da rodada | Média | Sistema de expiração de atribuição, limpar no cleanup |

---

## 5. Fluxo de Dano

### 5.1 Dano Corpo a Corpo

```
PokemonEntity (atacante)
  → FOFPokemonMeleeTask / FOFPokemonAttackTask
    → PokemonAttackGoal
      → FOFMove.applyDamage(target)
        → target.hurt(damageSource, amount)
          → FabricCombatEvents.ALLOW_DAMAGE
            → CombatEvents.ParticipantDamaged (BigBangEventos)
```

### 5.2 Dano à Distância

```
PokemonEntity (atacante)
  → FOFPokemonRangeTask
    → AbstractPokemonProjectile (spawn)
      → Projectile.impact(target)
        → target.hurt(damageSource, amount)
          → FabricCombatEvents.ALLOW_DAMAGE
            → CombatEvents.ParticipantDamaged (BigBangEventos)
```

### 5.3 Fluxo de Atribuição (Proposto)

```
Dano detectado → resolver atacante:
  1. Se entidade atacante é Player → atribuir ao Player
  2. Se entidade atacante é PokemonEntity → resolver dono via getOwnerUUID()
     → atribuir ao Player dono (se em sessão)
  3. Se dano veio de projétil → resolver dono do projétil
     → resolver dono do Pokémon fonte → atribuir ao Player dono
  4. Se dano ambiental → atribuir como ENVIRONMENT
```

---

## 6. Recomendações de Implementação

### 6.1 Cobblemon Integration

1. **Provider de combate:** Criar `CobblemonCombatProvider` que implementa `CombatProvider` do Core.
2. **Registro de `PokemonEntity`:** Usar `PokemonSentEvent.Post` para registrar e `PokemonRecallEvent.Post` para remover.
3. **Faint:** Usar `PokemonFaintedEvent` para detectar faint e aplicar política de perda de vida.
4. **Snapshot:** Usar `Pokemon.saveToNBT()` / `Pokemon.loadFromNBT()` para capturar e restaurar.
5. **Bloqueio de batalha:** Usar `BattleStartedEvent.Pre` cancelável para prevenir batalhas tradicionais durante o evento.
6. **Recall:** Para recolher um Pokémon, verificar se há API pública de recall. Alternativa: remover a `PokemonEntity` do mundo e restaurar o `Pokemon` ao `PartyStore`.

### 6.2 Fight or Flight Integration

1. **Provider de combate:** Criar `FightOrFlightCombatProvider` que implementa `CombatProvider`.
2. **Firewall de alvos:** Interceptar os target goals do FoF para validar alvos antes do ataque.
3. **Atribuição de projéteis:** Mapear `AbstractPokemonProjectile` → `PokemonEntity` fonte → Player dono.
4. **Limpeza de alvos:** Via brain da entidade ou `PokemonInterface` do Mixin do FoF.
5. **Supressão de progressão:** Usar eventos canceláveis do Cobblemon (`ExperienceGainedEvent.Pre`, `EvGainedEvent.Pre`).

### 6.3 Funcionalidades Não Seguras ou Não Suportadas

| Funcionalidade | Status | Justificativa |
|---|---|---|
| Auto send-out de Pokémon | Não suportado | API de envio programático não encontrada. Requer investigação adicional. |
| Limpar alvo do Fight or Flight sem Mixin | Parcialmente suportado | Requer acesso ao brain via `PokemonInterface` injetado pelo Mixin do FoF. |
| Suprimir todos os tipos de projétil | Parcialmente suportado | Projéteis de área (`AbstractPokemonAreaEffect`) podem ter atribuição complexa. |
| Seis Pokémon simultâneos | Fora do escopo | MVP suporta apenas 1 Pokémon por treinador. |
| Regras VGC | Fora do escopo | Não implementado nesta fase. |
| GUI de registro | Fora do escopo | Usar comandos para registro. |

---

## 7. Conclusão da Auditoria

**Cobblemon 1.7.3+1.21.1** expõe APIs públicas suficientes para:
- Identificar Pokémon, donos, estado de faint e batalha.
- Observar eventos de envio, recall e faint.
- Bloquear batalhas tradicionais.
- Capturar e restaurar estado do Pokémon.
- Suprimir XP, EV e evolução via eventos canceláveis.

**Fight or Flight 0.10.9** fornece capacidade de combate overworld mas:
- A API pública é limitada; a maior parte da lógica está em brain tasks e Mixins.
- O acesso ao alvo atual requer introspecção do brain ou uso do `PokemonInterface` injetado.
- A limpeza de alvos é possível mas requer implementação cuidadosa.

**Recomendação geral:** Implementar o Core de atores de combate genéricos, depois os providers Cobblemon e Fight or Flight, e por fim o PokéGladiator. Quando uma capacidade não puder ser implementada com segurança, documentar a limitação e continuar com o subconjunto funcional.

---

**Fim da auditoria.**
