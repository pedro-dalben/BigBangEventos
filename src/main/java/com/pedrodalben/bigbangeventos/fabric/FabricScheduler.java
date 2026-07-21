package com.pedrodalben.bigbangeventos.fabric;

import com.pedrodalben.bigbangeventos.platform.PlatformScheduler;
import net.minecraft.server.MinecraftServer;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class FabricScheduler implements PlatformScheduler {
    private final MinecraftServer server;
    private final Queue<Runnable> pending = new ConcurrentLinkedQueue<>();
    private final List<ScheduledTask> scheduled = Collections.synchronizedList(new ArrayList<>());
    private int tickCount;

    public FabricScheduler(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public boolean isServerThread() {
        return server.isSameThread();
    }

    @Override
    public void executeOnServerThread(Runnable action) {
        if (isServerThread()) {
            action.run();
        } else {
            pending.add(action);
        }
    }

    @Override
    public ScheduledHandle schedule(Duration delay, Runnable action) {
        int targetTick = tickCount + ticksFromDuration(delay);
        ScheduledTask task = new ScheduledTask(action, null, targetTick);
        scheduled.add(task);
        return task;
    }

    @Override
    public ScheduledHandle scheduleRepeating(Duration interval, Runnable action) {
        int intervalTicks = ticksFromDuration(interval);
        ScheduledTask task = new ScheduledTask(action, intervalTicks, tickCount + intervalTicks);
        scheduled.add(task);
        return task;
    }

    public void onTick() {
        tickCount++;
        while (!pending.isEmpty()) {
            Runnable r = pending.poll();
            if (r != null) {
                try { r.run(); } catch (Exception e) { /* log */ }
            }
        }
        synchronized (scheduled) {
            var iter = scheduled.iterator();
            while (iter.hasNext()) {
                ScheduledTask task = iter.next();
                if (task.cancelled) { iter.remove(); continue; }
                if (tickCount >= task.nextTick) {
                    try { task.action.run(); }
                    catch (Exception e) { /* log */ }
                    if (task.intervalTicks != null) {
                        task.nextTick = tickCount + task.intervalTicks;
                    } else {
                        iter.remove();
                    }
                }
            }
        }
    }

    private static int ticksFromDuration(Duration d) {
        return (int) Math.max(1, d.toMillis() / 50);
    }

    private static final class ScheduledTask implements ScheduledHandle {
        final Runnable action;
        final Integer intervalTicks;
        int nextTick;
        boolean cancelled;

        ScheduledTask(Runnable action, Integer intervalTicks, int nextTick) {
            this.action = action;
            this.intervalTicks = intervalTicks;
            this.nextTick = nextTick;
        }

        @Override
        public void cancel() { cancelled = true; }

        @Override
        public boolean isCancelled() { return cancelled; }
    }
}
