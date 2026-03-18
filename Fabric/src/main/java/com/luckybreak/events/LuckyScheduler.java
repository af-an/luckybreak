package com.luckybreak.events;

import com.luckybreak.LuckyBreak;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Simple tick-based task scheduler for delayed Lucky Block effects.
 * Register once on init; then call schedule(delayTicks, runnable) from any event.
 */
public class LuckyScheduler {

    public static final LuckyScheduler INSTANCE = new LuckyScheduler();

    private final List<ScheduledTask> tasks = new ArrayList<>();
    private final List<ScheduledTask> pending = new ArrayList<>();
    private boolean ticking;

    private LuckyScheduler() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> INSTANCE.tick());
    }

    /** Schedule {@code task} to run after {@code delayTicks} server ticks. */
    public void schedule(int delayTicks, Runnable task) {
        ScheduledTask scheduled = new ScheduledTask(Math.max(1, delayTicks), task);
        if (ticking) {
            pending.add(scheduled);
        } else {
            tasks.add(scheduled);
        }
    }

    private void tick() {
        if (tasks.isEmpty() && pending.isEmpty()) return;

        ticking = true;
        try {
            Iterator<ScheduledTask> it = tasks.iterator();
            while (it.hasNext()) {
                ScheduledTask task = it.next();
                task.ticksRemaining--;
                if (task.ticksRemaining <= 0) {
                    it.remove();
                    try {
                        task.runnable.run();
                    } catch (Exception e) {
                        LuckyBreak.LOGGER.error("[LuckyBreak] Scheduled task threw an exception: {}", e.getMessage());
                    }
                }
            }
        } finally {
            ticking = false;
        }

        if (!pending.isEmpty()) {
            tasks.addAll(pending);
            pending.clear();
        }
    }

    private static class ScheduledTask {
        int ticksRemaining;
        final Runnable runnable;

        ScheduledTask(int delayTicks, Runnable runnable) {
            this.ticksRemaining = delayTicks;
            this.runnable = runnable;
        }
    }
}
