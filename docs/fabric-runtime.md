# Runtime Fabric

O entrypoint `BigBangEventosMod` inicializa o armazenamento em `config/bigbangeventos`, registra comandos Brigadier e instala callbacks server-side. O JAR não possui entrypoint client, Mixins ou integração obrigatória com outros mods.

O runtime de desenvolvimento é iniciado com `./gradlew runServer`. A configuração YAML fica em `config/bigbangeventos/events`; sessões ficam em `config/bigbangeventos/sessions`.
