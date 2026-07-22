package com.pedrodalben.bigbangeventos.domain;

import com.pedrodalben.bigbangeventos.platform.PlatformScheduler;
import org.slf4j.Logger;
import java.util.*;
import java.util.function.Consumer;

public final class DomainEventBus {
    private final PlatformScheduler scheduler;
    private final Logger logger;
    private final List<Subscription<?>> subscriptions = new ArrayList<>();
    public DomainEventBus(PlatformScheduler scheduler, Logger logger) { this.scheduler=scheduler;this.logger=logger; }
    public synchronized <T extends DomainEvent> Subscription subscribe(String owner, Class<T> type, Consumer<T> listener) {
        if (subscriptions.stream().anyMatch(s -> s.owner.equals(owner) && s.type.equals(type) && s.listener == listener))
            throw new IllegalArgumentException("listener duplicado");
        var sub = new Subscription<>(owner,type,listener); subscriptions.add(sub); return sub;
    }
    public synchronized void publish(DomainEvent event) {
        Runnable dispatch = () -> dispatchNow(event);
        if (scheduler.isServerThread()) dispatch.run(); else scheduler.executeOnServerThread(dispatch);
    }
    public synchronized void disable(String owner) { subscriptions.removeIf(s -> s.owner.equals(owner)); }
    private void dispatchNow(DomainEvent event) {
        List<Subscription<?>> copy;
        synchronized (this) { copy = subscriptions.stream().filter(s -> s.type.isInstance(event)).toList(); }
        for (Subscription<?> s : copy) try { s.accept(event); } catch (Exception e) { logger.error("Falha no listener do módulo '{}' para {}", s.owner, event.getClass().getSimpleName(), e); }
    }
    public final class Subscription<T extends DomainEvent> implements AutoCloseable {
        private final String owner; private final Class<T> type; private final Consumer<T> listener;
        private Subscription(String owner, Class<T> type, Consumer<T> listener){this.owner=Objects.requireNonNull(owner);this.type=Objects.requireNonNull(type);this.listener=Objects.requireNonNull(listener);}
        @SuppressWarnings("unchecked") private void accept(DomainEvent event){listener.accept((T)event);}
        public void close(){synchronized(DomainEventBus.this){subscriptions.remove(this);}}
    }
}
