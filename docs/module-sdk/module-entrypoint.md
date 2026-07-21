# Entrypoint do Módulo

## Como Registrar

O BigBangEventos usa um entrypoint Fabric personalizado para descobrir
módulos: `bigbangeventos:event_module`.

Declare no `fabric.mod.json`:

```json
{
  "schemaVersion": 1,
  "id": "meumod",
  "version": "1.0.0",
  "name": "Meu Mod de Evento",
  "description": "Adiciona um novo tipo de evento ao BigBangEventos",
  "authors": ["Voce"],
  "license": "MIT",
  "environment": "server",
  "entrypoints": {
    "main": ["com.meumod.MeuModInitializer"],
    "bigbangeventos:event_module": ["com.meumod.MeuEventModule"]
  },
  "depends": {
    "fabricloader": ">=0.18.4",
    "minecraft": "1.21.1",
    "java": ">=21",
    "fabric-api": ">=0.116.13+1.21.1",
    "bigbangeventos": ">=0.1.0"
  },
  "custom": {
    "bigbangeventos:module": {
      "id": "meumod",
      "api-version": 1
    }
  }
}
```

## Entrypoint Main (Obrigatório)

Todo mod Fabric precisa de pelo menos um entrypoint `main`. Use o
`ModInitializer` padrão do Fabric. Ele roda antes do entrypoint do módulo.

```java
package com.meumod;

import net.fabricmc.api.ModInitializer;

public class MeuModInitializer implements ModInitializer {
    @Override
    public void onInitialize() {
        // Inicialização geral do mod (não específica de evento)
    }
}
```

## Entrypoint do Módulo

A classe referenciada em `bigbangeventos:event_module` deve implementar
`BigBangEventModule`.

```java
package com.meumod;

import com.pedrodalben.bigbangeventos.api.BigBangEventModule;
import com.pedrodalben.bigbangeventos.api.EventModuleContext;

public class MeuEventModule implements BigBangEventModule {
    @Override
    public String moduleId() {
        return "meumod";
    }

    @Override
    public int apiVersion() {
        return 1; // BigBangEventosApi.API_VERSION
    }

    @Override
    public void onLoad(EventModuleContext ctx) {
        // 1. Ler config
    }

    @Override
    public void onEnable(EventModuleContext ctx) {
        // 2. Registrar tipo, comandos, listeners
        ctx.typeRegistry().register(new MeuTipoEvento());
    }

    @Override
    public void onDisable(EventModuleContext ctx) {
        // 3. Limpeza
    }
}
```

## Interface BigBangEventModule

```java
package com.pedrodalben.bigbangeventos.api;

public interface BigBangEventModule {
    String moduleId();
    int apiVersion();
    void onLoad(EventModuleContext ctx);
    void onEnable(EventModuleContext ctx);
    void onDisable(EventModuleContext ctx);
}
```

## Interface EventModuleContext

```java
package com.pedrodalben.bigbangeventos.api;

import com.pedrodalben.bigbangeventos.eventtype.EventTypeRegistry;
import com.pedrodalben.bigbangeventos.platform.PlatformScheduler;
import java.nio.file.Path;

public interface EventModuleContext {
    int apiVersion();
    EventTypeRegistry typeRegistry();
    PlatformScheduler scheduler();
    BigBangEventosApi api();
    Path dataDirectory();
    <T> T loadConfig(String filename, Class<T> type);
    Path resolveConfig(String filename);
}
```

## Descoberta

O core descobre módulos automaticamente via Fabric Loader:

1. Escaneia todos os mods carregados pelo Fabric Loader.
2. Procura pelo entrypoint `bigbangeventos:event_module`.
3. Instancia a classe e chama `onLoad` → `onEnable`.

Não é necessário configurar nada manualmente.

## Validação

O core verifica automaticamente:

- Se `apiVersion()` retornado pelo módulo é compatível.
- Se o módulo não tenta registrar um tipo já existente.
- Se o módulo não falha em `onLoad`/`onEnable` (try/catch isolado).

## Erro Comum

```
[BigBangEventos] Módulo 'meumod' ignorado: API version 2 não é compatível.
```

Solução: atualize o módulo ou o core para versões compatíveis. Veja
`compatibility.md`.
