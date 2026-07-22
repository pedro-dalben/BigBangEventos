package com.pedrodalben.bigbangeventos.core.team;

import com.pedrodalben.bigbangeventos.api.OperationResult;
import com.pedrodalben.bigbangeventos.definition.EventDefinition;
import com.pedrodalben.bigbangeventos.definition.team.TeamDefinition;
import com.pedrodalben.bigbangeventos.domain.DomainEventBus;
import com.pedrodalben.bigbangeventos.domain.TeamEvents;
import com.pedrodalben.bigbangeventos.session.EventSession;
import com.pedrodalben.bigbangeventos.session.team.SessionTeam;
import com.pedrodalben.bigbangeventos.session.team.TeamStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public final class TeamService {
    private final Clock clock;
    private final DomainEventBus events;

    public TeamService(Clock clock, DomainEventBus events) {
        this.clock = clock; this.events = events;
    }

    public synchronized OperationResult createTeam(EventDefinition def, EventSession session, String teamDefId) {
        var tDef = findTeamDef(def, teamDefId);
        if (tDef == null) return OperationResult.fail("team_def_not_found", "Definição de time não encontrada");
        if (!tDef.enabled()) return OperationResult.fail("team_def_disabled", "Definição de time desabilitada");
        if (session.team(teamDefId).isPresent()) return OperationResult.fail("team_exists", "Time já existe na sessão");

        SessionTeam team = new SessionTeam(UUID.randomUUID(), def.id(), session.id(), teamDefId, clock.instant());
        session.addTeam(team);
        events.publish(new TeamEvents.TeamCreated(def.id(), session.id(), team.teamId(), teamDefId));
        return OperationResult.ok("Time criado");
    }

    public synchronized OperationResult removeTeam(EventDefinition def, EventSession session, UUID teamId) {
        SessionTeam team = session.teamById(teamId).orElse(null);
        if (team == null) return OperationResult.fail("team_not_found", "Time não encontrado");
        session.removeTeam(teamId);
        return OperationResult.ok("Time removido");
    }

    public synchronized OperationResult removeTeamByDef(EventDefinition def, EventSession session, String teamDefId) {
        SessionTeam team = session.team(teamDefId).orElse(null);
        if (team == null) return OperationResult.fail("team_not_found", "Time não encontrado");
        session.removeTeam(team.teamId());
        return OperationResult.ok("Time removido");
    }

    public synchronized OperationResult assignPlayer(EventDefinition def, EventSession session, UUID player, String teamDefId) {
        var tDef = findTeamDef(def, teamDefId);
        if (tDef == null) return OperationResult.fail("team_def_not_found", "Definição de time não encontrada");
        SessionTeam team = session.team(teamDefId).orElse(null);
        if (team == null) return OperationResult.fail("team_not_found", "Time não foi criado na sessão");

        if (team.hasMember(player)) return OperationResult.ok("Jogador já está no time");
        if (team.memberCount() >= tDef.maximumPlayers()) return OperationResult.fail("team_full", "Time atingiu capacidade máxima");

        SessionTeam current = getPlayerTeam(session, player);
        if (current != null) current.removeMember(player);

        team.addMember(player);
        events.publish(new TeamEvents.PlayerAssignedToTeam(def.id(), session.id(), team.teamId(), player));
        return OperationResult.ok("Jogador atribuído ao time");
    }

    public synchronized OperationResult removePlayer(EventDefinition def, EventSession session, UUID player) {
        SessionTeam team = getPlayerTeam(session, player);
        if (team == null) return OperationResult.fail("player_not_in_team", "Jogador não está em nenhum time");
        team.removeMember(player);
        events.publish(new TeamEvents.PlayerRemovedFromTeam(def.id(), session.id(), team.teamId(), player));
        return OperationResult.ok("Jogador removido do time");
    }

    public synchronized OperationResult addScore(EventDefinition def, EventSession session, String teamDefId, long delta) {
        SessionTeam team = session.team(teamDefId).orElse(null);
        if (team == null) return OperationResult.fail("team_not_found", "Time não encontrado");
        team.addScore(delta);
        events.publish(new TeamEvents.TeamScoreChanged(def.id(), session.id(), team.teamId(), team.score(), delta));
        return OperationResult.ok("Pontuação atualizada");
    }

    public synchronized OperationResult eliminateTeam(EventDefinition def, EventSession session, String teamDefId) {
        SessionTeam team = session.team(teamDefId).orElse(null);
        if (team == null) return OperationResult.fail("team_not_found", "Time não encontrado");
        if (team.status() == TeamStatus.ELIMINATED) return OperationResult.ok("Time já eliminado");
        team.status(TeamStatus.ELIMINATED);
        events.publish(new TeamEvents.TeamEliminated(def.id(), session.id(), team.teamId()));
        return OperationResult.ok("Time eliminado");
    }

    public synchronized OperationResult assignRandom(EventDefinition def, EventSession session, String eventId,
                                                                          List<UUID> players, List<String> teamDefIds) {
        if (teamDefIds.isEmpty()) return OperationResult.fail("no_teams", "Nenhum time disponível");
        List<String> available = teamDefIds.stream()
                .filter(tid -> session.team(tid).map(t -> t.memberCount() < findMax(def, tid)).orElse(false)
                        || session.team(tid).isEmpty())
                .collect(Collectors.toList());
        if (available.isEmpty()) return OperationResult.fail("all_teams_full", "Todos os times estão cheios");

        List<UUID> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled, new Random(seed(clock.instant())));

        int idx = 0;
        for (UUID player : shuffled) {
            String tid = available.get(idx % available.size());
            ensureTeamExists(def, session, tid);
            assignPlayer(def, session, player, tid);
            idx++;
        }
        return OperationResult.ok("Times distribuídos aleatoriamente");
    }

    public synchronized OperationResult assignBalanced(EventDefinition def, EventSession session, String eventId,
                                                                           List<UUID> players, List<String> teamDefIds) {
        if (teamDefIds.isEmpty()) return OperationResult.fail("no_teams", "Nenhum time disponível");
        for (String tid : teamDefIds) ensureTeamExists(def, session, tid);

        List<SessionTeam> teams = teamDefIds.stream()
                .map(tid -> session.team(tid).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (teams.isEmpty()) return OperationResult.fail("no_teams", "Nenhum time disponível");

        List<UUID> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled, new Random(seed(clock.instant())));

        for (UUID player : shuffled) {
            SessionTeam target = teams.stream()
                    .min(Comparator.comparingInt(SessionTeam::memberCount)
                            .thenComparing(t -> t.teamId().toString()))
                    .orElse(teams.get(0));
            assignPlayer(def, session, player, target.teamDefinitionId());
        }
        return OperationResult.ok("Times distribuídos equilibradamente");
    }

    public SessionTeam getPlayerTeam(EventSession session, UUID player) {
        return session.teams().values().stream()
                .filter(t -> t.hasMember(player)).findFirst().orElse(null);
    }

    public Optional<SessionTeam> getTeam(EventSession session, String teamDefId) {
        return session.team(teamDefId);
    }

    public List<SessionTeam> listTeams(EventSession session) {
        return List.copyOf(session.teams().values());
    }

    private void ensureTeamExists(EventDefinition def, EventSession session, String teamDefId) {
        if (session.team(teamDefId).isEmpty()) {
            createTeam(def, session, teamDefId);
        }
    }

    private TeamDefinition findTeamDef(EventDefinition def, String id) {
        return def.teamDefinitions().stream().filter(t -> t.id().equals(id)).findFirst().orElse(null);
    }

    private int findMax(EventDefinition def, String teamDefId) {
        var t = findTeamDef(def, teamDefId);
        return t != null ? t.maximumPlayers() : Integer.MAX_VALUE;
    }

    private static long seed(Instant now) { return now.toEpochMilli(); }
}
