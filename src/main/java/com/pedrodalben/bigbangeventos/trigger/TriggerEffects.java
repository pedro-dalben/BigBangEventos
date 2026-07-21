package com.pedrodalben.bigbangeventos.trigger;
import java.util.UUID; public interface TriggerEffects { void message(UUID player,String message); default void executeConsole(String command) { } }
