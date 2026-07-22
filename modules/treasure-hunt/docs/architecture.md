# Arquitetura

`TreasureHuntEventType` valida e delega lifecycle ao `TreasureHuntSessionService`, que usa somente `BigBangEventosApi`, services públicos, event bus e platform services. O módulo não importa BigBangRegions.
