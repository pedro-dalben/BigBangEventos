package br.com.bigbangcraft.eventos.parkour;

public final class ParkourMessages {

    private ParkourMessages() {}

    public static final String PREFIX = "§6[Parkour]§r ";

    public static final String EVENTO_NAO_ENCONTRADO = PREFIX + "§cEvento de parkour não encontrado.";
    public static final String SELECIONE_EVENTO = PREFIX + "§cUse /evento edit <id> antes.";
    public static final String NAO_E_PARKOUR = PREFIX + "§cEvento não é do tipo parkour.";
    public static final String COMANDO_APENAS_JOGADOR = PREFIX + "§cApenas jogadores podem usar este comando.";
    public static final String PERMISSAO_NEGADA = PREFIX + "§cVocê não tem permissão.";

    public static final String START_SET = PREFIX + "§aInício do parkour definido na sua posição.";
    public static final String FINISH_SET = PREFIX + "§aSaída do parkour definida na sua posição com raio %s.";
    public static final String FINISH_RADIUS_SET = PREFIX + "§aRaio da saída definido para %s.";
    public static final String FALL_Y_SET = PREFIX + "§aAltura de queda definida para Y=%s.";
    public static final String RESET_MODE_SET = PREFIX + "§aModo de reset definido para %s.";
    public static final String MAX_TIME_SET = PREFIX + "§aTempo máximo definido para %s segundos.";
    public static final String MAX_ATTEMPTS_SET = PREFIX + "§aTentativas máximas definidas para %s.";
    public static final String FINISH_MODE_SET = PREFIX + "§aModo de finalização definido para %s.";
    public static final String RANKING_STRATEGY_SET = PREFIX + "§aEstratégia de ranking definida para %s.";
    public static final String COMPLETE_DESTINATION_SET = PREFIX + "§aDestino ao completar definido para %s.";
    public static final String CHECKPOINTS_REQUIRED_SET = PREFIX + "§aCheckpoints obrigatórios definido para %s.";

    public static final String CHECKPOINT_ADDED = PREFIX + "§aCheckpoint §f%s§a adicionado (ordem %s, raio %.1f).";
    public static final String CHECKPOINT_REMOVED = PREFIX + "§eCheckpoint §f%s§e removido.";
    public static final String CHECKPOINT_NOT_FOUND = PREFIX + "§cCheckpoint §f%s§c não encontrado.";
    public static final String CHECKPOINT_LIST_HEADER = PREFIX + "§6Checkpoints:";
    public static final String CHECKPOINT_LIST_ENTRY = " §7- §f%s §7(ordem %s, raio %.1f) §a[%s]";
    public static final String CHECKPOINT_RADIUS_SET = PREFIX + "§aRaio do checkpoint §f%s§a definido para %.1f.";
    public static final String CHECKPOINT_INFO = PREFIX + "§6%s§r ordem=%s raio=%.1f em %.0f,%.0f,%.0f (%s)";
    public static final String CHECKPOINT_TELEPORTED = PREFIX + "§aTeleportado para checkpoint §f%s§a.";
    public static final String NO_CHECKPOINTS = PREFIX + "§cNenhum checkpoint configurado.";
    public static final String NO_START = PREFIX + "§cInício do parkour não configurado.";
    public static final String NO_FINISH = PREFIX + "§cSaída do parkour não configurada.";

    public static final String VALIDATION_FAILED = PREFIX + "§cValidação falhou:";
    public static final String VALIDATION_PASSED = PREFIX + "§aParkour válido!";

    public static String checkpointListEntry(String id, int order, double radius, String status) {
        return String.format(CHECKPOINT_LIST_ENTRY, id, order, radius, status);
    }

    public static String checkpointAdded(String id, int order, double radius) {
        return String.format(CHECKPOINT_ADDED, id, order, radius);
    }

    public static String checkpointRemoved(String id) {
        return String.format(CHECKPOINT_REMOVED, id);
    }

    public static String checkpointRadiusSet(String id, double radius) {
        return String.format(CHECKPOINT_RADIUS_SET, id, radius);
    }

    public static String checkpointInfo(String id, int order, double radius, double x, double y, double z, String dim) {
        return String.format(CHECKPOINT_INFO, id, order, radius, x, y, z, dim);
    }

    public static String checkpointTeleported(String id) {
        return String.format(CHECKPOINT_TELEPORTED, id);
    }

    public static String finishSet(String radius) {
        return String.format(FINISH_SET, radius);
    }

    public static String finishRadiusSet(String radius) {
        return String.format(FINISH_RADIUS_SET, radius);
    }

    public static String fallYSet(double y) {
        return String.format(FALL_Y_SET, String.valueOf(y));
    }

    public static String resetModeSet(String mode) {
        return String.format(RESET_MODE_SET, mode);
    }

    public static String maxTimeSet(long seconds) {
        return String.format(MAX_TIME_SET, seconds);
    }

    public static String maxAttemptsSet(int qty) {
        return String.format(MAX_ATTEMPTS_SET, qty);
    }

    public static String finishModeSet(String mode) {
        return String.format(FINISH_MODE_SET, mode);
    }

    public static String rankingStrategySet(String mode) {
        return String.format(RANKING_STRATEGY_SET, mode);
    }

    public static String completeDestinationSet(String mode) {
        return String.format(COMPLETE_DESTINATION_SET, mode);
    }

    public static String checkpointsRequiredSet(boolean required) {
        return String.format(CHECKPOINTS_REQUIRED_SET, required ? "sim" : "não");
    }

    public static String checkpointNotFound(String id) {
        return String.format(CHECKPOINT_NOT_FOUND, id);
    }
}
