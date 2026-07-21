# Parkour MVP — UAT (User Acceptance Testing)

Status: Draft (homologacao pendente de execucao no staging)
Branch: feat/parkour-runtime-uat

---

## Pre-requisitos Manuais

Antes da homologacao, o administrador deve:

1. Construir a arena de parkour no mundo
2. Criar regiao administrativa no BigBangRegions
3. Configurar flags manualmente
4. Confirmar protecao da arena
5. Somente depois configurar o evento Parkour

### Checklist de Protecao (BigBangRegions)

```
[ ] Regiao administrativa criada
[ ] Quebra bloqueada
[ ] Colocacao bloqueada
[ ] PvP bloqueado
[ ] Explosoes bloqueadas
[ ] Agua/lava bloqueadas (conforme necessario)
[ ] Pistoes bloqueados
[ ] Mob griefing bloqueado
[ ] Interacao com placas permitida
```

Nota: Consulte os comandos reais do BigBangRegions para cada flag.
O BigBangEventos nao executa nenhuma chamada ao BigBangRegions.

---

## UAT-01: Instalacao

| Campo | Valor |
|-------|-------|
| ID | UAT-01 |
| Descricao | Instalar Core e modulo Parkour no staging Cobbleverse |
| Pre-condicoes | Servidor Fabric 1.21.1, Java 21, BigBangRegions instalado |
| Passos | 1. Backup do servidor<br>2. Confirmar Java 21<br>3. Copiar JARs para mods/<br>4. Iniciar servidor<br>5. Verificar logs |
| Resultado esperado | Modulos carregados sem erros de Mixin, mappings ou classes |
| Resultado real | NOT_RUN |
| Evidencia | |
| Logs | |
| Bug | |
| Commit | |

---

## UAT-02: Protecao Manual da Arena

| Campo | Valor |
|-------|-------|
| ID | UAT-02 |
| Descricao | Criar regiao protetora e configurar flags |
| Pre-condicoes | UAT-01 PASS, arena construida |
| Passos | 1. Criar regiao no BigBangRegions<br>2. Configurar flags<br>3. Testar quebra/colocacao/PvP como jogador comum<br>4. Testar placas utilizaveis |
| Resultado esperado | Arena protegida, placas funcionais |
| Resultado real | NOT_RUN |
| Evidencia | |
| Logs | |
| Bug | |
| Commit | |

---

## UAT-03: Criacao do Evento Parkour

| Campo | Valor |
|-------|-------|
| ID | UAT-03 |
| Descricao | Criar e configurar evento parkour completo |
| Pre-condicoes | UAT-02 PASS |
| Passos | 1. `/evento create parkour_uat parkour`<br>2. `/evento edit parkour_uat`<br>3. `/evento set lobby`, `entrance`, `exit`<br>4. `/evento parkour set-start`, `set-finish`<br>5. `/evento parkour set-fall-y 40`<br>6. `/evento parkour checkpoint add cp1`<br>7. `/evento parkour checkpoint add cp2`<br>8. `/evento parkour validate`<br>9. `/evento parkour info`<br>10. Reiniciar servidor<br>11. Verificar persistencia |
| Resultado esperado | Evento criado, configuracao persiste apos restart |
| Resultado real | NOT_RUN |
| Evidencia | |
| Logs | |
| Bug | |
| Commit | |

---

## UAT-04: Validacao de Configuracao

| Campo | Valor |
|-------|-------|
| ID | UAT-04 |
| Descricao | Testar erros de configuracao |
| Pre-condicoes | UAT-03 PASS |
| Passos | 1. Criar evento sem lobby → validacao falha<br>2. Sem largada → validacao falha<br>3. Sem chegada → validacao falha<br>4. Sem checkpoints com checkpoints obrigatorios → validacao falha<br>5. Checkpoint com raio invalido → erro<br>6. Max-time negativo → validacao falha<br>7. Max-attempts negativo → validacao falha |
| Resultado esperado | Validacao bloqueia configuracoes invalidas |
| Resultado real | NOT_RUN |
| Evidencia | |
| Logs | |
| Bug | |
| Commit | |

---

## UAT-05: Fluxo com Um Jogador

| Campo | Valor |
|-------|-------|
| ID | UAT-05 |
| Descricao | Jogador entra, corre parkour, completa |
| Pre-condicoes | UAT-03 PASS, inventario conhecido preparado |
| Passos | 1. Jogador entra com inventario preparado<br>2. `/evento entrar parkour_uat`<br>3. Snapshot criado, jogador vai ao lobby<br>4. Staff abre e inicia evento<br>5. Jogador teleportado a largada<br>6. Cronometro inicia<br>7. Completa CP1 (entrar na area)<br>8. Completa CP2 (entrar na area)<br>9. Cai → retorna ao ultimo CP<br>10. Cronometro continua<br>11. Chega ao final<br>12. Ranking atualizado<br>13. Jogador restaurado<br>14. Teleportado a saida |
| Resultado esperado | Fluxo completo, tempo correto, ranking exibido, itens restaurados |
| Resultado real | NOT_RUN |
| Evidencia | |
| Logs | |
| Bug | |
| Commit | |

---

## UAT-06: Checkpoint por Placa (SIGN_INTERACT)

| Campo | Valor |
|-------|-------|
| ID | UAT-06 |
| Descricao | Checkpoint ativado ao clicar placa |
| Pre-condicoes | UAT-03 PASS |
| Passos | 1. Criar trigger SIGN_INTERACT com nome do checkpoint: `/evento trigger create cp1_plate SIGN_INTERACT`<br>2. Vincular a placa: `/evento trigger bind cp1_plate` + clicar placa<br>3. Iniciar evento com jogador<br>4. Jogador clica na placa<br>5. Checkpoint completado<br>6. Testar clique duplicado<br>7. Testar checkpoint fora de ordem |
| Resultado esperado | Checkpoint so avanca na ordem correta, sem duplicacao |
| Resultado real | NOT_RUN |
| Evidencia | |
| Logs | |
| Bug | |
| Commit | |

---

## UAT-07: Checkpoint por REGION_ENTER

| Campo | Valor |
|-------|-------|
| ID | UAT-07 |
| Descricao | Checkpoint invisivel ativado ao entrar em area |
| Pre-condicoes | UAT-03 PASS |
| Passos | 1. Criar trigger REGION_ENTER com nome do checkpoint: `/evento trigger create cp1_region REGION_ENTER`<br>2. Configurar area do trigger (via API)<br>3. Iniciar evento com jogador<br>4. Jogador entra na area definida<br>5. Checkpoint completado<br>6. Testar multiplas entradas na area<br>7. Testar saida e reentrada |
| Resultado esperado | Checkpoint ativa OUTSIDE→INSIDE, nao ativa enquanto dentro, nao duplica |
| Resultado real | NOT_RUN |
| Evidencia | |
| Logs | |
| Bug | |
| Commit | |

---

## UAT-08: Chegada por Placa (SIGN_INTERACT)

| Campo | Valor |
|-------|-------|
| ID | UAT-08 |
| Descricao | Finalizar parkour ao clicar placa de chegada |
| Pre-condicoes | UAT-03 PASS |
| Passos | 1. Criar trigger SIGN_INTERACT com nome "finish": `/evento trigger create finish SIGN_INTERACT`<br>2. Vincular a placa de chegada<br>3. Jogador completa todos checkpoints<br>4. Jogador clica na placa de chegada<br>5. Tempo registrado, ranking atualizado, jogador restaurado |
| Resultado esperado | Conclusao idempotente, tempo correto |
| Resultado real | NOT_RUN |
| Evidencia | |
| Logs | |
| Bug | |
| Commit | |

---

## UAT-09: Chegada por REGION_ENTER

| Campo | Valor |
|-------|-------|
| ID | UAT-09 |
| Descricao | Finalizar parkour ao entrar na area de chegada |
| Pre-condicoes | UAT-03 PASS |
| Passos | 1. Criar trigger REGION_ENTER com nome "finish"<br>2. Configurar area de chegada<br>3. Jogador completa todos checkpoints<br>4. Jogador entra na area de chegada<br>5. Conclui, tempo registrado, ranking atualizado |
| Resultado esperado | Semelhante ao UAT-08, via area em vez de placa |
| Resultado real | NOT_RUN |
| Evidencia | |
| Logs | |
| Bug | |
| Commit | |

---

## UAT-10: Checkpoint Fora de Ordem

| Campo | Valor |
|-------|-------|
| ID | UAT-10 |
| Descricao | Tentar completar checkpoint fora de ordem |
| Pre-condicoes | UAT-03 PASS |
| Passos | 1. Jogador vai direto ao CP3 sem completar CP1 e CP2<br>2. Jogador tenta completar CP2 sem CP1<br>3. Jogador repete CP1 |
| Resultado esperado | Apenas o proximo checkpoint e aceito. Mensagem informa checkpoint atual. Sem duplicacao. |
| Resultado real | NOT_RUN |
| Evidencia | |
| Logs | |
| Bug | |
| Commit | |

---

## UAT-11: Chegada Antecipada

| Campo | Valor |
|-------|-------|
| ID | UAT-11 |
| Descricao | Tentar chegar sem completar checkpoints |
| Pre-condicoes | UAT-03 PASS, checkpoints obrigatorios |
| Passos | 1. Jogador vai direto a chegada sem completar checkpoints |
| Resultado esperado | Nao conclui. Nao registra tempo. Nao entra no ranking. Mensagem informa checkpoints faltantes. |
| Resultado real | NOT_RUN |
| Evidencia | |
| Logs | |
| Bug | |
| Commit | |

---

## UAT-12: Conclusao Concorrente

| Campo | Valor |
|-------|-------|
| ID | UAT-12 |
| Descricao | Impedir conclusao duplicada |
| Pre-condicoes | UAT-03 PASS |
| Passos | 1. Chegada com placa + REGION_ENTER simultaneos<br>2. Reentrada na regiao final |
| Resultado esperado | Uma unica conclusao, um unico tempo, uma unica posicao, uma unica restauracao |
| Resultado real | NOT_RUN |
| Evidencia | |
| Logs | |
| Bug | |
| Commit | |

---

## UAT-13: Dois Jogadores

| Campo | Valor |
|-------|-------|
| ID | UAT-13 |
| Descricao | Dois jogadores correndo independentemente |
| Pre-condicoes | UAT-03 PASS |
| Passos | 1. Ambos entram simultaneamente<br>2. Evento inicia<br>3. Checkpoints independentes<br>4. Um jogador cai enquanto outro avanca<br>5. Ranking mostra ambos corretamente |
| Resultado esperado | Progresso de um nao altera o outro. Ranking correto. |
| Resultado real | NOT_RUN |
| Evidencia | |
| Logs | |
| Bug | |
| Commit | |

---

## UAT-14: Desconexao Dentro do Prazo

| Campo | Valor |
|-------|-------|
| ID | UAT-14 |
| Descricao | Jogador desconecta e reconecta dentro do grace period |
| Pre-condicoes | UAT-03 PASS, grace period 120s |
| Passos | 1. Jogador entra no evento<br>2. Evento inicia<br>3. Jogador desconecta<br>4. Estado DISCONNECTED<br>5. Reconecta dentro de 120s<br>6. Volta para participacao ativa<br>7. Cronometro nao reinicia |
| Resultado esperado | Jogador retorna ao evento, cronometro preservado |
| Resultado real | NOT_RUN |
| Evidencia | |
| Logs | |
| Bug | |
| Commit | |

---

## UAT-15: Desconexao Prazo Expirado

| Campo | Valor |
|-------|-------|
| ID | UAT-15 |
| Descricao | Jogador desconecta e grace period expira |
| Pre-condicoes | UAT-03 PASS |
| Passos | 1. Jogador desconecta<br>2. Grace period expira (>120s)<br>3. Jogador DISQUALIFIED<br>4. Snapshot fica pendente<br>5. Jogador reconecta<br>6. Snapshot restaurado<br>7. Nao retorna ao evento |
| Resultado esperado | Restauracao ocorre, jogador fora do evento |
| Resultado real | NOT_RUN |
| Evidencia | |
| Logs | |
| Bug | |
| Commit | |

---

## UAT-16: Saida Voluntaria

| Campo | Valor |
|-------|-------|
| ID | UAT-16 |
| Descricao | Jogador sai do evento voluntariamente |
| Pre-condicoes | UAT-03 PASS |
| Passos | 1. Jogador entra<br>2. `/evento sair`<br>3. Restauracao ocorre<br>4. Teleporte a saida/original<br>5. Participacao removida<br>6. Repetir comando nao duplica |
| Resultado esperado | Idempotente, sem duplicacao de itens |
| Resultado real | NOT_RUN |
| Evidencia | |
| Logs | |
| Bug | |
| Commit | |

---

## UAT-17: Cancelamento pelo Staff

| Campo | Valor |
|-------|-------|
| ID | UAT-17 |
| Descricao | Staff cancela evento com dois jogadores |
| Pre-condicoes | UAT-03 PASS |
| Passos | 1. Evento em RUNNING com 2 jogadores<br>2. Staff cancela: `/evento cancel parkour_uat`<br>3. Cronometros encerrados<br>4. Ambos restaurados<br>5. Sessao CANCELLED<br>6. Segundo cancelamento idempotente |
| Resultado esperado | Ambos restaurados, sem recompensas, idempotente |
| Resultado real | NOT_RUN |
| Evidencia | |
| Logs | |
| Bug | |
| Commit | |

---

## UAT-18: Reinicio Normal do Servidor

| Campo | Valor |
|-------|-------|
| ID | UAT-18 |
| Descricao | Servidor reinicia com evento ativo |
| Pre-condicoes | UAT-03 PASS |
| Passos | 1. Jogador entra, snapshot persistido<br>2. Evento inicia<br>3. `/stop` no servidor<br>4. Iniciar servidor<br>5. Sessao marcada FAILED<br>6. Jogador reconecta<br>7. Snapshot restaurado<br>8. Novo `/evento sair` nao restaura novamente |
| Resultado esperado | Restauracao unica, sem duplicacao |
| Resultado real | NOT_RUN |
| Evidencia | |
| Logs | |
| Bug | |
| Commit | |

---

## UAT-19: Snapshot com Itens Modded (Cobblemon)

| Campo | Valor |
|-------|-------|
| ID | UAT-19 |
| Descricao | Validar snapshot e restauracao com itens do Cobblemon |
| Pre-condicoes | Cobblemon instalado no staging |
| Passos | 1. Preparar inventario: item vanilla, Cobblemon, Poke Bola, item com componentes, item encantado, item nomeado, item danificado, stack parcial, armadura, offhand<br>2. Registrar slots, quantidades, componentes, dano, encantamentos, XP, vida, fome, efeitos, GM<br>3. Entrar no evento<br>4. Completar ou cancelar<br>5. Verificar todos os itens e estado |
| Resultado esperado | Todos itens preservados inclusive modded. Nenhum item vira AIR. |
| Resultado real | NOT_RUN |
| Evidencia | |
| Logs | |
| Bug | |
| Commit | |

---

## UAT-20: Modo KEEP

| Campo | Valor |
|-------|-------|
| ID | UAT-20 |
| Descricao | Validar modo KEEP (inventario nao limpo) |
| Pre-condicoes | Configurar evento com inventoryMode=KEEP |
| Passos | 1. Jogador com inventario X entra no evento<br>2. Inventario permanece X<br>3. Durante evento obtem item Y<br>4. Sai do evento<br>5. Inventario tem X+Y |
| Resultado esperado | Snapshot criado mas inventario nao limpo. Ao sair, snapshot nao sobrescreve itens adquiridos. |
| Resultado real | NOT_RUN |
| Evidencia | |
| Logs | |
| Bug | |
| Commit | |

---

## UAT-21: Modo CLEAR_AND_RESTORE

| Campo | Valor |
|-------|-------|
| ID | UAT-21 |
| Descricao | Validar modo CLEAR_AND_RESTORE |
| Pre-condicoes | Modo default |
| Passos | 1. Jogador com inventario X entra<br>2. Inventario limpo<br>3. Durante evento obtem item Y<br>4. Sai/termina<br>5. Inventario volta a X (item Y removido) |
| Resultado esperado | Restauracao integral do snapshot |
| Resultado real | NOT_RUN |
| Evidencia | |
| Logs | |
| Bug | |
| Commit | |

---

## UAT-22: Comandos e Permissoes

| Campo | Valor |
|-------|-------|
| ID | UAT-22 |
| Descricao | Validar permissoes de comando |
| Pre-condicoes | UAT-03 PASS |
| Passos | 1. Jogador comum: list, info, status, entrar, sair, ver ranking → OK<br>2. Jogador comum: create, edit, open, start, cancel → negado<br>3. Staff: todos comandos → OK<br>4. Console: comandos sem posicao → OK<br>5. Console: set-start → erro claro |
| Resultado esperado | Permissoes respeitadas |
| Resultado real | NOT_RUN |
| Evidencia | |
| Logs | |
| Bug | |
| Commit | |

---

## UAT-23: Performance

| Campo | Valor |
|-------|-------|
| ID | UAT-23 |
| Descricao | Verificar performance com 2 jogadores |
| Pre-condicoes | UAT-13 PASS |
| Passos | 1. Verificar logs durante execucao<br>2. Verificar tempo de tick (MSPT)<br>3. Verificar ausencia de I/O por tick<br>4. Verificar ausencia de spam de logs<br>5. Verificar cleanup apos sessao |
| Resultado esperado | Sem degradacao perceptivel. REGION_ENTER scan a cada 2 ticks. |
| Resultado real | NOT_RUN |
| Evidencia | |
| Logs | |
| Bug | |
| Commit | |

---

## Status Summary

| Total | PASS | FAIL | BLOCKED | NOT_RUN |
|-------|------|------|---------|---------|
| 23 | 0 | 0 | 0 | 23 |

---

## Criterios de Aceite para Producao

Para declarar Parkour pronto para producao, TODOS os seguintes cenarios devem estar PASS:

- [ ] UAT-19: snapshot com itens modded (Cobblemon)
- [ ] UAT-05: fluxo com um jogador (restauracao)
- [ ] UAT-16: saida voluntaria
- [ ] UAT-17: cancelamento
- [ ] UAT-14: desconexao dentro do prazo
- [ ] UAT-15: desconexao prazo expirado
- [ ] UAT-18: reinicio
- [ ] UAT-06/UAT-07: checkpoints (placa e regiao)
- [ ] UAT-12: conclusao concorrente
- [ ] UAT-13: dois jogadores
