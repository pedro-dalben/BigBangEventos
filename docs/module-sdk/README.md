# Module SDK — BigBangEventos

## O que é

O Module SDK permite que mods Fabric externos registrem tipos de evento
personalizados no BigBangEventos. Cada módulo é um mod Fabric separado que
declara dependência do `bigbangeventos` e implementa o entrypoint
`bigbangeventos:event_module`.

## Como funciona

1. Crie um mod Fabric server-side.
2. Adicione `bigbangeventos` como dependência.
3. Implemente `BigBangEventModule` (no pacote `com.pedrodalben.bigbangeventos.api`).
4. Declare o entrypoint `bigbangeventos:event_module` no `fabric.mod.json`.
5. Registre tipos de evento, comandos e lógica personalizada.

## Documentação

| Documento | Descrição |
|-----------|-----------|
| `architecture.md` | Visão geral da arquitetura core vs módulos |
| `module-lifecycle.md` | Ciclo de vida do módulo: onLoad, onEnable, onDisable |
| `module-entrypoint.md` | Como registrar o módulo via entrypoint Fabric |
| `event-type-development.md` | Como criar um novo EventType |
| `commands.md` | Como registrar comandos no /evento |
| `triggers-conditions-actions.md` | Gatilhos, condições e ações |
| `participant-data.md` | Dados por participante |
| `persistence.md` | Opções de persistência |
| `threading.md` | Regras de threading |
| `testing.md` | Como testar o módulo |
| `compatibility.md` | Versionamento de API e compatibilidade |
| `troubleshooting.md` | Problemas comuns |
| `ai-context.md` | Contexto otimizado para IA |

## Quick Start

```java
public class MeuMod implements BigBangEventModule {
    public String moduleId() { return "meumod"; }
    public int apiVersion() { return BigBangEventosApi.API_VERSION; }
    public void onLoad(EventModuleContext ctx) { }
    public void onEnable(EventModuleContext ctx) {
        ctx.typeRegistry().register(new MeuTipoEvento());
    }
    public void onDisable(EventModuleContext ctx) { }
}
```

```json
// fabric.mod.json
{
  "id": "meumod",
  "entrypoints": {
    "bigbangeventos:event_module": ["com.meumod.MeuMod"]
  },
  "depends": {
    "bigbangeventos": ">=0.1.0"
  }
}
```

## Exemplo

Veja `examples/event-module-template/` para um template completo.

## Módulos oficiais

- `modules/parkour/` — Módulo de parkour com checkpoints, cronômetro e ranking.
