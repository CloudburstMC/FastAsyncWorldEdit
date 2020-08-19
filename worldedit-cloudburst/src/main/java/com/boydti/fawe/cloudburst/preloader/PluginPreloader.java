package com.boydti.fawe.cloudburst.preloader;

import com.boydti.fawe.Fawe;
import com.sk89q.worldedit.cloudburst.CloudburstAdapter;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.regions.Region;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandSender;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.plugin.PluginBase;
import org.cloudburstmc.server.plugin.PluginLoader;
import org.cloudburstmc.server.utils.Config;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class PluginPreloader extends PluginBase {
    private Level world;
    private Set<BlockVector2> loaded;
    private int index;
    private AtomicBoolean invalidator;
    private final Object invalidatorLock;

    public PluginPreloader() {
        invalidator = new AtomicBoolean();
        invalidatorLock = new Object();
    }

    public AtomicBoolean invalidate() {
        synchronized (invalidatorLock) {
            invalidator.set(false);
            return invalidator = new AtomicBoolean(true);
        }
    }

    private synchronized void unload() {
        Level oldWorld = world;
        if (oldWorld != null) {
            Set<BlockVector2> toUnload = loaded;
            if (loaded != null && index > 0) {
                Iterator<BlockVector2> iter = toUnload.iterator();
                Fawe.get().getQueueHandler().sync(() -> {
                    for (int i = 0; i < index && iter.hasNext(); i++) {
                        BlockVector2 chunk = iter.next();
                        // FIXME: Not sure what this does but we need to add support
//                        world.removePluginChunkTicket(chunk.getX(), chunk.getZ(), this);
                    }
                });
            }
        }
        this.world = null;
        this.loaded = null;
        this.index = 0;
    }

    public void update(Region region) {
        AtomicBoolean invalidator = invalidate();
        synchronized (this) {
            com.sk89q.worldedit.world.World weWorld = region.getWorld();
            if (weWorld == null) {
                return;
            }
            unload();
            index = 0;
            world = CloudburstAdapter.adapt(weWorld);
            loaded = region.getChunks();
            Iterator<BlockVector2> iter = loaded.iterator();

            if (!invalidator.get()) {
                return;
            }
            Fawe.get().getQueueHandler().syncWhenFree(() -> {
                for (; iter.hasNext() && invalidator.get(); index++) {
                    BlockVector2 chunk = iter.next();
                    if (!world.isChunkLoaded(chunk.getX(), chunk.getZ())) {
                        // FIXME: Not sure what this does but we need to add support
//                        world.addPluginChunkTicket(chunk.getX(), chunk.getZ(), this);
                    }
                }
            });
        }
    }

    public void clear() {
        invalidate();
        unload();
    }

    @Override
    public @NotNull Config getConfig() {
        return null;
    }

    @Override
    public @Nullable InputStream getResource(@NotNull String filename) {
        return null;
    }

    @Override
    public void saveConfig() {
    }

    @Override
    public void saveDefaultConfig() {
    }

    @Override
    public boolean saveResource(@NotNull String resourcePath, boolean replace) {
        return false;
    }

    @Override
    public void reloadConfig() {
    }

    @Override
    public @NotNull PluginLoader getPluginLoader() {
        return null;
    }

    @Override
    public @NotNull Server getServer() {
        return null;
    }

    @Override
    public void onDisable() {

    }

    @Override
    public void onLoad() {

    }

    @Override
    public void onEnable() {

    }

    @Override
    public @NotNull Logger getLogger() {
        return null;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        return false;
    }
}
