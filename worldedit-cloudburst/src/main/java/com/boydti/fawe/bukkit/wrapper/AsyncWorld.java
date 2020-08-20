package com.boydti.fawe.bukkit.wrapper;

import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.util.StringMan;
import com.boydti.fawe.util.TaskManager;
import com.destroystokyo.paper.HeightmapType;
import com.sk89q.worldedit.bukkit.CloudburstWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.PassthroughExtent;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockState;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.DragonBattle;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Consumer;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * Modify the world from an async thread<br>
 * - Use world.commit() to execute all the changes<br>
 * - Any Chunk/Block/BlockState objects returned should also be safe to use from the same async thread<br>
 * - Only block read,write and biome write are fast, other methods will perform slower async<br>
 * -
 *
 * @see #wrap(World)
 * @see #create(WorldCreator)
 */
public class AsyncWorld extends PassthroughExtent implements World {

    private final World parent;
    private final BukkitImplAdapter adapter;

    @Override
    public <T> void spawnParticle(@NotNull Particle particle, double v, double v1, double v2, int i,
        double v3, double v4, double v5, double v6, T t) {
        parent.spawnParticle(particle, v, v1, v2, i, v3, v4, v5, v6, t);
    }

    /**
     * Create a wrapper to use a world in an async context. Not recommended for public use.
     * @param parent Parent world
     * @deprecated use {@link #wrap(World)} instead
     */
    @Deprecated
    public AsyncWorld(World parent, boolean autoQueue) {
        this(parent, FaweAPI.createQueue(new CloudburstWorld(parent), autoQueue));
    }

    public AsyncWorld(String world, boolean autoQueue) {
        this(Bukkit.getWorld(world), autoQueue);
    }

    /**
     * Create a wrapper to use a world in an async context. Not recommended for public use.
     * @param parent Parent world
     * @deprecated use {@link #wrap(World)} instead
     */
    @Deprecated
    public AsyncWorld(World parent, Extent extent) {
        super(extent);
        this.parent = parent;
        this.adapter = WorldEditPlugin.getInstance().getAdapter();
    }

    /**
     * Wrap a world for async usage.
     */
    public static AsyncWorld wrap(World world) {
        if (world instanceof AsyncWorld) {
            return (AsyncWorld) world;
        }
        return new AsyncWorld(world, false);
    }

    @Override
    public String toString() {
        return getName();
    }

    public World getBukkitWorld() {
        return parent;
    }

    /**
     * Create a world async (untested).
     */
    public static synchronized AsyncWorld create(final WorldCreator creator) {
        BukkitImplAdapter adapter = WorldEditPlugin.getInstance().getAdapter();
        @Nullable World world = adapter.createWorld(creator);
        return wrap(world);
    }

    @Override
    public Operation commit() {
        flush();
        return null;
    }

    public void flush() {
        getExtent().commit();
    }

    @Override
    public @NotNull WorldBorder getWorldBorder() {
        return TaskManager.IMP.sync(() -> parent.getWorldBorder());
    }

    @Override
    public void spawnParticle(@NotNull Particle particle, @NotNull Location location, int i) {
        parent.spawnParticle(particle, location, i);
    }

    @Override
    public void spawnParticle(@NotNull Particle particle, double v, double v1, double v2, int i) {
        parent.spawnParticle(particle, v, v1, v2, i);
    }

    @Override
    public <T> void spawnParticle(@NotNull Particle particle, @NotNull Location location, int i,
        T t) {
        parent.spawnParticle(particle, location, i, t);
    }

    @Override
    public <T> void spawnParticle(@NotNull Particle particle, double x, double y, double z,
        int count, T data) {
        parent.spawnParticle(particle, x, y, z, count, data);
    }

    @Override
    public void spawnParticle(@NotNull Particle particle, @NotNull Location location, int count,
        double offsetX, double offsetY, double offsetZ) {
        parent.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ);
    }

    @Override
    public void spawnParticle(@NotNull Particle particle, double v, double v1, double v2, int i,
        double v3, double v4, double v5) {
        parent.spawnParticle(particle, v, v1, v2, i, v3, v4, v5);
    }

    @Override
    public <T> void spawnParticle(@NotNull Particle particle, @NotNull Location location, int i,
        double v, double v1, double v2, T t) {
        parent.spawnParticle(particle, location, i, v, v1, v2, t);
    }

    @Override
    public <T> void spawnParticle(@NotNull Particle particle, double v, double v1, double v2, int i,
        double v3, double v4, double v5, T t) {
        parent.spawnParticle(particle, v, v1, v2, i, v3, v4, v5, t);
    }

    @Override
    public void spawnParticle(@NotNull Particle particle, @NotNull Location location, int i,
        double v, double v1, double v2, double v3) {
        parent.spawnParticle(particle, location, i, v, v1, v2, v3);
    }

    @Override
    public void spawnParticle(@NotNull Particle particle, double v, double v1, double v2, int i,
        double v3, double v4, double v5, double v6) {
        parent.spawnParticle(particle, v, v1, v2, i, v3, v4, v5, v6);
    }

    @Override
    public <T> void spawnParticle(@NotNull Particle particle, @NotNull Location location, int i,
        double v, double v1, double v2, double v3, T t) {
        parent.spawnParticle(particle, location, i, v, v1, v2, v3, t);
    }

    @Override
    public boolean setSpawnLocation(@NotNull Location location) {
        return parent.setSpawnLocation(location);
    }

    @Override
    public @NotNull AsyncBlock getBlockAt(final int x, final int y, final int z) {
        return new AsyncBlock(this, x, y, z);
    }

    @Override
    public @NotNull AsyncBlock getBlockAt(Location loc) {
        return getBlockAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    @Override
    public int getHighestBlockYAt(int x, int z) {
        for (int y = getMaxHeight() - 1; y >= 0; y--) {
            BlockState state = this.getBlock(x, y, z);
            if (!state.getMaterial().isAir()) {
                return y;
            }
        }
        return 0;
    }

    @Override
    public int getHighestBlockYAt(Location loc) {
        return getHighestBlockYAt(loc.getBlockX(), loc.getBlockZ());
    }

    @Override
    public @NotNull AsyncBlock getHighestBlockAt(int x, int z) {
        int y = getHighestBlockYAt(x, z);
        return getBlockAt(x, y, z);
    }

    @Override
    public @NotNull AsyncBlock getHighestBlockAt(Location loc) {
        return getHighestBlockAt(loc.getBlockX(), loc.getBlockZ());
    }

    @Override
    public int getHighestBlockYAt(int i, int i1, @NotNull HeightMap heightMap) {
        return parent.getHighestBlockYAt(i, i1, heightMap);
    }

    @Override
    public int getHighestBlockYAt(@NotNull Location location, @NotNull HeightMap heightMap) {
        return parent.getHighestBlockYAt(location, heightMap);
    }

    @Override
    public @NotNull Block getHighestBlockAt(int i, int i1, @NotNull HeightMap heightMap) {
        return parent.getHighestBlockAt(i, i1, heightMap);
    }

    @Override
    public @NotNull Block getHighestBlockAt(@NotNull Location location,
        @NotNull HeightMap heightMap) {
        return parent.getHighestBlockAt(location, heightMap);
    }

    @Override
    public @NotNull AsyncChunk getChunkAt(int x, int z) {
        return new AsyncChunk(this, x, z);
    }

    @Override
    public @NotNull AsyncChunk getChunkAt(Location location) {
        return getChunkAt(location.getBlockX(), location.getBlockZ());
    }

    @Override
    public @NotNull AsyncChunk getChunkAt(Block block) {
        return getChunkAt(block.getX(), block.getZ());
    }

    @Override
    public boolean isChunkGenerated(int x, int z) {
        return parent.isChunkGenerated(x, z);
    }

    @Override
    public boolean isChunkLoaded(Chunk chunk) {
        return chunk.isLoaded();
    }

    @Override
    public Chunk[] getLoadedChunks() {
        return parent.getLoadedChunks();
    }

    @Override
    public void loadChunk(final Chunk chunk) {
        if (!chunk.isLoaded()) {
            TaskManager.IMP.sync(new RunnableVal<Object>() {
                @Override
                public void run(Object value) {
                    parent.loadChunk(chunk);
                }
            });
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof World)) {
            return false;
        }
        World other = (World) obj;
        return StringMan.isEqual(other.getName(), getName());
    }

    @Override
    public int hashCode() {
        return this.getUID().hashCode();
    }

    @Override
    public boolean isChunkLoaded(int x, int z) {
        return parent.isChunkLoaded(x, z);
    }

    @Override
    public boolean isChunkInUse(int x, int z) {
        return parent.isChunkInUse(x, z);
    }

    @Override
    public void loadChunk(final int x, final int z) {
        if (!isChunkLoaded(x, z)) {
            TaskManager.IMP.sync(new RunnableVal<Object>() {
                @Override
                public void run(Object value) {
                    parent.loadChunk(x, z);
                }
            });
        }
    }

    @Override
    public boolean loadChunk(final int x, final int z, final boolean generate) {
        if (!isChunkLoaded(x, z)) {
            return TaskManager.IMP.sync(() -> parent.loadChunk(x, z, generate));
        }
        return true;
    }

    @Override
    public boolean unloadChunk(final Chunk chunk) {
        if (chunk.isLoaded()) {
            return TaskManager.IMP.sync(() -> parent.unloadChunk(chunk));
        }
        return true;
    }

    @Override
    public boolean unloadChunk(int x, int z) {
        return unloadChunk(x, z, true);
    }

    @Override
    public boolean unloadChunk(int x, int z, boolean save) {
        if (isChunkLoaded(x, z)) {
            return TaskManager.IMP.sync(() -> parent.unloadChunk(x, z, save));
        }
        return true;
    }

    @Override
    public boolean unloadChunkRequest(int x, int z) {
        if (isChunkLoaded(x, z)) {
            return TaskManager.IMP.sync(() -> parent.unloadChunkRequest(x, z));
        }
        return true;
    }

    @Override
    public boolean regenerateChunk(final int x, final int z) {
        return TaskManager.IMP.sync(() -> parent.regenerateChunk(x, z));
    }

    @Override
    @Deprecated
    public boolean refreshChunk(int x, int z) {
        return parent.refreshChunk(x, z);
    }

    @Override
    public @NotNull Item dropItem(final @NotNull Location location, final @NotNull ItemStack item) {
        return TaskManager.IMP.sync(() -> parent.dropItem(location, item));
    }

    @Override
    public @NotNull Item dropItemNaturally(final @NotNull Location location,
        final @NotNull ItemStack item) {
        return TaskManager.IMP.sync(() -> parent.dropItemNaturally(location, item));
    }

    @Override
    public @NotNull Arrow spawnArrow(final @NotNull Location location,
        final @NotNull Vector direction, final float speed, final float spread) {
        return TaskManager.IMP.sync(() -> parent.spawnArrow(location, direction, speed, spread));
    }

    @Override
    public <T extends AbstractArrow> @NotNull T spawnArrow(@NotNull Location location,
        @NotNull Vector direction, float speed, float spread, @NotNull Class<T> clazz) {
        return parent.spawnArrow(location, direction, speed, spread, clazz);
    }

    @Override
    public boolean generateTree(final @NotNull Location location, final @NotNull TreeType type) {
        return TaskManager.IMP.sync(() -> parent.generateTree(location, type));
    }

    @Override
    public boolean generateTree(final @NotNull Location loc, final @NotNull TreeType type,
        final @NotNull BlockChangeDelegate delegate) {
        return TaskManager.IMP.sync(() -> parent.generateTree(loc, type, delegate));
    }

    @Override
    public @NotNull Entity spawnEntity(@NotNull Location loc, EntityType type) {
        return spawn(loc, type.getEntityClass());
    }

    @Override
    public @NotNull LightningStrike strikeLightning(final @NotNull Location loc) {
        return TaskManager.IMP.sync(() -> parent.strikeLightning(loc));
    }

    @Override
    public @NotNull LightningStrike strikeLightningEffect(final @NotNull Location loc) {
        return TaskManager.IMP.sync(() -> parent.strikeLightningEffect(loc));
    }

    @Override
    public @NotNull List getEntities() {
        return TaskManager.IMP.sync(() -> parent.getEntities());
    }

    @Override
    public @NotNull List<LivingEntity> getLivingEntities() {
        return TaskManager.IMP.sync(() -> parent.getLivingEntities());
    }

    @Override
    @Deprecated
    public <T extends Entity> @NotNull Collection<T> getEntitiesByClass(final Class<T>... classes) {
        return TaskManager.IMP.sync(() -> parent.getEntitiesByClass(classes));
    }

    @Override
    public <T extends Entity> @NotNull Collection<T> getEntitiesByClass(
        final @NotNull Class<T> cls) {
        return TaskManager.IMP.sync(() -> parent.getEntitiesByClass(cls));
    }

    @Override
    public @NotNull Collection<Entity> getEntitiesByClasses(final Class<?>... classes) {
        return TaskManager.IMP.sync(() -> parent.getEntitiesByClasses(classes));
    }

    @Override
    public @NotNull List<Player> getPlayers() {
        return TaskManager.IMP.sync(() -> parent.getPlayers());
    }

    @Override
    public @NotNull Collection<Entity> getNearbyEntities(final @NotNull Location location,
        final double x, final double y, final double z) {
        return TaskManager.IMP.sync(() -> parent.getNearbyEntities(location, x, y, z));
    }

    @Override
    public @NotNull String getName() {
        return parent.getName();
    }

    @Override
    public @NotNull UUID getUID() {
        return parent.getUID();
    }

    @Override
    public @NotNull Location getSpawnLocation() {
        return parent.getSpawnLocation();
    }

    @Override
    public boolean setSpawnLocation(final int x, final int y, final int z) {
        return TaskManager.IMP.sync(() -> parent.setSpawnLocation(x, y, z));
    }

    @Override
    public long getTime() {
        return parent.getTime();
    }

    @Override
    public void setTime(long time) {
        parent.setTime(time);
    }

    @Override
    public long getFullTime() {
        return parent.getFullTime();
    }

    @Override
    public void setFullTime(long time) {
        parent.setFullTime(time);
    }

    @Override
    public boolean hasStorm() {
        return parent.hasStorm();
    }

    @Override
    public void setStorm(boolean hasStorm) {
        parent.setStorm(hasStorm);
    }

    @Override
    public int getWeatherDuration() {
        return parent.getWeatherDuration();
    }

    @Override
    public void setWeatherDuration(int duration) {
        parent.setWeatherDuration(duration);
    }

    @Override
    public boolean isThundering() {
        return parent.isThundering();
    }

    @Override
    public void setThundering(boolean thundering) {
        parent.setThundering(thundering);
    }

    @Override
    public int getThunderDuration() {
        return parent.getThunderDuration();
    }

    @Override
    public void setThunderDuration(int duration) {
        parent.setThunderDuration(duration);
    }

    @Override
    public boolean createExplosion(double x, double y, double z, float power) {
        return this.createExplosion(x, y, z, power, false, true);
    }

    @Override
    public boolean createExplosion(double x, double y, double z, float power, boolean setFire) {
        return this.createExplosion(x, y, z, power, setFire, true);
    }

    @Override
    public boolean createExplosion(final double x, final double y, final double z,
        final float power, final boolean setFire, final boolean breakBlocks) {
        return TaskManager.IMP
            .sync(() -> parent.createExplosion(x, y, z, power, setFire, breakBlocks));
    }

    @Override
    public boolean createExplosion(double x, double y, double z, float power, boolean setFire,
        boolean breakBlocks, @Nullable Entity source) {
        return TaskManager.IMP
            .sync(() -> parent.createExplosion(x, y, z, power, setFire, breakBlocks, source));
    }

    @Override
    public boolean createExplosion(@NotNull Location loc, float power) {
        return this.createExplosion(loc, power, false);
    }

    @Override
    public boolean createExplosion(Location loc, float power, boolean setFire) {
        return this.createExplosion(loc.getX(), loc.getY(), loc.getZ(), power, setFire);
    }

    @NotNull
    @Override
    public Environment getEnvironment() {
        return parent.getEnvironment();
    }

    @Override
    public long getSeed() {
        return parent.getSeed();
    }

    @Override
    public boolean getPVP() {
        return parent.getPVP();
    }

    @Override
    public void setPVP(boolean pvp) {
        parent.setPVP(pvp);
    }

    @Override
    public ChunkGenerator getGenerator() {
        return parent.getGenerator();
    }

    @Override
    public void save() {
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                parent.save();
            }
        });
    }

    @Override
    public @NotNull List<BlockPopulator> getPopulators() {
        return parent.getPopulators();
    }

    @Override
    public <T extends Entity> @NotNull T spawn(final @NotNull Location location,
        final @NotNull Class<T> clazz) throws IllegalArgumentException {
        return TaskManager.IMP.sync(() -> parent.spawn(location, clazz));
    }

    @Override
    public <T extends Entity> @NotNull T spawn(@NotNull Location location, @NotNull Class<T> clazz,
        Consumer<T> function) throws IllegalArgumentException {
        return TaskManager.IMP.sync(() -> parent.spawn(location, clazz, function));
    }

    @Override
    public <T extends Entity> @NotNull T spawn(@NotNull Location location, @NotNull Class<T> clazz,
        @Nullable Consumer<T> function, CreatureSpawnEvent.@NotNull SpawnReason reason)
        throws IllegalArgumentException {
        return null;
    }

    @Override
    public @NotNull FallingBlock spawnFallingBlock(@NotNull Location location,
        @NotNull MaterialData data) throws IllegalArgumentException {
        return TaskManager.IMP.sync(() -> parent.spawnFallingBlock(location, data));
    }

    @Override
    @Deprecated
    public @NotNull FallingBlock spawnFallingBlock(@NotNull Location location,
        @NotNull Material material, byte data) throws IllegalArgumentException {
        return TaskManager.IMP.sync(() -> parent.spawnFallingBlock(location, material, data));
    }

    @Override
    public @NotNull FallingBlock spawnFallingBlock(@NotNull Location location,
        @NotNull BlockData blockData) throws IllegalArgumentException {
        return TaskManager.IMP.sync(() -> parent.spawnFallingBlock(location, blockData));
    }

    @Override
    public void playEffect(@NotNull Location location, @NotNull Effect effect, int data) {
        this.playEffect(location, effect, data, 64);
    }

    @Override
    public void playEffect(final @NotNull Location location, final @NotNull Effect effect,
        final int data, final int radius) {
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                parent.playEffect(location, effect, data, radius);
            }
        });
    }

    @Override
    public <T> void playEffect(@NotNull Location loc, @NotNull Effect effect, T data) {
        this.playEffect(loc, effect, data, 64);
    }

    @Override
    public <T> void playEffect(final @NotNull Location location, final @NotNull Effect effect,
        final T data, final int radius) {
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                parent.playEffect(location, effect, data, radius);
            }
        });
    }

    @Override
    public @NotNull ChunkSnapshot getEmptyChunkSnapshot(final int x, final int z,
        final boolean includeBiome, final boolean includeBiomeTempRain) {
        return TaskManager.IMP
            .sync(() -> parent.getEmptyChunkSnapshot(x, z, includeBiome, includeBiomeTempRain));
    }

    @Override
    public void setSpawnFlags(boolean allowMonsters, boolean allowAnimals) {
        parent.setSpawnFlags(allowMonsters, allowAnimals);
    }

    @Override
    public boolean getAllowAnimals() {
        return parent.getAllowAnimals();
    }

    @Override
    public boolean getAllowMonsters() {
        return parent.getAllowMonsters();
    }

    @Override
    public @NotNull Biome getBiome(int x, int z) {
        return adapter.adapt(getExtent().getBiomeType(x, 0, z));
    }

    @Override
    public @NotNull Biome getBiome(int x, int y, int z) {
        return adapter.adapt(getExtent().getBiomeType(x, y, z));
    }

    @Override
    public void setBiome(int x, int z, @NotNull Biome bio) {
        BiomeType biome = adapter.adapt(bio);
        getExtent().setBiome(x, 0, z, biome);
    }

    @Override
    public void setBiome(int x, int y, int z, @NotNull Biome bio) {
        BiomeType biome = adapter.adapt(bio);
        getExtent().setBiome(x, y, z, biome);
    }

    @Override
    public double getTemperature(int x, int z) {
        return parent.getTemperature(x, z);
    }

    @Override
    public double getTemperature(int x, int y, int z) {
        return parent.getTemperature(x, y, z);
    }

    @Override
    public double getHumidity(int x, int z) {
        return parent.getHumidity(x, z);
    }

    @Override
    public double getHumidity(int x, int y, int z) {
        return parent.getHumidity(x, y, z);
    }

    @Override
    public int getMaxHeight() {
        return parent.getMaxHeight();
    }

    @Override
    public int getSeaLevel() {
        return parent.getSeaLevel();
    }

    @Override
    public boolean getKeepSpawnInMemory() {
        return parent.getKeepSpawnInMemory();
    }

    @Override
    public void setKeepSpawnInMemory(final boolean keepLoaded) {
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                parent.setKeepSpawnInMemory(keepLoaded);
            }
        });
    }

    @Override
    public boolean isAutoSave() {
        return parent.isAutoSave();
    }

    @Override
    public void setAutoSave(boolean value) {
        parent.setAutoSave(value);
    }

    @Override
    public void setDifficulty(@NotNull Difficulty difficulty) {
        parent.setDifficulty(difficulty);
    }

    @Override
    public @NotNull Difficulty getDifficulty() {
        return parent.getDifficulty();
    }

    @Override
    public @NotNull File getWorldFolder() {
        return parent.getWorldFolder();
    }

    @Override
    public WorldType getWorldType() {
        return parent.getWorldType();
    }

    @Override
    public boolean canGenerateStructures() {
        return parent.canGenerateStructures();
    }

    @Override
    public void setHardcore(boolean hardcore) {
        parent.setHardcore(hardcore);
    }

    @Override
    public boolean isHardcore() {
        return parent.isHardcore();
    }

    @Override
    public long getTicksPerAnimalSpawns() {
        return parent.getTicksPerAnimalSpawns();
    }

    @Override
    public void setTicksPerAnimalSpawns(int ticksPerAnimalSpawns) {
        parent.setTicksPerAnimalSpawns(ticksPerAnimalSpawns);
    }

    @Override
    public long getTicksPerMonsterSpawns() {
        return parent.getTicksPerMonsterSpawns();
    }

    @Override
    public void setTicksPerMonsterSpawns(int ticksPerMonsterSpawns) {
        parent.setTicksPerMonsterSpawns(ticksPerMonsterSpawns);
    }

    @Override
    public int getMonsterSpawnLimit() {
        return parent.getMonsterSpawnLimit();
    }

    @Override
    public void setMonsterSpawnLimit(int limit) {
        parent.setMonsterSpawnLimit(limit);
    }

    @Override
    public int getAnimalSpawnLimit() {
        return parent.getAnimalSpawnLimit();
    }

    @Override
    public void setAnimalSpawnLimit(int limit) {
        parent.setAnimalSpawnLimit(limit);
    }

    @Override
    public int getWaterAnimalSpawnLimit() {
        return parent.getWaterAnimalSpawnLimit();
    }

    @Override
    public void setWaterAnimalSpawnLimit(int limit) {
        parent.setWaterAnimalSpawnLimit(limit);
    }

    @Override
    public int getWaterAmbientSpawnLimit() {
        return parent.getWaterAmbientSpawnLimit();
    }

    @Override
    public void setWaterAmbientSpawnLimit(int limit) {
        parent.setWaterAmbientSpawnLimit(limit);
    }

    @Override
    public int getAmbientSpawnLimit() {
        return parent.getAmbientSpawnLimit();
    }

    @Override
    public void setAmbientSpawnLimit(int limit) {
        parent.setAmbientSpawnLimit(limit);
    }

    @Override
    public void playSound(final @NotNull Location location, final @NotNull Sound sound,
        final float volume, final float pitch) {
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                parent.playSound(location, sound, volume, pitch);
            }
        });
    }

    @Override
    public void playSound(final @NotNull Location location, final @NotNull String sound,
        final float volume, final float pitch) {
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                parent.playSound(location, sound, volume, pitch);
            }
        });
    }

    @Override
    public void playSound(@NotNull Location location, @NotNull Sound sound,
        @NotNull SoundCategory category, float volume, float pitch) {
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                parent.playSound(location, sound, category, volume, pitch);
            }
        });
    }

    @Override
    public void playSound(@NotNull Location location, @NotNull String sound,
        @NotNull SoundCategory category, float volume, float pitch) {
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                parent.playSound(location, sound, category, volume, pitch);
            }
        });
    }

    @Override
    public String[] getGameRules() {
        return parent.getGameRules();
    }

    @Override
    public String getGameRuleValue(String rule) {
        return parent.getGameRuleValue(rule);
    }

    @Override
    public boolean setGameRuleValue(@NotNull String rule, @NotNull String value) {
        return parent.setGameRuleValue(rule, value);
    }

    @Override
    public boolean isGameRule(@NotNull String rule) {
        return parent.isGameRule(rule);
    }

    @Override
    public <T> T getGameRuleValue(@NotNull GameRule<T> gameRule) {
        return parent.getGameRuleValue(gameRule);
    }

    @Override
    public <T> T getGameRuleDefault(@NotNull GameRule<T> gameRule) {
        return parent.getGameRuleDefault(gameRule);
    }

    @Override
    public <T> boolean setGameRule(@NotNull GameRule<T> gameRule, @NotNull T t) {
        return parent.setGameRule(gameRule, t);
    }

    @Override
    public @NotNull Spigot spigot() {
        return parent.spigot();
    }

    @Override
    public @Nullable Raid locateNearestRaid(@NotNull Location location, int i) {
        return parent.locateNearestRaid(location, i);
    }

    @Override
    public @NotNull List<Raid> getRaids() {
        return parent.getRaids();
    }

    @Override
    public void setMetadata(final @NotNull String key, final @NotNull MetadataValue meta) {
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                parent.setMetadata(key, meta);
            }
        });
    }

    @Override
    public @NotNull List<MetadataValue> getMetadata(@NotNull String key) {
        return parent.getMetadata(key);
    }

    @Override
    public boolean hasMetadata(@NotNull String key) {
        return parent.hasMetadata(key);
    }

    @Override
    public void removeMetadata(final @NotNull String key, final @NotNull Plugin plugin) {
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                parent.removeMetadata(key, plugin);
            }
        });
    }

    @Override
    public void sendPluginMessage(@NotNull Plugin source, @NotNull String channel, byte[] message) {
        parent.sendPluginMessage(source, channel, message);
    }

    @Override
    public @NotNull Set<String> getListeningPluginChannels() {
        return parent.getListeningPluginChannels();
    }

    public BukkitImplAdapter getAdapter() {
        return adapter;
    }

    @Override
    public @NotNull Collection<Entity> getNearbyEntities(@NotNull BoundingBox arg0) {
        return parent.getNearbyEntities(arg0);
    }

    @Override
    public @NotNull Collection<Entity> getNearbyEntities(@NotNull BoundingBox arg0,
        Predicate<Entity> arg1) {
        return parent.getNearbyEntities(arg0, arg1);
    }

    @Override
    public @NotNull Collection<Entity> getNearbyEntities(@NotNull Location arg0, double arg1,
        double arg2, double arg3, Predicate<Entity> arg4) {
        return parent.getNearbyEntities(arg0, arg1, arg2, arg3, arg4);
    }

    @Override
    public boolean isChunkForceLoaded(int arg0, int arg1) {
        return parent.isChunkForceLoaded(arg0, arg1);
    }

    @Override
    public Location locateNearestStructure(@NotNull Location arg0, @NotNull StructureType arg1,
        int arg2, boolean arg3) {
        return parent.locateNearestStructure(arg0, arg1, arg2, arg3);
    }

    @Override
    public int getViewDistance() {
        return parent.getViewDistance();
    }

    @Override
    public void setViewDistance(int viewDistance) {

    }

    @Override
    public int getNoTickViewDistance() {
        return 0;
    }

    @Override
    public void setNoTickViewDistance(int viewDistance) {

    }

    @Override
    public RayTraceResult rayTrace(@NotNull Location arg0, @NotNull Vector arg1, double arg2,
        @NotNull FluidCollisionMode arg3, boolean arg4, double arg5, Predicate<Entity> arg6) {
        return parent.rayTrace(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
    }

    @Override
    public RayTraceResult rayTraceBlocks(@NotNull Location arg0, @NotNull Vector arg1,
        double arg2) {
        return parent.rayTraceBlocks(arg0, arg1, arg2);
    }

    @Override
    public RayTraceResult rayTraceBlocks(@NotNull Location start, @NotNull Vector direction,
        double maxDistance, @NotNull FluidCollisionMode fluidCollisionMode) {
        return parent.rayTraceBlocks(start, direction, maxDistance, fluidCollisionMode);
    }

    @Override
    public RayTraceResult rayTraceBlocks(@NotNull Location start, @NotNull Vector direction,
        double arg2, @NotNull FluidCollisionMode fluidCollisionMode, boolean ignorePassableBlocks) {
        return parent
            .rayTraceBlocks(start, direction, arg2, fluidCollisionMode, ignorePassableBlocks);
    }

    @Override
    public RayTraceResult rayTraceEntities(@NotNull Location start, @NotNull Vector direction,
        double maxDistance) {
        return parent.rayTraceEntities(start, direction, maxDistance);
    }

    @Override
    public RayTraceResult rayTraceEntities(@NotNull Location arg0, @NotNull Vector arg1,
        double arg2, double arg3) {
        return parent.rayTraceEntities(arg0, arg1, arg2, arg3);
    }

    @Override
    public RayTraceResult rayTraceEntities(@NotNull Location arg0, @NotNull Vector arg1,
        double arg2, Predicate<Entity> arg3) {
        return parent.rayTraceEntities(arg0, arg1, arg2, arg3);
    }

    @Override
    public RayTraceResult rayTraceEntities(@NotNull Location arg0, @NotNull Vector arg1,
        double arg2, double arg3, Predicate<Entity> arg4) {
        return parent.rayTraceEntities(arg0, arg1, arg2, arg3, arg4);
    }


    @Override
    public <T> void spawnParticle(@NotNull Particle particle, double x, double y, double z,
        int count, double offsetX, double offsetY, double offsetZ, double extra, @Nullable T data,
        boolean force) {

    }

    @Override
    public void setChunkForceLoaded(int x, int z, boolean forced) {
        parent.setChunkForceLoaded(x, z, forced);
    }

    @Override
    public @NotNull Collection<Chunk> getForceLoadedChunks() {
        return parent.getForceLoadedChunks();
    }

    @Override
    public boolean addPluginChunkTicket(int x, int z, @NotNull Plugin plugin) {
        return getBukkitWorld().addPluginChunkTicket(x, z, plugin);
    }

    @Override
    public boolean removePluginChunkTicket(int x, int z, @NotNull Plugin plugin) {
        return getBukkitWorld().removePluginChunkTicket(x, z, plugin);
    }

    @Override
    public void removePluginChunkTickets(@NotNull Plugin plugin) {
        getBukkitWorld().removePluginChunkTickets(plugin);
    }

    @Override
    public @NotNull Collection<Plugin> getPluginChunkTickets(int x, int z) {
        return getBukkitWorld().getPluginChunkTickets(x, z);
    }

    @Override
    public @NotNull Map<Plugin, Collection<Chunk>> getPluginChunkTickets() {
        return getBukkitWorld().getPluginChunkTickets();
    }

    @Override
    public int getHighestBlockYAt(int x, int z,
        com.destroystokyo.paper.@NotNull HeightmapType heightmap)
        throws UnsupportedOperationException {
        return TaskManager.IMP.sync(() -> parent.getHighestBlockYAt(x, z, heightmap));
    }

    @Override
    public int getEntityCount() {
        return TaskManager.IMP.sync(() -> parent.getEntityCount());
    }

    @Override
    public int getTileEntityCount() {
        return TaskManager.IMP.sync(() -> parent.getTileEntityCount());
    }

    @Override
    public int getTickableTileEntityCount() {
        return TaskManager.IMP.sync(() -> parent.getTickableTileEntityCount());
    }

    @Override
    public int getChunkCount() {
        return TaskManager.IMP.sync(() -> parent.getChunkCount());
    }

    @Override
    public int getPlayerCount() {
        return TaskManager.IMP.sync(() -> parent.getPlayerCount());
    }

    @Override
    public @NotNull CompletableFuture<Chunk> getChunkAtAsync(int arg0, int arg1, boolean arg2) {
        return parent.getChunkAtAsync(arg0, arg1, arg2);
    }

    @Override
    public @NotNull CompletableFuture<Chunk> getChunkAtAsync(int x, int z, boolean gen,
        boolean urgent) {
        return null;
    }

    @Override
    public boolean isDayTime() {
        return parent.isDayTime();
    }

    @Override
    public void getChunkAtAsync(int x, int z, @NotNull ChunkLoadCallback cb) {
        parent.getChunkAtAsync(x, z, cb);
    }

    @Override
    public void getChunkAtAsync(@NotNull Location location, @NotNull ChunkLoadCallback cb) {
        parent.getChunkAtAsync(location, cb);
    }

    @Override
    public void getChunkAtAsync(@NotNull Block block, @NotNull ChunkLoadCallback cb) {
        parent.getChunkAtAsync(block, cb);
    }

    @Override
    public Entity getEntity(@NotNull UUID uuid) {
        return TaskManager.IMP.sync(() -> parent.getEntity(uuid));
    }

    @Nullable
    @Override
    public DragonBattle getEnderDragonBattle() {
        return TaskManager.IMP.sync(() -> parent.getEnderDragonBattle());
    }

    @Override
    public boolean createExplosion(Entity source, @NotNull Location loc, float power,
        boolean setFire, boolean breakBlocks) {
        return TaskManager.IMP
            .sync(() -> parent.createExplosion(source, loc, power, setFire, breakBlocks));
    }

    @Override
    public boolean createExplosion(@NotNull Location loc, float power, boolean setFire,
        boolean breakBlocks) {
        return false;
    }

    @Override
    public boolean createExplosion(@NotNull Location loc, float power, boolean setFire,
        boolean breakBlocks, @Nullable Entity source) {
        return false;
    }


    @Override
    public <T> void spawnParticle(@NotNull Particle particle, List<Player> receivers,
        @NotNull Player source, double x, double y, double z, int count, double offsetX,
        double offsetY, double offsetZ, double extra, T data) {
        parent.spawnParticle(particle, receivers, source, x, y, z, count, offsetX, offsetY, offsetZ,
            extra, data);
    }

    @Override
    public <T> void spawnParticle(@NotNull Particle particle, List<Player> list, Player player,
        double v, double v1, double v2, int i, double v3, double v4, double v5, double v6, T t,
        boolean b) {
        parent.spawnParticle(particle, list, player, v, v1, v2, i, v3, v4, v5, v6, t, b);
    }

    @Override
    public <T> void spawnParticle(@NotNull Particle particle, @NotNull Location location, int count,
        double offsetX, double offsetY, double offsetZ, double extra, @Nullable T data,
        boolean force) {
        parent.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra, data,
            force);
    }

    @Override
    public int getHighestBlockYAt(@NotNull Location location, @NotNull HeightmapType heightmap)
        throws UnsupportedOperationException {
        return parent.getHighestBlockYAt(location, heightmap);
    }

    @Override
    public @NotNull Block getHighestBlockAt(int x, int z, @NotNull HeightmapType heightmap)
        throws UnsupportedOperationException {
        return parent.getHighestBlockAt(x, z, heightmap);
    }

    @Override
    public @NotNull Block getHighestBlockAt(@NotNull Location location,
        @NotNull HeightmapType heightmap) throws UnsupportedOperationException {
        return parent.getHighestBlockAt(location, heightmap);
    }

    @Override
    public long getTicksPerWaterSpawns() {
        return parent.getTicksPerWaterSpawns();
    }

    @Override
    public void setTicksPerWaterSpawns(int ticksPerWaterSpawns) {
        parent.setTicksPerWaterSpawns(ticksPerWaterSpawns);
    }

    @Override
    public long getTicksPerWaterAmbientSpawns() {
        return parent.getTicksPerWaterAmbientSpawns();
    }

    @Override
    public void setTicksPerWaterAmbientSpawns(int ticksPerAmbientSpawns) {
        parent.setTicksPerWaterAmbientSpawns(ticksPerAmbientSpawns);
    }

    @Override
    public long getTicksPerAmbientSpawns() {
        return parent.getTicksPerAmbientSpawns();
    }

    @Override
    public void setTicksPerAmbientSpawns(int ticksPerAmbientSpawns) {
        parent.setTicksPerAmbientSpawns(ticksPerAmbientSpawns);
    }

}
