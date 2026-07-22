# BigBangEventos Treasure Hunt

Módulo Fabric server-side oficial, ID técnico `treasure_hunt`.

Usa objetivos, etapas, typed data, triggers e eventos de domínio do Core. O comando oficial é `/evento treasure`.

Dados principais: `treasure_hunt:fragments`, `treasure_hunt:score`, `treasure_hunt:current_clue`, `treasure_hunt:discovered_clues` e `treasure_hunt:completed_stages`.

Jogador: `/evento treasure clue`, `/evento treasure progress`, `/evento treasure score`.

Admin: `/evento treasure validate <event>`, `/evento treasure stage add <event> <id>`, `/evento treasure stage list <event>`, `/evento treasure objective add <event> <stage> <id> <type>` e `/evento treasure objective list <event>`.
