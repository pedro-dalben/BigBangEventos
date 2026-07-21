# Desenvolvimento de EventType

## Interface EventType

```java
package com.pedrodalben.bigbangeventos.eventtype;

public interface EventType {
    String id();
    String displayName();
    default ValidationResult validate(EventDefinition definition) { return ValidationResult.empty(); }
    default void onSessionCreated(EventSession session) { }
    default void onRegistrationOpen(EventSession session) { }
    default void onSessionStart(EventSession session) { }
    default void onSessionFinish(EventSession session) { }
    default void onSessionCancel(EventSession session, String reason) { }
}
```

## Implementação Mínima

```java
package com.meumod;

import com.pedrodalben.bigbangeventos.eventtype.EventType;

public class MeuTipoEvento implements EventType {
    @Override
    public String id() {
        return "meu_tipo";
    }

    @Override
    public String displayName() {
        return "Meu Tipo de Evento";
    }
}
```

## Validação Customizada

Valide as configurações específicas do tipo:

```java
@Override
public ValidationResult validate(EventDefinition definition) {
    ValidationResult r = ValidationResult.empty();
    Map<String, Object> settings = definition.typeSettings();

    if (!settings.containsKey("tempo_limite")) {
        r.add(ValidationLevel.ERROR, "missing_tempo", "tempo_limite é obrigatório");
    }

    Object tempo = settings.get("tempo_limite");
    if (tempo instanceof Number n && n.intValue() <= 0) {
        r.add(ValidationLevel.ERROR, "invalid_tempo", "tempo_limite deve ser positivo");
    }

    return r;
}
```

## Lifecycle Hooks

Use os hooks para controlar o evento:

```java
@Override
public void onSessionCreated(EventSession session) {
    // Sessão acabou de ser criada
    // Inicialize dados do tipo específico
}

@Override
public void onRegistrationOpen(EventSession session) {
    // Inscrições abertas
}

@Override
public void onSessionStart(EventSession session) {
    // Evento começou
    for (EventParticipant p : session.participants()) {
        p.data("inicio", Instant.now().toString());
    }
}

@Override
public void onSessionFinish(EventSession session) {
    // Evento terminou normalmente
    // Calcular ranking final
    Rankings.TIME_ASCENDING.rank(session);
}

@Override
public void onSessionCancel(EventSession session, String reason) {
    // Evento cancelado
}
```

## Type Settings

Configurações específicas do tipo ficam em `EventDefinition.typeSettings()`:

```java
definition.typeSetting("tempo_limite", 300); // segundos
definition.typeSetting("vidas", 3);
definition.typeSetting("checkpoints", List.of("cp1", "cp2"));
```

## Registro

Registre o tipo durante `onEnable`:

```java
public void onEnable(EventModuleContext ctx) {
    ctx.typeRegistry().register(new MeuTipoEvento());
}
```

## Boas Práticas

1. **Nunca acesse `EventEngine` diretamente.** Use `BigBangEventosApi`.
2. **Nunca modifique `EventStorage` diretamente.** O core salva automaticamente.
3. **Use `typeSettings()` para config.** Nunca crie definições parciais.
4. **Trate erros.** Use `try/catch` nos hooks; uma exceção não deve quebrar o core.
5. **Seja idempotente.** Hooks podem ser chamados múltiplas vezes.

## Exemplo Completo

Veja `modules/parkour/` para um exemplo real de implementação de
`EventType` com checkpoints, cronômetro e ranking.
