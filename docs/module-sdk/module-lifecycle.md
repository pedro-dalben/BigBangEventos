# Ciclo de Vida do Módulo

Cada módulo passa por 3 fases. A interface `BigBangEventModule` define
os métodos de callback.

## Fase 1: onLoad

Chamado quando o módulo é descoberto pelo core, antes de qualquer outro
mod estar disponível.

**O que fazer aqui:**
- Ler configuração inicial do módulo
- Validar dependências
- Inicializar estruturas de dados

**O que NÃO fazer:**
- Registrar tipos de evento (muito cedo)
- Acessar serviços do core (ainda não prontos)
- Interagir com Minecraft

```java
public void onLoad(EventModuleContext ctx) {
    this.config = ctx.loadConfig("meumod.yml");
    this.dataDir = ctx.dataDirectory();
}
```

## Fase 2: onEnable

Chamado quando o core está totalmente inicializado. O módulo pode
registrar tudo.

**O que fazer aqui:**
- Registrar `EventType` via `ctx.typeRegistry().register()`
- Registrar comandos via `CommandRegistrationCallback`
- Registrar listeners Fabric (jogador, bloco, tick)
- Agendar tarefas periódicas via `PlatformScheduler`
- Verificar versão da API

```java
public void onEnable(EventModuleContext ctx) {
    if (apiVersion() != ctx.apiVersion()) {
        throw new RuntimeException("Versão da API incompatível");
    }
    ctx.typeRegistry().register(new ParkourEventType());
    CommandRegistrationCallback.EVENT.register((d, ra, e) -> {
        d.register(Commands.literal("parkour")
            .executes(ctx -> { /* ... */ }));
    });
}
```

## Fase 3: onDisable

Chamado quando o servidor está desligando ou o módulo sendo removido.

**O que fazer aqui:**
- Salvar dados pendentes
- Cancelar tarefas agendadas
- Limpar recursos
- Notificar participantes ativos

```java
public void onDisable(EventModuleContext ctx) {
    this.saveAll();
    this.scheduler.shutdown();
}
```

## Diagrama de Estados

```
[Servidor Iniciando]
       ↓
    onLoad() → config carregada
       ↓
    onEnable() → tipos registrados
       ↓
  ┌───┴───┐
  │ Ativo  │ ← servidor rodando
  └───┬───┘
       ↓
    onDisable() → dados salvos
       ↓
[Servidor Desligado]
```

## Garantias do Core

- `onLoad` é chamado antes de qualquer outro módulo ser carregado.
- `onEnable` é chamado depois que `EventEngine` está pronto.
- `onDisable` é chamado durante o shutdown do servidor (via `ServerLifecycleEvents.SERVER_STOPPING`).
- Exceções em um módulo não afetam outros módulos (cada callback é isolado com try/catch).
