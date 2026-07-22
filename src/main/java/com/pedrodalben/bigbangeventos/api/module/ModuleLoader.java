package com.pedrodalben.bigbangeventos.api.module;

import com.pedrodalben.bigbangeventos.BigBangEventos;
import com.pedrodalben.bigbangeventos.api.BigBangEventosApi;
import com.pedrodalben.bigbangeventos.eventtype.EventTypeRegistry;
import com.pedrodalben.bigbangeventos.platform.PlatformPlayerService;
import com.pedrodalben.bigbangeventos.platform.PlatformScheduler;
import com.pedrodalben.bigbangeventos.platform.PlatformTeleportService;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;
import com.pedrodalben.bigbangeventos.objective.*;
import com.pedrodalben.bigbangeventos.stage.StageService;
import com.pedrodalben.bigbangeventos.domain.DomainEventBus;
import com.pedrodalben.bigbangeventos.data.TypedDataService;

public final class ModuleLoader {

    public static final int API_VERSION = 3;

    private static final Logger LOG = LoggerFactory.getLogger("BigBangEventos.Modules");
    private static final String ENTRYPOINT_KEY = "bigbangeventos:event_module";

    private final List<LoadedModule> loaded = new ArrayList<>();

    public void discoverAndLoad() {
        var containers = FabricLoader.getInstance().getEntrypointContainers(ENTRYPOINT_KEY, BigBangEventModule.class);

        if (containers.isEmpty()) {
            LOG.info("Nenhum módulo de evento encontrado.");
            return;
        }

        for (var container : containers) {
            BigBangEventModule module = container.getEntrypoint();
            String id = module.id();
            String name = module.name();
            String version = module.version();
            int reqApi = module.requiredApiVersion();

            if (id == null || id.isBlank()) {
                LOG.error("Módulo {} ignorado: ID inválido", container.getProvider().getMetadata().getId());
                continue;
            }

            if (loaded.stream().anyMatch(lm -> lm.module().id().equals(id))) {
                LOG.error("Módulo '{}' ignorado: ID duplicado", id);
                continue;
            }

            if (reqApi > API_VERSION) {
                LOG.error("Módulo '{}' (v{}) requer API v{}, mas BigBangEventos fornece API v{}. Módulo desativado.",
                        name, version, reqApi, API_VERSION);
                continue;
            }

            EventModuleContext context = createContext(container);
            try {
                module.onLoad(context);
                module.onEnable(context);
                loaded.add(new LoadedModule(module, context));
                LOG.info("Módulo '{}' (v{}) carregado com sucesso.", name, version);
                LOG.info("Tipos de evento disponíveis: {}", BigBangEventos.engine().types().all().stream().map(com.pedrodalben.bigbangeventos.eventtype.EventType::id).sorted().toList());
            } catch (Exception e) {
                LOG.error("Falha ao carregar módulo '{}' (v{}): {}", name, version, e.getMessage(), e);
            }
        }
    }

    public void disableAll() {
        for (LoadedModule lm : loaded) {
            try {
                lm.module().onDisable(lm.context());
                LOG.info("Módulo '{}' desabilitado.", lm.module().name());
            } catch (Exception e) {
                LOG.error("Falha ao desabilitar módulo '{}': {}", lm.module().name(), e.getMessage());
            }
        }
        loaded.clear();
    }

    public List<BigBangEventModule> modules() {
        return loaded.stream().map(LoadedModule::module).toList();
    }

    private EventModuleContext createContext(EntrypointContainer<BigBangEventModule> container) {
        Path configDir = FabricLoader.getInstance().getConfigDir()
                .resolve("bigbangeventos").resolve("modules")
                .resolve(container.getEntrypoint().id());
        return new DefaultModuleContext(
                BigBangEventos.api(),
                BigBangEventos.engine().types(),
                BigBangEventos.scheduler(),
                BigBangEventos.engine().players(),
                BigBangEventos.engine().teleport(),
                configDir,
                container.getEntrypoint().id()
        );
    }

    private record LoadedModule(BigBangEventModule module, EventModuleContext context) {}

    private static final class DefaultModuleContext implements EventModuleContext {
        private final BigBangEventosApi api;
        private final EventTypeRegistry eventTypes;
        private final PlatformScheduler scheduler;
        private final PlatformPlayerService players;
        private final PlatformTeleportService teleport;
        private final Path configDirectory;
        private final EventModuleLogger logger;
        private final ObjectiveTypeRegistry objectives;
        private final ObjectiveService objectiveService;
        private final StageService stageService;
        private final DomainEventBus events;
        private final TypedDataService data;

        DefaultModuleContext(BigBangEventosApi api, EventTypeRegistry eventTypes,
                PlatformScheduler scheduler, PlatformPlayerService players,
                             PlatformTeleportService teleport, Path configDirectory, String moduleId) {
            this.api = api;
            this.eventTypes = eventTypes;
            this.scheduler = scheduler;
            this.players = players;
            this.teleport = teleport;
            this.configDirectory = configDirectory;
            this.objectives = BigBangEventos.engine().objectiveTypes();
            this.objectiveService = BigBangEventos.engine().objectives();
            this.stageService = BigBangEventos.engine().stages();
            this.events = BigBangEventos.engine().events();
            this.data = BigBangEventos.engine().data();
            this.logger = new EventModuleLogger() {
                private final Logger log = LoggerFactory.getLogger("Module." + moduleId);
                public void info(String message) { log.info(message); }
                public void info(String format, Object... args) { log.info(format, args); }
                public void warn(String message) { log.warn(message); }
                public void warn(String format, Object... args) { log.warn(format, args); }
                public void error(String message) { log.error(message); }
                public void error(String format, Object... args) { log.error(format, args); }
                public void debug(String message) { log.debug(message); }
                public void debug(String format, Object... args) { log.debug(format, args); }
            };
        }

        public BigBangEventosApi api() { return api; }
        public EventTypeRegistry eventTypes() { return eventTypes; }
        public PlatformScheduler scheduler() { return scheduler; }
        public PlatformPlayerService players() { return players; }
        public PlatformTeleportService teleport() { return teleport; }
        public EventModuleLogger logger() { return logger; }
        public Path configDirectory() { return configDirectory; }
        public ObjectiveTypeRegistry objectives(){return objectives;}
        public ObjectiveService objectiveService(){return objectiveService;}
        public StageService stageService(){return stageService;}
        public DomainEventBus events(){return events;}
        public TypedDataService data(){return data;}
    }
}
