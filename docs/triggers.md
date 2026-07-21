# Gatilhos

Há modelo para todos os tipos solicitados; `SIGN_INTERACT` e `MANUAL` têm caminho funcional. A placa é identificada pela dimensão e coordenadas vinculadas, nunca pelo texto. O executor faz habilitação, limite/cooldown, condições e ações em ordem; falha interrompe o restante.

Exemplo: crie `chegada sign_interact`, vincule com `/evento trigger bind chegada`, adicione `player_is_participant`, `event_is_running`, `player_not_finished`, depois `send_message`, `add_points` e `player_complete`.
