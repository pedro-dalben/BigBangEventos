# Comandos — BigBangEventos

## Alias
- `/evento` (principal)
- `/event` (atalho)

## Comandos de Administrador (nível 2)

| Comando | Descrição |
|---------|-----------|
| `/evento create <id> <type>` | Criar novo evento |
| `/evento edit <id>` | Selecionar evento para edição |
| `/evento delete <id>` | Excluir evento |
| `/evento open <id>` | Abrir inscrições |
| `/evento close <id>` | Fechar inscrições |
| `/evento start <id>` | Iniciar evento |
| `/evento pause <id>` | Pausar evento |
| `/evento resume <id>` | Retomar evento |
| `/evento finish <id>` | Finalizar evento |
| `/evento cancel <id>` | Cancelar evento |
| `/evento validate <id>` | Validar configuração |
| `/evento set <location>` | Definir localização (lobby, entrance, exit) |
| `/evento trigger create <name> <type>` | Criar gatilho |
| `/evento trigger bind <name>` | Vincular gatilho a placa |
| `/evento trigger bind-cancel` | Cancelar bind pendente |
| `/evento trigger list` | Listar gatilhos do evento selecionado |

## Comandos de Jogador

| Comando | Descrição |
|---------|-----------|
| `/evento list` | Listar eventos disponíveis |
| `/evento info <id>` | Ver informações do evento |
| `/evento status` | Ver status da participação atual |
| `/evento entrar <id>` | Entrar no evento |
| `/evento sair` | Sair do evento atual |

## Comandos de Recuperação (Admin)

| Comando | Descrição |
|---------|-----------|
| `/evento recovery status` | Listar snapshots pendentes |
| `/evento recovery list` | Listar todos snapshots |
| `/evento recovery player <nome>` | Detalhes do snapshot do jogador |
| `/evento recovery retry <nome>` | Tentar restaurar novamente |

## Comandos de Diagnóstico (Admin)

| Comando | Descrição |
|---------|-----------|
| `/evento debug player <nome>` | Diagnóstico completo do jogador |

### Saída do `debug player`:
```
UUID: <uuid>
Online: true/false
Evento: <id>
Sessão: <estado>
Estado: <participant-state>
Snapshot: <snapshot-id> (<state>)
Localização: <dimension> <x> <y> <z>
```

## Permissões

- Console: acesso total
- Operadores (nível 2): acesso administrativo
- Jogadores comuns: list, info, status, entrar, sair
- Permissões futuras via `PermissionProvider`: `bigbangeventos.admin`, `bigbangeventos.command.*`, `bigbangeventos.player.*`
