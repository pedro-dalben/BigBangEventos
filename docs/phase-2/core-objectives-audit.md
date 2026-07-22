# Auditoria do Core — Fase 2

O Core já possui engine, lifecycle, participantes, triggers, áreas, ranking, YAML, snapshots, restore e SDK Fabric. `typeSettings` continua sendo o storage legado de módulos.

Esta fase adiciona `ObjectiveDefinition`, `ObjectiveProgress`, `ObjectiveService`, `EventStageDefinition`, `StageService`, typed data e `DomainEventBus`. O Parkour continua usando suas próprias APIs e não é migrado.

`PARTICIPANT` e `SESSION` são suportados; `TEAM` é reservado e falha na validação. Definições e sessões usam schema 2, com defaults para schema 1. O recovery preserva progresso, marca sessões incompletas como `FAILED` e não retoma etapas.

Snapshots completos, confirmação assíncrona de teleporte, Cobblemon, dois jogadores reais, reinício real e staging continuam no backlog `pre-production-hardening`.
