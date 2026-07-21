# Runtime Smoke Tests — BigBangEventos

## Ambiente de Teste

- Servidor Fabric 1.21.1 em modo desenvolvimento (`./gradlew runServer`)
- Cliente Fabric 1.21.1
- Java 21
- Sem mods adicionais (teste vanilla)

## Smoke Test 1 — Cenário Normal

### Pré-condições
- Servidor iniciado
- Cliente conectado

### Passos
1. Jogador coloca itens identificáveis no inventário (ex: 5 diamantes)
2. Verificar vida (deve ser 20), fome (20), XP (0)
3. Anotar localização atual (ex: spawn)
4. Staff: `/evento create smoke generic`
5. Staff: `/evento edit smoke`
6. Staff: `/evento set lobby` (no local do lobby)
7. Staff: `/evento set entrance` (no local da entrada)
8. Staff: `/evento set exit` (no local da saída)
9. Staff: `/evento open smoke`
10. Jogador: `/evento entrar smoke`
11. Verificar snapshot criado em `config/bigbangeventos/snapshots/<uuid>/`
12. Verificar teleporte para lobby
13. Verificar se inventário foi limpo (CLEAR_AND_RESTORE)
14. Staff: `/evento start smoke`
15. Verificar teleporte para entrada
16. Staff: `/evento finish smoke`
17. Verificar restauração de inventário (5 diamantes de volta)
18. Verificar teleporte para saída
19. Confirmar que nenhum item foi perdido ou duplicado

### Resultado Esperado
- [ ] Snapshot persistido
- [ ] Teleporte ao lobby ao entrar
- [ ] Teleporte à entrada ao iniciar
- [ ] Inventário restaurado ao finalizar
- [ ] 5 diamantes no inventário (nem mais, nem menos)
- [ ] Vida/fome/XP restaurados
- [ ] Teleporte à saída configurada

---

## Smoke Test 2 — Saída Voluntária

### Passos
1. Jogador: `/evento entrar smoke`
2. Aguardar snapshot
3. Jogador: `/evento sair`
4. Verificar inventário restaurado
5. Verificar teleporte (saída ou localização original)
6. Verificar que participação foi removida
7. Jogador: `/evento sair` novamente
8. Verificar mensagem "já não participa"

### Resultado Esperado
- [ ] Inventário restaurado após sair
- [ ] Teleporte correto
- [ ] Participação removida
- [ ] Segunda execução idempotente

---

## Smoke Test 3 — Cancelamento

### Passos
1. Dois jogadores entram no evento
2. Staff: `/evento start smoke`
3. Staff: `/evento cancel smoke`
4. Verificar ambos restaurados
5. Verificar sessão CANCELADA
6. Staff: `/evento cancel smoke` novamente
7. Verificar idempotência (sem erro, sem duplicação)

### Resultado Esperado
- [ ] Ambos jogadores restaurados
- [ ] Sessão cancelada
- [ ] Nenhum recebe recompensa
- [ ] Segundo cancelamento idempotente

---

## Smoke Test 4 — Desconexão e Retorno

### Passos
1. Jogador entra no evento
2. Staff: `/evento start smoke`
3. Jogador desconecta (fecha cliente)
4. Verificar estado DISCONNECTED
5. Jogador reconecta dentro de 120 segundos
6. Verificar participação recuperada (ACTIVE)
7. Verificar snapshot NÃO foi restaurado prematuramente

### Resultado Esperado
- [ ] Estado DISCONNECTED registrado
- [ ] Reconexão recupera participação
- [ ] Snapshot mantido (não restaurado)

---

## Smoke Test 5 — Grace Period Expirado

### Pré-condições
- Configurar `grace-period-seconds: 10` para teste rápido

### Passos
1. Jogador entra no evento
2. Desconecta
3. Aguardar 15 segundos
4. Jogador reconecta
5. Verificar snapshot restaurado
6. Verificar jogador NÃO retorna ao evento

### Resultado Esperado
- [ ] Participante desclassificado
- [ ] Snapshot restaurado no reconectar
- [ ] Jogador fora do evento

---

## Smoke Test 6 — Reinício

### Passos
1. Jogador entra no evento
2. Snapshot é persistido
3. Staff: `/evento start smoke`
4. Servidor encerrado (`/stop` ou Ctrl+C)
5. Servidor iniciado novamente
6. Verificar que sessão está FAILED (motivo: SERVER_RESTART)
7. Jogador reconecta
8. Verificar snapshot restaurado
9. Verificar itens não duplicados

### Resultado Esperado
- [ ] Sessão marcada FAILED após reinício
- [ ] Jogador restaurado ao reconectar
- [ ] Nenhum item duplicado

### Segundo Reinício
- [ ] Segundo reinício não tenta restaurar novamente (RESTORED)

---

## Smoke Test 7 — Placas

### Passos
1. Staff cria trigger: `/evento trigger create finish SIGN_INTERACT`
2. Staff: `/evento trigger bind finish`
3. Staff clica em uma placa para vincular
4. Jogador entra no evento
5. Staff: `/evento start smoke`
6. Jogador clica na placa vinculada
7. Verificar conclusão registrada
8. Jogador clica novamente → verificar que não duplica

### Resultado Esperado
- [ ] Bind de placa funciona
- [ ] Interação reconhecida (apenas MAIN_HAND)
- [ ] Conclusão única (não duplica)

---

## Smoke Test 8 — Comandos de Recuperação

### Passos
1. Staff: `/evento recovery status`
2. Staff: `/evento recovery list`
3. Staff: `/evento recovery player <nome>`
4. Staff: `/evento debug player <nome>`

### Resultado Esperado
- [ ] Comandos retornam informação
- [ ] Sem erro de permissão para admin
- [ ] Jogador comum não pode usar recovery/debug
