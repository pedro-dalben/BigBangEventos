# Template: Criar Módulo de Evento

## Instruções para IA

Leia `docs/module-sdk/ai-context.md` antes de gerar código. Siga as
regras:
- Não altere o BigBangEventos Core.
- Crie um módulo Fabric server-side separado.
- Use apenas APIs públicas listadas.

## Template

Crie um módulo Fabric server-side para BigBangEventos com as seguintes
características:

### Identidade

- ID do módulo: `{{EVENT_ID}}_module`
- Nome do evento: {{EVENT_NAME}}
- Descrição: {{EVENT_DESCRIPTION}}
- Grupo do módulo: `com.meumod.{{EVENT_ID}}`

### Regras do Evento

{{EVENT_RULES}}

### Comandos

{{COMMANDS}}

### Configuração

{{CONFIGURATION}}

### Gatilhos (Triggers)

{{TRIGGERS}}

### Condições

{{CONDITIONS}}

### Ações

{{ACTIONS}}

### Ranking

{{RANKING}}

### Estrutura de Arquivos

```
{{EVENT_ID}}_module/
  build.gradle
  src/main/java/com/meumod/{{EVENT_ID}}/
    {{EVENT_ID|capitalize}}Initializer.java      (ModInitializer)
    {{EVENT_ID|capitalize}}Module.java            (BigBangEventModule)
    {{EVENT_ID|capitalize}}EventType.java         (EventType)
    {{EVENT_ID|capitalize}}DataStore.java         (persistência)
    {{EVENT_ID|capitalize}}Command.java           (comandos)
  src/main/resources/
    fabric.mod.json
  src/test/java/com/meumod/{{EVENT_ID}}/
    {{EVENT_ID|capitalize}}ModuleTest.java
```

### Testes

{{TEST_SCENARIOS}}

### Observações

- Minecraft 1.21.1
- Fabric Loader >=0.18.4
- Fabric API >=0.116.13+1.21.1
- Java >=21
- Dependência: bigbangeventos >=0.1.0
