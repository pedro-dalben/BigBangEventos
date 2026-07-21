# Compatibilidade

## Versionamento da API

A API do módulo é versionada com um **inteiro** (`api-version`). Cada
versão do `bigbangeventos` declara qual versão da API suporta.

| Versão do Core | API Version |
|----------------|-------------|
| 0.1.0 | 1 |

O módulo declara em `fabric.mod.json` qual versão da API espera:

```json
{
  "custom": {
    "bigbangeventos:module": {
      "id": "meumod",
      "api-version": 1
    }
  }
}
```

E na implementação:

```java
public int apiVersion() {
    return 1; // ou uma constante
}
```

## Verificação Mínima de API

O core verifica na carga do módulo:

1. Se `apiVersion()` do módulo é igual à versão atual da API.
2. Se não for, o módulo é ignorado com aviso no log.
3. O módulo não recebe `onLoad` nem `onEnable`.

```java
// No EventModuleContext
int apiVersion = ctx.apiVersion(); // versão atual do core
```

## Dependências

O módulo declara dependências no `fabric.mod.json`:

```json
"depends": {
    "fabricloader": ">=0.18.4",
    "minecraft": "1.21.1",
    "java": ">=21",
    "fabric-api": ">=0.116.13+1.21.1",
    "bigbangeventos": ">=0.1.0"
}
```

## Mudanças que Quebram Compatibilidade

| Mudança | Impacto |
|---------|---------|
| API version incrementado | Módulos antigos não carregam |
| Pacote `api` removido ou movido | Módulo não compila |
| Método adicionado a `BigBangEventModule` | Módulo existente compila (default methods) |
| Enum `ActionType`/`ConditionType` modificado | Módulo compila se não usa o valor removido |
| `EventDefinition` campo removido | Módulo que acessa diretamente não compila |

## Compatibilidade para Frente

- O core nunca remove métodos de interfaces públicas sem incrementar
  API version.
- Métodos novos em interfaces têm implementação padrão (`default`).
- Enums podem receber novos valores sem quebrar módulos existentes.
- Classes internas (`core`, `fabric`, `persistence`, `lifecycle`, `timer`)
  podem mudar a qualquer momento.

## Verificação em Tempo de Execução

```java
public void onEnable(EventModuleContext ctx) {
    int MINIMUM_API = 1;
    if (ctx.apiVersion() < MINIMUM_API) {
        throw new RuntimeException(
            "MeuMod requer API version " + MINIMUM_API
            + " mas core fornece " + ctx.apiVersion());
    }
}
```

## Logs de Incompatibilidade

```
[BigBangEventos] Módulo 'meumod' não carregado:
    API version 2 não é compatível (core: 1).
    Atualize o módulo ou o BigBangEventos Core.
```

## Boas Práticas

1. **Sempre declare `bigbangeventos` como dependência** com versão
   mínima em `fabric.mod.json`.
2. **Verifique `ctx.apiVersion()` em `onEnable`.**
3. **Teste o módulo com a versão mais antiga suportada do core.**
4. **Documente no README do módulo qual versão do core é necessária.**
5. **Mude o API version do módulo apenas quando quebrar compatibilidade.**
