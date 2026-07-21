package com.pedrodalben.bigbangeventos.platform;

import java.time.Duration;

public interface PlatformScheduler {
    boolean isServerThread();
    void executeOnServerThread(Runnable action);
    ScheduledHandle schedule(Duration delay, Runnable action);
    ScheduledHandle scheduleRepeating(Duration interval, Runnable action);

    interface ScheduledHandle {
        void cancel();
        boolean isCancelled();
    }
}
