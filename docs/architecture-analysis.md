# Análise do ambiente

- Plataforma: Fabric server-side, para o servidor dedicado Cobbleverse.
- Minecraft: 1.21.1; Fabric Loader 0.18.4; Fabric API 0.116.13+1.21.1; Java 21; Loom 1.9.2.
- Mappings: Parchment 2024.11.17 sobre 1.21.1.
- Referência local: BigBangWorld, BigBangEssentials e BigBangClaimsRegions usam callbacks Fabric compatíveis com este ambiente.

## Estrutura e decisões

`src/main/java` contém domínio, persistência, Brigadier e adapters Fabric. Definições são YAML em `config/bigbangeventos/events`; sessões ficam separadas em `sessions`. O núcleo usa `Clock`, uma única porta de transições e objetos de localização serializáveis.

BigBangEssentials foi apenas consultado para versões e bootstrap. Não há importação, cópia de classe interna ou dependência obrigatória. Integrações futuras devem consumir API pública/provedores, não reflexão.

## Riscos e validações pendentes

O servidor Cobbleverse real ainda precisa validar permissões externas, formato definitivo dos arquivos e os hooks públicos que o Essentials vai oferecer. Cobblemon, economia, itens completos, teleporte e snapshots reais não são assumidos pelo núcleo.
