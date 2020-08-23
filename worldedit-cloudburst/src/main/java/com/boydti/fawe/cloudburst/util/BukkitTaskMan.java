package com.boydti.fawe.cloudburst.util;

import com.boydti.fawe.util.TaskManager;
import org.cloudburstmc.server.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class BukkitTaskMan extends TaskManager {

    private final Plugin plugin;

    public BukkitTaskMan(final Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public int repeat(@NotNull final Runnable runnable, final int interval) {
        return this.plugin.getServer().getScheduler().scheduleDelayedRepeatingTask(this.plugin, runnable, interval, interval, false).getTaskId();
    }

    @SuppressWarnings("deprecation")
    @Override
    public int repeatAsync(@NotNull final Runnable runnable, final int interval) {
        return this.plugin.getServer().getScheduler().scheduleDelayedRepeatingTask(this.plugin, runnable, interval, interval, true).getTaskId();
    }

    @Override
    public void async(@NotNull final Runnable runnable) {
        this.plugin.getServer().getScheduler().scheduleTask(this.plugin, runnable, true);
    }

    @Override
    public void task(@NotNull final Runnable runnable) {
        this.plugin.getServer().getScheduler().scheduleTask(this.plugin, runnable);
    }

    @Override
    public void later(@NotNull final Runnable runnable, final int delay) {
        this.plugin.getServer().getScheduler().scheduleDelayedTask(this.plugin, runnable, delay, false);
    }

    @Override
    public void laterAsync(@NotNull final Runnable runnable, final int delay) {
        this.plugin.getServer().getScheduler().scheduleDelayedTask(this.plugin, runnable, delay, true);
    }

    @Override
    public void cancel(final int task) {
        if (task != -1) {
            this.plugin.getServer().getScheduler().cancelTask(task);
        }
    }
}
