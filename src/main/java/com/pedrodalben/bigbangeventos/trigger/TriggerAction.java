package com.pedrodalben.bigbangeventos.trigger;
import java.util.*;
public record TriggerAction(ActionType type, Map<String,String> arguments) { public TriggerAction { arguments=Map.copyOf(arguments); } }
