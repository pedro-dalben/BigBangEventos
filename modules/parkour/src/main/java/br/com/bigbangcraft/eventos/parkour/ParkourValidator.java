package br.com.bigbangcraft.eventos.parkour;

import br.com.bigbangcraft.eventos.parkour.model.*;
import com.pedrodalben.bigbangeventos.definition.EventDefinition;
import com.pedrodalben.bigbangeventos.validation.ValidationLevel;
import com.pedrodalben.bigbangeventos.validation.ValidationResult;

import java.util.List;

public final class ParkourValidator {

    public ParkourValidator() {}

    public ValidationResult validate(EventDefinition definition) {
        ValidationResult result = ValidationResult.empty();

        if (ParkourConfiguration.getStartLocation(definition).isEmpty()) {
            result.add(ValidationLevel.ERROR, "no_start", "Início do parkour não configurado.");
        }

        if (ParkourConfiguration.getFinishLocation(definition).isEmpty()) {
            result.add(ValidationLevel.ERROR, "no_finish", "Saída do parkour não configurada.");
        }

        double finishRadius = ParkourConfiguration.getFinishRadius(definition);
        if (finishRadius <= 0) {
            result.add(ValidationLevel.ERROR, "invalid_finish_radius", "Raio da saída deve ser positivo.");
        }

        long maxTime = ParkourConfiguration.getMaxTimeSeconds(definition);
        if (maxTime < 0) {
            result.add(ValidationLevel.ERROR, "invalid_max_time", "Tempo máximo não pode ser negativo.");
        }

        int maxAttempts = ParkourConfiguration.getMaxAttempts(definition);
        if (maxAttempts < 0) {
            result.add(ValidationLevel.ERROR, "invalid_max_attempts", "Tentativas máximas não podem ser negativas.");
        }

        double fallY = ParkourConfiguration.getFallYLevel(definition);
        if (!Double.isFinite(fallY)) {
            result.add(ValidationLevel.ERROR, "invalid_fall_y", "Nível Y de queda inválido.");
        }

        ParkourFallMode fallMode = ParkourConfiguration.getFallMode(definition);
        validateEnum(definition, result, "parkour.fallMode", fallMode, ParkourFallMode.values());

        ParkourResetMode resetMode = ParkourConfiguration.getResetMode(definition);
        validateEnum(definition, result, "parkour.resetMode", resetMode, ParkourResetMode.values());

        ParkourFinishMode finishMode = ParkourConfiguration.getFinishMode(definition);
        validateEnum(definition, result, "parkour.finishMode", finishMode, ParkourFinishMode.values());

        ParkourRankingStrategy rankingStrategy = ParkourConfiguration.getRankingStrategy(definition);
        validateEnum(definition, result, "parkour.rankingStrategy", rankingStrategy, ParkourRankingStrategy.values());

        ParkourCompleteDestination dest = ParkourConfiguration.getCompleteDestination(definition);
        validateEnum(definition, result, "parkour.completeDestination", dest, ParkourCompleteDestination.values());

        if (result.valid() && ParkourConfiguration.getFinishLocation(definition).isPresent()
                && ParkourConfiguration.getStartLocation(definition).isPresent()) {
            result.add(ValidationLevel.INFO, "ready", "Parkour configurado corretamente.");
        }

        return result;
    }

    private <T extends Enum<T>> void validateEnum(EventDefinition def, ValidationResult result,
                                                   String key, T value, T[] values) {
        if (value == null) {
            result.add(ValidationLevel.WARNING, "invalid_" + key.replace('.', '_'),
                    "Valor inválido para " + key + ". Valores: " + listEnumValues(values));
        }
    }

    private <T extends Enum<T>> String listEnumValues(T[] values) {
        StringBuilder sb = new StringBuilder();
        for (T v : values) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(v.name());
        }
        return sb.toString();
    }
}
