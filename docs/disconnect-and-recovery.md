# Disconnect and Recovery — BigBangEventos

## Grace Period

### Configuração
```yaml
disconnect:
  default-policy: grace-period
  grace-period-seconds: 120
  timer-policy: continue
```

### Comportamento
1. Jogador desconecta → estado `DISCONNECTED`
2. Cronômetro de grace period inicia
3. Participação reservada, snapshot ativo
4. Jogador NÃO é restaurado durante grace period

### Dentro do Prazo
- Reconexão → estado volta para `ACTIVE` ou `REGISTERED`
- Participação mantida
- Snapshot não é restaurado

### Fora do Prazo
- Participante → `DISQUALIFIED`
- Motivo: `DISCONNECT_TIMEOUT`
- Snapshot fica pendente para restauração no próximo login

## Política de Cronômetro

Apenas `CONTINUE` implementado nesta fase:
- Tempo do evento continua baseado no instante original
- Cronômetro não pausa durante desconexão

## Reconexão

### Ordem de Prioridade
1. Restauração pendente → restaurar antes de qualquer retorno ao evento
2. Participação dentro do grace period → retornar ao evento
3. Grace period expirado → restaurar snapshot, informar desclassificação
4. Sessão não existe mais → restaurar snapshot

## Recuperação após Reinício

### Política
```yaml
recovery:
  interrupted-session-policy: cancel-and-restore
```

### Fluxo
1. Carregar sessões não-terminais
2. Marcar sessões interrompidas como `FAILED`
3. Motivo: `SERVER_RESTART`
4. Para cada participante:
   - Online → restaurar snapshot
   - Offline → snapshot pendente (restaurado no próximo login)

### O que NÃO acontece
- Sessão NÃO é retomada automaticamente
- Resultados já persistidos NÃO são descartados
- Snapshots NÃO são apagados

## Encerramento Limpo

No shutdown:
1. Sessões são persistidas
2. Snapshots já estão em disco
3. Jogadores NÃO são restaurados durante shutdown
4. Próxima inicialização recupera corretamente

## Estados de Participante

```
REGISTERED → inscrito, aguardando início
WAITING → em espera (não usado atualmente)
ACTIVE → participando ativamente
PAUSED → evento pausado
FINISHED → concluiu o evento
ELIMINATED → eliminado
DISQUALIFIED → desclassificado (grace period expirado)
LEFT → saiu voluntariamente
DISCONNECTED → desconectado durante evento
RESTORE_PENDING → aguardando restauração
RESTORED → snapshot restaurado
```

## Comandos de Diagnóstico

```bash
/evento recovery status      # listar snapshots pendentes
/evento recovery list        # listar todos snapshots
/evento recovery player <j>  # detalhes de snapshot de jogador
/evento recovery retry <j>   # tentar restaurar novamente
/evento debug player <j>     # diagnóstico completo do jogador
```
