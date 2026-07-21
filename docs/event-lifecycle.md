# Ciclo de vida

`CREATED → REGISTRATION_OPEN → REGISTRATION_CLOSED/COUNTDOWN → RUNNING → PAUSED/RUNNING → FINISHING → FINISHED` é validado centralmente. Cancelamento é permitido antes da finalização. Estados terminais não voltam a executar; a tentativa retorna `invalid_transition`.
