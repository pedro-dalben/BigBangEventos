# Auditoria da migração Fabric

## Alvo confirmado

`TARGET_LOADER=Fabric`, `TARGET_MINECRAFT=1.21.1`, `TARGET_ENVIRONMENT=dedicated-server`.

## Diagnóstico inicial

O projeto inicial tinha build multi-módulo com `common/` e `neoforge/`. `common/build.gradle` e `neoforge/build.gradle` usavam `net.neoforged.moddev`; `neoforge` continha `NeoForgeEntrypoint`, `NeoForgeEvents` e `META-INF/neoforge.mods.toml`. O comando também estava contaminado por imports Mojmap/NeoForge runtime. Não foram encontrados Mixins, código client-only, `mods.toml`, FML ou dependência BigBangEssentials.

## Código preservado

Todo o domínio, persistência YAML, sessões, participantes, transições, ranking, timers, snapshots abstratos, validações, gatilhos e testes foi movido de `common/src` para `src`. A API pública e os IDs de persistência foram mantidos.

## Código adaptado/removido

O bootstrap foi substituído por `fabric.BigBangEventosMod`; comandos usam `CommandRegistrationCallback`; a interação usa `UseBlockCallback`. O módulo `common`, o módulo `neoforge`, o metadata NeoForge e seus builds foram removidos. O vínculo e a execução básica de placas usam a chave dimensão/posição e a mão principal no servidor.

## Versões encontradas

O log local do Cobbleverse registra Minecraft 1.21.1, Fabric Loader 0.18.4 e Fabric API 0.116.13+1.21.1. Java local é 21.0.5; Gradle Wrapper resolve Gradle 8.14.3. Mappings seguem o padrão usado pelos projetos Fabric locais: Mojang oficiais + Parchment 2024.11.17.

## Limitações

Não há instância de servidor Cobbleverse local acessível, apenas instância cliente/logs e artefatos de projetos Fabric. Portanto a validação contra o modpack completo não pode ser declarada.
