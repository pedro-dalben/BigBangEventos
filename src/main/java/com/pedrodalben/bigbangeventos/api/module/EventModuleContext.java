package com.pedrodalben.bigbangeventos.api.module;

import com.pedrodalben.bigbangeventos.api.BigBangEventosApi;
import com.pedrodalben.bigbangeventos.eventtype.EventTypeRegistry;
import com.pedrodalben.bigbangeventos.platform.PlatformPlayerService;
import com.pedrodalben.bigbangeventos.platform.PlatformScheduler;
import com.pedrodalben.bigbangeventos.platform.PlatformTeleportService;

public interface EventModuleContext {

    BigBangEventosApi api();

    EventTypeRegistry eventTypes();

    PlatformScheduler scheduler();

    PlatformPlayerService players();

    PlatformTeleportService teleport();

    EventModuleLogger logger();

    java.nio.file.Path configDirectory();
}
