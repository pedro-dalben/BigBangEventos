package br.com.bigbangcraft.eventos.gladiator;

import com.pedrodalben.bigbangeventos.definition.EventDefinition;
import com.pedrodalben.bigbangeventos.definition.LocationName;
import com.pedrodalben.bigbangeventos.validation.ValidationLevel;
import com.pedrodalben.bigbangeventos.validation.ValidationResult;

public final class GladiatorValidator {

    public ValidationResult validate(EventDefinition def) {
        ValidationResult result = new ValidationResult();

        if (def.location(LocationName.LOBBY).isEmpty())
            err(result, "Lobby não definido");
        if (def.location(LocationName.ENTRANCE).isEmpty())
            err(result, "Entrada não definida");
        if (def.location(LocationName.EXIT).isEmpty())
            err(result, "Saída não definida");
        if (def.area().isEmpty())
            err(result, "Arena não definida");

        String mode = GladiatorConfiguration.mode(def);
        if (!mode.equals("FREE_FOR_ALL") && !mode.equals("LAST_PLAYER_STANDING"))
            err(result, "Modo inválido: " + mode);

        int lives = GladiatorConfiguration.initialLives(def);
        if (lives < 0) err(result, "Vidas não podem ser negativas");
        if (lives == 0 && mode.equals("LAST_PLAYER_STANDING"))
            err(result, "LAST_PLAYER_STANDING requer vidas > 0");

        int rounds = GladiatorConfiguration.roundsTotal(def);
        if (rounds < 1) err(result, "Mínimo de 1 rodada");

        int timeLimit = GladiatorConfiguration.roundTimeLimit(def);
        if (timeLimit < 0) err(result, "Tempo limite não pode ser negativo");

        int countdown = GladiatorConfiguration.countdownSeconds(def);
        if (countdown < 0) err(result, "Contagem regressiva não pode ser negativa");

        int delay = GladiatorConfiguration.respawnDelay(def);
        if (delay < 0) err(result, "Delay de respawn não pode ser negativo");

        int invul = GladiatorConfiguration.invulnerabilitySeconds(def);
        if (invul < 0) err(result, "Invulnerabilidade não pode ser negativa");

        int scoreLimit = GladiatorConfiguration.scoreLimit(def);
        if (scoreLimit < 0) err(result, "Score limit não pode ser negativo");

        String ranking = GladiatorConfiguration.rankingStrategy(def);
        if (!ranking.equals("SURVIVAL_THEN_KILLS") && !ranking.equals("KILLS_DESCENDING")
                && !ranking.equals("SCORE_DESCENDING") && !ranking.equals("SURVIVAL_ORDER"))
            err(result, "Estratégia de ranking inválida");

        String respawnPol = GladiatorConfiguration.respawnPolicy(def);
        if (!respawnPol.equals("NONE") && !respawnPol.equals("DELAYED")
                && !respawnPol.equals("AT_TEAM_SPAWN") && !respawnPol.equals("AT_PERSONAL_SPAWN"))
            err(result, "Política de respawn inválida");

        return result;
    }

    private static void err(ValidationResult r, String m) {
        r.add(ValidationLevel.ERROR, "gladiator_validation", m);
    }
}
