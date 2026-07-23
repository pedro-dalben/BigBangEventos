package br.com.bigbangcraft.eventos.pokegladiator;

import com.pedrodalben.bigbangeventos.definition.EventDefinition;
import com.pedrodalben.bigbangeventos.definition.LocationName;
import com.pedrodalben.bigbangeventos.validation.ValidationLevel;
import com.pedrodalben.bigbangeventos.validation.ValidationResult;

public class PokeGladiatorValidator {

    public ValidationResult validate(EventDefinition def) {
        ValidationResult r = ValidationResult.empty();

        if (def.location(LocationName.LOBBY).isEmpty())
            r.add(ValidationLevel.ERROR, "missing_lobby", "Localizacao LOBBY obrigatoria");
        if (def.location(LocationName.ENTRANCE).isEmpty())
            r.add(ValidationLevel.ERROR, "missing_entrance", "Localizacao ENTRANCE obrigatoria");
        if (def.location(LocationName.EXIT).isEmpty())
            r.add(ValidationLevel.ERROR, "missing_exit", "Localizacao EXIT obrigatoria");

        String mode = PokeGladiatorConfiguration.mode(def);
        if (!"LAST_TRAINER_STANDING".equals(mode) && !"HYBRID_FREE_FOR_ALL".equals(mode)) {
            r.add(ValidationLevel.ERROR, "invalid_mode", "Modo invalido: " + mode + ". Use LAST_TRAINER_STANDING ou HYBRID_FREE_FOR_ALL");
        }

        int lives = PokeGladiatorConfiguration.initialLives(def);
        if (lives < 0)
            r.add(ValidationLevel.ERROR, "negative_lives", "Vidas iniciais devem ser >= 0");

        if ("LAST_TRAINER_STANDING".equals(mode) && lives <= 0)
            r.add(ValidationLevel.ERROR, "no_lives_lts", "LAST_TRAINER_STANDING requer vidas > 0");

        int rounds = PokeGladiatorConfiguration.roundsTotal(def);
        if (rounds < 1)
            r.add(ValidationLevel.ERROR, "no_rounds", "Numero de rodadas deve ser >= 1");

        int limit = PokeGladiatorConfiguration.roundTime(def);
        if (limit < 0)
            r.add(ValidationLevel.ERROR, "invalid_round_time", "Tempo limite de rodada invalido");

        String recovery = PokeGladiatorConfiguration.recoveryPolicy(def);
        if ("KEEP_RESULT".equals(recovery)) {
            r.add(ValidationLevel.WARNING, "keep_result", "Politica de recuperacao KEEP_RESULT nao restaura HP/status do Pokemon");
        }

        if (!PokeGladiatorConfiguration.pokemonXp(def))
            r.add(ValidationLevel.INFO, "pokemon_xp_disabled", "Experiencia de Pokemon desabilitada");
        if (!PokeGladiatorConfiguration.evGain(def))
            r.add(ValidationLevel.INFO, "ev_gain_disabled", "Ganho de EV desabilitado");

        boolean anyCombat = PokeGladiatorConfiguration.trainerVsTrainer(def)
            || PokeGladiatorConfiguration.trainerVsPokemon(def)
            || PokeGladiatorConfiguration.pokemonVsTrainer(def)
            || PokeGladiatorConfiguration.pokemonVsPokemon(def);
        if (!anyCombat)
            r.add(ValidationLevel.ERROR, "no_combat_types", "Nenhum tipo de combate habilitado na matriz");

        return r;
    }
}
