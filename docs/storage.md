# Armazenamento

`LocalEventStorage` grava um YAML por definição em `config/bigbangeventos/events` e metadados de sessão em `sessions`. A interface `EventStorage` permite uma implementação posterior SQLite/MySQL/MariaDB/PostgreSQL sem expor armazenamento aos comandos.
