package br.com.bigbangcraft.eventos.parkour;

import br.com.bigbangcraft.eventos.parkour.model.ParkourCheckpoint;
import br.com.bigbangcraft.eventos.parkour.model.ParkourParticipantData;
import com.pedrodalben.bigbangeventos.api.BigBangEventosApi;
import com.pedrodalben.bigbangeventos.definition.EventDefinition;
import com.pedrodalben.bigbangeventos.definition.EventLocation;
import com.pedrodalben.bigbangeventos.session.EventSession;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class ParkourCheckpointService {

    public static final String CHECKPOINTS_KEY = "parkour.checkpoints";

    private final BigBangEventosApi api;
    private final ParkourParticipantService participantService;
    private final Gson gson = new Gson();
    private final Type listType = new TypeToken<List<CheckpointData>>() {}.getType();

    private static final class CheckpointData {
        String id; int order; double x, y, z; float yaw, pitch;
        double radius; String serverId; String dimension; boolean enabled;

        CheckpointData() {}
    }

    public ParkourCheckpointService(BigBangEventosApi api, ParkourParticipantService participantService) {
        this.api = api;
        this.participantService = participantService;
    }

    public List<ParkourCheckpoint> getCheckpoints(String eventId) {
        EventDefinition def = api.findEvent(eventId).orElse(null);
        if (def == null) return List.of();
        return loadFromDef(def);
    }

    public void setCheckpoints(String eventId, List<ParkourCheckpoint> checkpoints) {
        EventDefinition def = api.findEvent(eventId).orElse(null);
        if (def == null) return;
        saveToDef(def, checkpoints);
        api.saveEvent(def);
    }

    public void addCheckpoint(String eventId, ParkourCheckpoint cp) {
        List<ParkourCheckpoint> list = new ArrayList<>(getCheckpoints(eventId));
        list.add(cp);
        setCheckpoints(eventId, list);
    }

    public void removeCheckpoint(String eventId, String checkpointId) {
        List<ParkourCheckpoint> list = getCheckpoints(eventId).stream()
                .filter(c -> !c.id().equals(checkpointId))
                .collect(Collectors.toList());
        setCheckpoints(eventId, list);
    }

    public Optional<ParkourCheckpoint> findCheckpoint(String eventId, String checkpointId) {
        return getCheckpoints(eventId).stream()
                .filter(c -> c.id().equals(checkpointId))
                .findFirst();
    }

    public Optional<ParkourCheckpoint> getNextCheckpoint(String eventId, String sessionId, String playerId) {
        List<ParkourCheckpoint> checkpoints = getCheckpoints(eventId);
        if (checkpoints.isEmpty()) return Optional.empty();
        ParkourParticipantData data = participantService.getOrCreate(sessionId, playerId);
        String current = data.currentCheckpointId();
        if (current == null) return Optional.of(checkpoints.get(0));
        boolean found = false;
        for (ParkourCheckpoint cp : checkpoints) {
            if (found) return Optional.of(cp);
            if (cp.id().equals(current)) found = true;
        }
        return Optional.empty();
    }

    public boolean isExpectedCheckpoint(String eventId, String sessionId, String playerId, String checkpointId) {
        return getNextCheckpoint(eventId, sessionId, playerId)
                .map(cp -> cp.id().equals(checkpointId))
                .orElse(false);
    }

    public boolean hasCompletedAll(String eventId, String sessionId, String playerId) {
        List<ParkourCheckpoint> checkpoints = getCheckpoints(eventId);
        if (checkpoints.isEmpty()) return false;
        ParkourParticipantData data = participantService.getOrCreate(sessionId, playerId);
        return data.completedCheckpointIds().containsAll(
                checkpoints.stream().map(ParkourCheckpoint::id).collect(Collectors.toSet()));
    }

    public String getCurrentCheckpointId(String sessionId, String playerId) {
        return participantService.getOrCreate(sessionId, playerId).currentCheckpointId();
    }

    public boolean completeCheckpoint(String eventId, String sessionId, String playerId, String checkpointId) {
        ParkourParticipantData data = participantService.getOrCreate(sessionId, playerId);
        if (data.hasCompletedCheckpoint(checkpointId)) return false;
        if (!isExpectedCheckpoint(eventId, sessionId, playerId, checkpointId)) return false;
        data.addCompletedCheckpoint(checkpointId);
        data.currentCheckpointId(checkpointId);
        return true;
    }

    public int checkpointCount(String eventId) {
        return getCheckpoints(eventId).size();
    }

    public boolean hasCheckpoints(String eventId) {
        return !getCheckpoints(eventId).isEmpty();
    }

    private List<ParkourCheckpoint> loadFromDef(EventDefinition def) {
        Object raw = def.typeSettings().get(CHECKPOINTS_KEY);
        if (raw instanceof String json && !json.isEmpty()) {
            try {
                List<CheckpointData> cdList = gson.fromJson(json, listType);
                if (cdList == null) return List.of();
                List<ParkourCheckpoint> result = new ArrayList<>();
                for (CheckpointData cd : cdList) {
                    EventLocation loc = new EventLocation(cd.serverId, cd.dimension, cd.x, cd.y, cd.z, cd.yaw, cd.pitch);
                    ParkourCheckpoint cp = new ParkourCheckpoint(cd.id, cd.order, loc, cd.radius);
                    cp.enabled(cd.enabled);
                    result.add(cp);
                }
                return result;
            } catch (Exception e) {
                return List.of();
            }
        }
        return List.of();
    }

    private void saveToDef(EventDefinition def, List<ParkourCheckpoint> checkpoints) {
        List<CheckpointData> cdList = new ArrayList<>();
        for (ParkourCheckpoint cp : checkpoints) {
            CheckpointData cd = new CheckpointData();
            cd.id = cp.id();
            cd.order = cp.order();
            cd.x = cp.location().x();
            cd.y = cp.location().y();
            cd.z = cp.location().z();
            cd.yaw = cp.location().yaw();
            cd.pitch = cp.location().pitch();
            cd.radius = cp.radius();
            cd.serverId = cp.location().serverId();
            cd.dimension = cp.location().dimension();
            cd.enabled = cp.enabled();
            cdList.add(cd);
        }
        def.typeSetting(CHECKPOINTS_KEY, gson.toJson(cdList));
    }
}
