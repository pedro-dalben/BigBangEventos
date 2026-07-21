# Troubleshooting Player Restore — BigBangEventos

## Procedimento de Emergência

> O jogador entrou no servidor, mas o inventário não foi restaurado.

### Passo 1: Diagnosticar (não entregar itens manualmente)

```bash
/evento debug player <nome>
```

Verificar:
- Estado do participante
- Snapshot pendente
- Estado de restauração
- Componentes já restaurados

### Passo 2: Verificar Snapshot

```bash
/evento recovery player <nome>
```

Isso mostra:
- Snapshot ID
- Estado (PENDING/CAPTURED/RESTORING/RESTORED/FAILED)
- Sessão de origem
- Componentes restaurados

### Passo 3: Consultar Logs

```bash
grep "AUDIT" logs/latest.log | grep "<uuid>"
```

Procurar por:
- `AUDIT snapshot_created` — snapshot foi criado
- `AUDIT snapshot_restored` — restauração foi concluída
- `AUDIT restore_failed` — restauração falhou

### Passo 4: Tentar Restauração Automática

```bash
/evento recovery retry <nome>
```

Se o snapshot estiver em FAILED, isso tenta restaurar novamente.
Se estiver RESTORED, não faz nada (idempotente).

### Passo 5: Inspecionar Arquivo

```bash
cat config/bigbangeventos/snapshots/<uuid>/<snapshot-id>.yml
```

Verificar:
- Campos serializedInventory: não vazios
- Campos health, food, etc.
- Estado do snapshot no arquivo

### Passo 6: Recuperação Manual (último recurso)

**ATENÇÃO**: Só faça isso se a restauração automática falhou E você confirmou que o snapshot contém dados válidos.

1. Anote os itens do snapshot (estão no YAML como strings NBT)
2. Restaure os itens manualmente (comando `/give`)
3. Marque o snapshot como RESTORED:
   - Edite o arquivo YAML, mude `state: FAILED` para `state: RESTORED`
   - Adicione `restoredComponents: [INVENTORY, ARMOR, OFFHAND, EXPERIENCE, HEALTH, HUNGER, EFFECTS, GAME_MODE, FLIGHT_STATE, MISC_STATE, LOCATION]`

## O Que NÃO Apagar Manualmente

- **Snapshots com estado CAPTURED ou APPLIED** — o jogador pode estar no meio do evento
- **Snapshots com estado RESTORING** — restauração em andamento
- **Arquivos de sessão em `sessions/`** — contêm participantes ativos
- **Arquivos de definição em `events/`** — contêm configuração do evento

## Erros Comuns

### "Jogador não está online"
- Jogador precisa estar online para restauração
- Snapshot fica como RESTORE_PENDING
- Será restaurado no próximo login

### "Snapshot não pôde ser criado"
- Verificar se jogador está online
- Verificar se dimensão do jogador é acessível
- Verificar logs para exceção específica

### "Restauração falhou"
- Verificar logs para componente específico que falhou
- Componentes já restaurados não são reaplicados
- Usar `/evento recovery retry` para tentar novamente

### "Mundo não encontrado"
- Dimensão da localização original não existe mais
- Verificar se o mundo foi deletado ou renomeado
- Teleporte pode falhar mas inventário já foi restaurado
