# Criando tipos de evento

Implemente `EventType`, registre uma instância no `EventTypeRegistry` durante o bootstrap e valide `EventDefinition.typeSettings()`. O tipo não deve editar o estado diretamente: use os serviços do motor. O próximo tipo `parkour` pode usar checkpoints do participante, cronômetro individual, gatilho de chegada e `Rankings.TIME_ASCENDING`.
