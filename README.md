# BigBangEventos

Mod server-side Fabric para Minecraft 1.21.1. O núcleo preserva eventos `generic`, sessões, transições validadas, participação, ranking, persistência YAML e gatilhos abstratos. Não depende de BigBangEssentials ou Cobblemon.

## Compilar

```bash
./gradlew clean test build
```

## Artefato

`build/libs/bigbangeventos-0.1.0.jar`

## Instalar

1. Pare o servidor de teste.
2. Coloque o JAR em `mods`.
3. Confirme Fabric Loader 0.18.4, Fabric API 0.116.13+1.21.1 e Java 21.
4. Inicie e verifique os logs.
5. Execute `/evento`.

O teste completo do modpack Cobbleverse exige staging; não instale diretamente em produção.

Limitações desta fundação: tipos de prova, GUI, recompensas, snapshot real de inventário, teleportes e vínculo funcional de placas ainda precisam de adapters Fabric dedicados.
