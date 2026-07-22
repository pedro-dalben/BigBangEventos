# Template de Módulo de Evento — BigBangEventos

## O que é

Este é um template para criar um módulo Fabric server-side que adiciona
um novo tipo de evento ao BigBangEventos.

Para API 2, o módulo pode opcionalmente usar objetivos, etapas, typed data e
`ctx.events()`; módulos API 1 continuam válidos.

## Como Usar

### 1. Copiar o Template

```bash
cp -r examples/event-module-template meu-modulo-evento
cd meu-modulo-evento
```

### 2. Renomear Pacotes e Classes

Substitua `com.meumod` pelo seu pacote. Exemplo:

| Arquivo | Substituir por |
|---------|---------------|
| `com/meumod/MeuModInitializer.java` | `com/seudominio/seumod/SeuModInitializer.java` |
| `com/meumod/MeuModModule.java` | `com/seudominio/seumod/SeuModModule.java` |
| `com/meumod/MeuTipoEvento.java` | `com/seudominio/seumod/SeuTipoEvento.java` |

### 3. Configurar build.gradle

- Altere `group`, `archivesBaseName` e `version`.
- Confirme a dependência do `bigbangeventos`.

### 4. Configurar fabric.mod.json

- Altere `id`, `name`, `description`, `authors`.
- Confirme o entrypoint `bigbangeventos:event_module`.
- Confirme as dependências.

### 5. Implementar o EventType

Edite `MeuTipoEvento.java`:

- Defina `id()` e `displayName()`.
- Implemente `validate()` para validar configuração.
- Use lifecycle hooks se necessário.

### 6. Compilar

```bash
./gradlew build
```

O JAR estará em `build/libs/`.

### 7. Instalar

- Coloque o JAR na pasta `mods/` do servidor.
- (Re)inicie o servidor.
- Verifique logs: `[BigBangEventos] Module 'meumod' loaded`.

### 8. Criar Evento

```bash
/evento create meu_evento meu_tipo
/evento edit meu_evento
/evento set lobby
/evento set entrance
/evento set exit
/evento validate meu_evento
/evento open meu_evento
/evento start meu_evento
```

## Estrutura do Template

```
event-module-template/
  build.gradle                          # Build config
  src/main/java/com/meumod/
    MeuModInitializer.java              # ModInitializer (Fabric)
    MeuModModule.java                   # BigBangEventModule
    MeuTipoEvento.java                  # EventType
    MeuModCommand.java                  # Comandos do módulo
    MeuModDataStore.java                # Persistência
    MeuModListener.java                 # Listeners Fabric
  src/main/resources/
    fabric.mod.json                     # Metadados do mod
  src/test/java/com/meumod/
    MeuModTest.java                     # Testes do módulo
    TestStubs.java                      # Stubs para testes
```

## Dependências

| Dependência | Versão |
|-------------|--------|
| Minecraft | 1.21.1 |
| Fabric Loader | >=0.18.4 |
| Fabric API | >=0.116.13+1.21.1 |
| BigBangEventos | >=0.1.0 |
| Java | >=21 |

## Documentação Relacionada

Veja `docs/module-sdk/` para documentação completa do SDK.
