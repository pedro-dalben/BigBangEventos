package com.pedrodalben.bigbangeventos.api.module;

public interface BigBangEventModule {

    String id();

    String name();

    String version();

    int requiredApiVersion();

    default void onLoad(EventModuleContext context) {}

    default void onEnable(EventModuleContext context) {}

    default void onDisable(EventModuleContext context) {}
}
