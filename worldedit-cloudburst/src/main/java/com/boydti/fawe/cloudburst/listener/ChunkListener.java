package com.boydti.fawe.cloudburst.listener;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.cloudburst.FaweCloudburst;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.util.FaweTimer;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.TaskManager;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.EntityTypes;
import org.cloudburstmc.server.event.EventHandler;
import org.cloudburstmc.server.event.EventPriority;
import org.cloudburstmc.server.event.Listener;
import org.cloudburstmc.server.event.block.*;
import org.cloudburstmc.server.event.entity.EntityBlockChangeEvent;
import org.cloudburstmc.server.event.entity.ItemSpawnEvent;
import org.cloudburstmc.server.event.inventory.FurnaceBurnEvent;
import org.cloudburstmc.server.event.inventory.FurnaceSmeltEvent;
import org.cloudburstmc.server.event.level.ChunkLoadEvent;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.chunk.Chunk;
import org.cloudburstmc.server.plugin.Plugin;
import org.cloudburstmc.server.plugin.PluginManager;
import org.slf4j.Logger;

import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

public abstract class ChunkListener implements Listener {

    private final Logger logger = getLogger(ChunkListener.class);
    protected int rateLimit = 0;
    protected Vector3i lastCancelPos;
    private int[] badLimit = new int[]{Settings.IMP.TICK_LIMITER.PHYSICS_MS,
            Settings.IMP.TICK_LIMITER.FALLING, Settings.IMP.TICK_LIMITER.ITEMS};

    public ChunkListener() {
        if (Settings.IMP.TICK_LIMITER.ENABLED) {
            PluginManager plm = Server.getInstance().getPluginManager();
            Plugin plugin = Fawe.<FaweCloudburst>imp().getPlugin();
            plm.registerEvents(this, plugin);
            try {
                plm.registerEvents(new ChunkListener8Plus(this), plugin);
            } catch (Throwable ignored) {
            }
            TaskManager.IMP.repeat(() -> {
                Vector3i tmpLoc = lastCancelPos;
                if (tmpLoc != null) {
                    logger.debug("[FAWE Tick Limiter] Detected and cancelled physics lag source at "
                            + tmpLoc);
                }
                rateLimit--;
                physicsFreeze = false;
                itemFreeze = false;
                lastZ = Integer.MIN_VALUE;
                physSkip = 0;
                physCancelPair = Long.MIN_VALUE;
                physCancel = false;
                lastCancelPos = null;

                counter.clear();
                for (Long2ObjectMap.Entry<Boolean> entry : badChunks.long2ObjectEntrySet()) {
                    long key = entry.getLongKey();
                    int x = MathMan.unpairIntX(key);
                    int z = MathMan.unpairIntY(key);
                    counter.put(key, badLimit);
                }
                badChunks.clear();
            }, Settings.IMP.TICK_LIMITER.INTERVAL);
        }
    }

    protected abstract int getDepth(Exception ex);

    protected abstract StackTraceElement getElement(Exception ex, int index);

    public static boolean physicsFreeze = false;
    public static boolean itemFreeze = false;

    protected final Long2ObjectOpenHashMap<Boolean> badChunks = new Long2ObjectOpenHashMap<>();
    private Long2ObjectOpenHashMap<int[]> counter = new Long2ObjectOpenHashMap<>();
    private int lastX = Integer.MIN_VALUE;
    private int lastZ = Integer.MIN_VALUE;
    private int[] lastCount;

    public int[] getCount(int cx, int cz) {
        if (lastX == cx && lastZ == cz) {
            return lastCount;
        }
        lastX = cx;
        lastZ = cz;
        long pair = MathMan.pairInt(cx, cz);
        int[] tmp = lastCount = counter.get(pair);
        if (tmp == null) {
            lastCount = tmp = new int[3];
            counter.put(pair, tmp);
        }
        return tmp;
    }

    public void cleanup(Chunk chunk) {
        for (Entity entity : chunk.getEntities()) {
            if (entity.getType() == EntityTypes.ITEM) {
                entity.close();
            }
        }

    }

    protected int physSkip;
    protected boolean physCancel;
    protected long physCancelPair;

    protected long physStart;
    protected long physTick;

    public final void reset() {
        physSkip = 0;
        physStart = System.currentTimeMillis();
        physCancel = false;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockBurnEvent event) {
        reset();
    }

//    @EventHandler(priority = EventPriority.LOWEST)
//    public void event(BlockCanBuildEvent event) {
//        reset();
//    }
//
//    @EventHandler(priority = EventPriority.LOWEST)
//    public void event(BlockDamageEvent event) {
//        reset();
//    }
//
//    @EventHandler(priority = EventPriority.LOWEST)
//    public void event(BlockDispenseEvent event) {
//        reset();
//    }
//
//    @EventHandler(priority = EventPriority.LOWEST)
//    public void event(BlockExpEvent event) {
//        reset();
//    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockFadeEvent event) {
        reset();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockFromToEvent event) {
        reset();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockGrowEvent event) {
        reset();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockIgniteEvent event) {
        reset();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockPlaceEvent event) {
        reset();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(FurnaceBurnEvent event) {
        reset();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(FurnaceSmeltEvent event) {
        reset();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(LeavesDecayEvent event) {
        reset();
    }

//    @EventHandler(priority = EventPriority.LOWEST)
//    public void event(NotePlayEvent event) {
//        reset();
//    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(SignChangeEvent event) {
        reset();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockRedstoneEvent event) {
        reset();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPhysics(BlockUpdateEvent event) {
        if (physicsFreeze) {
            event.setCancelled(true);
            return;
        }
        if (physCancel) {
            Block block = event.getBlock();
            long pair = MathMan.pairInt(block.getX() >> 4, block.getZ() >> 4);
            if (physCancelPair == pair) {
                event.setCancelled(true);
                return;
            }
            if (badChunks.containsKey(pair)) {
                physCancelPair = pair;
                event.setCancelled(true);
                return;
            }
        } else {
            if ((++physSkip & 1023) != 0) {
                return;
            }
            FaweTimer timer = Fawe.get().getTimer();
            if (timer.getTick() != physTick) {
                physTick = timer.getTick();
                physStart = System.currentTimeMillis();
                return;
            } else if (System.currentTimeMillis() - physStart
                    < Settings.IMP.TICK_LIMITER.PHYSICS_MS) {
                return;
            }
        }
        Exception e = new Exception();
        int depth = getDepth(e);
        if (depth >= 256) {
            if (containsSetAir(e, event)) {
                Block block = event.getBlock();
                int cx = block.getX() >> 4;
                int cz = block.getZ() >> 4;
                physCancelPair = MathMan.pairInt(cx, cz);
                if (rateLimit <= 0) {
                    rateLimit = 20;
                    lastCancelPos = block.getPosition();
                }
                cancelNearby(cx, cz);
                event.setCancelled(true);
                physCancel = true;
                return;
            }
        }
        physSkip = 1;
        physCancel = false;
    }

    protected boolean containsSetAir(Exception e, BlockUpdateEvent event) {
        for (int frame = 25; frame < 35; frame++) {
            StackTraceElement elem = getElement(e, frame);
            if (elem != null) {
                String methodName = elem.getMethodName();
                // setAir | setTypeAndData (hacky, but this needs to be efficient)
                if (methodName.charAt(0) == 's' && methodName.length() == 6
                        || methodName.length() == 14) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void cancelNearby(int cx, int cz) {
        cancel(cx, cz);
        cancel(cx + 1, cz);
        cancel(cx - 1, cz);
        cancel(cx, cz + 1);
        cancel(cx, cz - 1);
        cancel(cx - 1, cz - 1);
        cancel(cx - 1, cz + 1);
        cancel(cx + 1, cz - 1);
        cancel(cx + 1, cz + 1);
    }

    private void cancel(int cx, int cz) {
        long key = MathMan.pairInt(cx, cz);
        badChunks.put(key, (Boolean) true);
        counter.put(key, badLimit);
        int[] count = getCount(cx, cz);
        count[0] = Integer.MAX_VALUE;
        count[1] = Integer.MAX_VALUE;
        count[2] = Integer.MAX_VALUE;

    }

    // Falling
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockChange(EntityBlockChangeEvent event) {
        if (physicsFreeze) {
            event.setCancelled(true);
            return;
        }
        Block block = event.getBlock();
        int x = block.getX();
        int z = block.getZ();
        int cx = x >> 4;
        int cz = z >> 4;
        int[] count = getCount(cx, cz);
        if (count[1] >= Settings.IMP.TICK_LIMITER.FALLING) {
            event.setCancelled(true);
            return;
        }
        if (event.getEntity().getType() == EntityTypes.FALLING_BLOCK) {
            if (++count[1] >= Settings.IMP.TICK_LIMITER.FALLING) {

                // Only cancel falling blocks when it's lagging
                if (Fawe.get().getTimer().getTPS() < 18) {
                    cancelNearby(cx, cz);
                    if (rateLimit <= 0) {
                        rateLimit = 20;
                        lastCancelPos = block.getPosition();
                    }
                    event.setCancelled(true);
                } else {
                    count[1] = 0;
                }
            }
        }
    }

    /**
     * Prevent FireWorks from loading chunks.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!Settings.IMP.TICK_LIMITER.FIREWORKS_LOAD_CHUNKS) {
            Chunk chunk = event.getChunk();
            Set<Entity> entities = chunk.getEntities();
            Level world = chunk.getLevel();

            Exception e = new Exception();
            int start = 14;
            int end = 22;
            int depth = Math.min(end, getDepth(e));

            for (int frame = start; frame < depth; frame++) {
                StackTraceElement elem = getElement(e, frame);
                if (elem == null) {
                    return;
                }
                String className = elem.getClassName();
                int len = className.length();
                if (len > 15 && className.charAt(len - 15) == 'E' && className
                        .endsWith("EntityFireworks")) {
                    for (Entity ent : world.getEntities()) {
                        if (ent.getType() == EntityTypes.FIREWORKS_ROCKET) {
                            Vector3f velocity = ent.getMotion();
                            double vertical = Math.abs(velocity.getY());
                            if (Math.abs(velocity.getX()) > vertical
                                    || Math.abs(velocity.getZ()) > vertical) {
                                logger.warn(
                                        "[FAWE `tick-limiter`] Detected and cancelled rogue FireWork at "
                                                + ent.getLocation());
                                ent.close();
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (physicsFreeze) {
            event.setCancelled(true);
            return;
        }
        Entity entity = event.getEntity();
        Vector3f vec = entity.getPosition();
        int cx = vec.getFloorX() >> 4;
        int cz = vec.getFloorZ() >> 4;
        int[] count = getCount(cx, cz);
        if (count[2] >= Settings.IMP.TICK_LIMITER.ITEMS) {
            event.setCancelled(true);
            return;
        }
        if (++count[2] >= Settings.IMP.TICK_LIMITER.ITEMS) {
            cleanup(entity.getLevel().getChunk(vec));
            cancelNearby(cx, cz);
            if (rateLimit <= 0) {
                rateLimit = 20;
                logger.warn(
                        "[FAWE `tick-limiter`] Detected and cancelled item lag source at " + vec);
            }
            event.setCancelled(true);
        }
    }
}
