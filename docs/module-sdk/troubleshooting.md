# Solução de Problemas

## Módulo não é carregado

**Sintoma:** O módulo não aparece nos logs, nenhum tipo de evento é registrado.

**Causas possíveis:**

1. `fabric.mod.json` não declara o entrypoint `bigbangeventos:event_module`.
2. O entrypoint aponta para uma classe que não implementa `BigBangEventModule`.
3. A classe do módulo lança exceção em `onLoad` ou `onEnable`.

**Solução:**
- Verifique o entrypoint no `fabric.mod.json`:
```json
"entrypoints": {
    "bigbangeventos:event_module": ["com.meumod.MeuModule"]
}
```
- Confirme que a classe implementa `BigBangEventModule`.
- Verifique os logs: `[BigBangEventos] Module '...'`.
- Adicione `try/catch` no `onLoad` e `onEnable` e logue exceções.

## API Version incorreta

**Sintoma:** Log `API version X não é compatível (core: Y). Módulo ignorado.`

**Solução:**
- Verifique `apiVersion()` no módulo e mude para o valor do core atual.
- O valor atual é `BigBangEventosApi.API_VERSION`.

## ClassNotFoundException ou NoClassDefFoundError

**Sintoma:** Erro ao iniciar o servidor: classe do core não encontrada.

**Causas possíveis:**
1. `bigbangeventos.jar` não está na pasta `mods/`.
2. Versão do `bigbangeventos` é muito antiga e não tem a classe necessária.
3. O módulo depende de uma classe de pacote interno (`core.*`, `fabric.*`).

**Solução:**
- Confirme que `bigbangeventos` está em `mods/`.
- Confirme a versão em `fabric.mod.json` depende de `bigbangeventos`.
- Mude para usar apenas classes do pacote `api` e outros públicos.

## Dependências ausentes

**Sintoma:** Servidor não inicia, erro de dependência não satisfeita.

**Causas possíveis:**
1. `fabric-api` não está instalado.
2. `fabricloader` versão incompatível.
3. `bigbangeventos` não está na pasta `mods/`.

**Solução:**
- Verifique `depends` no `fabric.mod.json`.
- Instale todas as dependências.
- Use `fabric-api` completo (não apenas módulos individuais).

## EventType não encontrado

**Sintoma:** `/evento create meu_tipo` retorna "Tipo não encontrado".

**Causas possíveis:**
1. O módulo não registrou o tipo em `onEnable`.
2. O módulo não foi carregado (veja acima).
3. O tipo foi registrado com ID diferente.

**Solução:**
- Verifique logs: `[BigBangEventos]` na inicialização.
- Confirme que `ctx.typeRegistry().register(new MeuTipo())` é chamado.
- O ID do tipo deve ser igual ao argumento do comando.

## Comandos do módulo não aparecem

**Sintoma:** `/meumod` retorna "Comando desconhecido".

**Causas possíveis:**
1. `CommandRegistrationCallback` não foi registrado.
2. O registro ocorreu antes do Fabric estar pronto.
3. O modulo não foi carregado.

**Solução:**
- Registre `CommandRegistrationCallback` em `onEnable`.
- Verifique o namespace: comandos raiz não precisam de prefixo.

## Dados não persistem

**Sintoma:** Dados do módulo somem após reiniciar o servidor.

**Causas possíveis:**
1. O módulo não salva os dados em disco.
2. O diretório de dados não existe.
3. O arquivo YAML está corrompido.

**Solução:**
- Implemente `saveAll()` em `onDisable`.
- Use `ctx.dataDirectory()` para localização.
- Verifique se o diretório é criado (crie com `Files.createDirectories`).

## Gatilho não dispara

**Sintoma:** Clicar na placa não executa o gatilho.

**Causas possíveis:**
1. O gatilho está desabilitado (`enabled = false`).
2. As condições não são satisfeitas.
3. O binding da placa está incorreto.
4. O evento não está em execução.

**Solução:**
- Verifique `trigger.enabled()`.
- Verifique as condições (especialmente `EVENT_IS_RUNNING`).
- Refaça o bind com `/evento trigger bind <nome>` e clique na placa.
- Inicie o evento com `/evento start <id>`.

## Erro ao criar evento

**Sintoma:** `/evento create` retorna erro.

**Causas possíveis:**
1. ID do evento não segue o padrão `[a-z0-9][a-z0-9_-]{0,63}`.
2. Tipo não registrado.
3. Evento já existe.

**Solução:**
- Use apenas letras minúsculas, números, underscore e hífen.
- Confirme que o tipo está registrado.
- Use `/evento list` para ver eventos existentes.

## Log

Ative logging no módulo para depuração:

```java
private static final Logger LOG = LoggerFactory.getLogger("MeuMod");
LOG.info("Mensagem de log");
LOG.warn("Aviso: {}", detalhe);
LOG.error("Erro ao processar", exception);
```
