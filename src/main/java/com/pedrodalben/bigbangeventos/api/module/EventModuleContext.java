package com.pedrodalben.bigbangeventos.api.module;

import com.pedrodalben.bigbangeventos.api.BigBangEventosApi;
import com.pedrodalben.bigbangeventos.eventtype.EventTypeRegistry;
import com.pedrodalben.bigbangeventos.platform.PlatformPlayerService;
import com.pedrodalben.bigbangeventos.platform.PlatformScheduler;
import com.pedrodalben.bigbangeventos.platform.PlatformTeleportService;
import com.pedrodalben.bigbangeventos.objective.*;
import com.pedrodalben.bigbangeventos.stage.StageService;
import com.pedrodalben.bigbangeventos.domain.DomainEventBus;
import com.pedrodalben.bigbangeventos.data.TypedDataService;
import com.pedrodalben.bigbangeventos.core.team.TeamService;
import com.pedrodalben.bigbangeventos.core.round.RoundService;
import com.pedrodalben.bigbangeventos.core.combat.CombatService;
import com.pedrodalben.bigbangeventos.core.combat.LifeService;
import com.pedrodalben.bigbangeventos.core.combat.EliminationService;
import com.pedrodalben.bigbangeventos.core.respawn.RespawnService;
import com.pedrodalben.bigbangeventos.core.spectator.SpectatorService;

public interface EventModuleContext {

    BigBangEventosApi api();

    EventTypeRegistry eventTypes();

    PlatformScheduler scheduler();

    PlatformPlayerService players();

    PlatformTeleportService teleport();

    EventModuleLogger logger();

    java.nio.file.Path configDirectory();

    default ObjectiveTypeRegistry objectives() { throw new UnsupportedOperationException("API 2 não disponível"); }
    default ObjectiveService objectiveService() { throw new UnsupportedOperationException("API 2 não disponível"); }
    default StageService stageService() { throw new UnsupportedOperationException("API 2 não disponível"); }
    default DomainEventBus events() { throw new UnsupportedOperationException("API 2 não disponível"); }
    default TypedDataService data() { throw new UnsupportedOperationException("API 2 não disponível"); }
    default TeamService teams() { throw new UnsupportedOperationException("API 3 não disponível"); }
    default RoundService rounds() { throw new UnsupportedOperationException("API 3 não disponível"); }
    default CombatService combat() { throw new UnsupportedOperationException("API 3 não disponível"); }
    default LifeService lives() { throw new UnsupportedOperationException("API 3 não disponível"); }
    default EliminationService eliminations() { throw new UnsupportedOperationException("API 3 não disponível"); }
    default RespawnService respawns() { throw new UnsupportedOperationException("API 3 não disponível"); }
    default SpectatorService spectators() { throw new UnsupportedOperationException("API 3 não disponível"); }
}
