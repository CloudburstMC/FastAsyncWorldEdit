/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.bukkit;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.implementation.packet.ChunkPacket;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.nukkitx.math.vector.Vector3i;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.internal.wna.WorldNativeAccess;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.world.AbstractWorld;
import com.sk89q.worldedit.world.WorldUnloadedException;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.weather.WeatherType;
import com.sk89q.worldedit.world.weather.WeatherTypes;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.Chest;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.inventory.DoubleChestInventory;
import org.cloudburstmc.server.inventory.Inventory;
import org.cloudburstmc.server.inventory.InventoryHolder;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.potion.Effect;
import org.cloudburstmc.server.registry.BiomeRegistry;
import org.cloudburstmc.server.utils.data.TreeSpecies;
import org.slf4j.Logger;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

public class CloudburstWorld extends AbstractWorld {

    private static final Logger logger = WorldEdit.logger;

    private static final Int2ObjectMap<Effect> EFFECTS = new Int2ObjectOpenHashMap<>();

    static {
        for (Effect effect : getEffects()) {
            int id = effect.getId();
            EFFECTS.put(id, effect);
        }
    }

    private static Effect[] getEffects() {
        try {
            Field field = Effect.class.getDeclaredField("effects");
            field.setAccessible(true);
            return (Effect[]) field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError("Unable to find Effects field");
        }
    }

    private WeakReference<Level> worldRef;
    private final String worldNameRef;

    /**
     * Construct the object.
     *
     * @param world the world
     */
    public CloudburstWorld(Level world) {
        this.worldRef = new WeakReference<>(world);
        this.worldNameRef = world.getId();
    }

    @Override
    public List<com.sk89q.worldedit.entity.Entity> getEntities(Region region) {
        Level world = getWorld();

        Entity[] ents = world.getEntities();
        List<com.sk89q.worldedit.entity.Entity> entities = new ArrayList<>();
        for (Entity ent : ents) {
            if (region.contains(CloudburstAdapter.asBlockVector(ent.getLocation()))) {
                entities.add(CloudburstAdapter.adapt(ent));
            }
        }
        return entities;
    }

    @Override
    public List<com.sk89q.worldedit.entity.Entity> getEntities() {
        List<com.sk89q.worldedit.entity.Entity> list = new ArrayList<>();
        for (Entity entity : getWorld().getEntities()) {
            list.add(CloudburstAdapter.adapt(entity));
        }
        return list;
    }

    @Nullable
    @Override
    public com.sk89q.worldedit.entity.Entity createEntity(com.sk89q.worldedit.util.Location location, BaseEntity entity) {
        BukkitImplAdapter adapter = WorldEditPlugin.getInstance().getAdapter();
        if (adapter != null) {
            try {
                Entity createdEntity = adapter.createEntity(CloudburstAdapter.adapt(getWorld(), location), entity);
                if (createdEntity != null) {
                    return new BukkitEntity(createdEntity);
                } else {
                    return null;
                }
            } catch (Exception e) {
                logger.warn("Corrupt entity found when creating: " + entity.getType().getId());
                if (entity.getNbtData() != null) {
                    logger.warn(entity.getNbtData().toString());
                }
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Get the world handle.
     *
     * @return the world
     */
    public Level getWorld() {
        Level tmp = worldRef.get();
        if (tmp == null) {
            tmp = Server.getInstance().getLevel(worldNameRef);
            if (tmp != null) {
                worldRef = new WeakReference<>(tmp);
            }
        }
        return checkNotNull(tmp, "The world was unloaded and the reference is unavailable");
    }

    /**
     * Get the world handle.
     *
     * @return the world
     */
    protected Level getWorldChecked() throws WorldEditException {
        Level world = worldRef.get();
        if (world == null) {
            throw new WorldUnloadedException();
        }
        return world;
    }

    @Override
    public String getName() {
        return getWorld().getName();
    }

    @Override
    public String getId() {
        return getWorld().getName().replace(" ", "_").toLowerCase(Locale.ROOT);
    }

    @Override
    public Path getStoragePath() {
        return Paths.get(Server.getInstance().getDataPath()).resolve("worlds").resolve(getWorld().getId());
    }

    @Override
    public int getBlockLightLevel(BlockVector3 pt) {
        return getWorld().getBlockLightAt(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ());
    }

    @Override
    public boolean regenerate(Region region, EditSession editSession) {
        BukkitImplAdapter adapter = WorldEditPlugin.getInstance().getAdapter();
        try {
            if (adapter != null) {
                return adapter.regenerate(getWorld(), region, editSession);
            } else {
                throw new UnsupportedOperationException("Missing BukkitImplAdapater for this version.");
            }
        } catch (Exception e) {
            logger.warn("Regeneration via adapter failed.", e);
        }
        /*
        BaseBlock[] history = new BaseBlock[16 * 16 * (getMaxY() + 1)];

        for (BlockVector2 chunk : region.getChunks()) {
            BlockVector3 min = BlockVector3.at(chunk.getBlockX() * 16, 0, chunk.getBlockZ() * 16);

            // First save all the blocks inside
            for (int x = 0; x < 16; ++x) {
                for (int y = 0; y < (getMaxY() + 1); ++y) {
                    for (int z = 0; z < 16; ++z) {
                        BlockVector3 pt = min.add(x, y, z);
                        int index = y * 16 * 16 + z * 16 + x;
                        history[index] = editSession.getFullBlock(pt);
                    }
                }
            }

            try {
                getWorld().regenerateChunk(chunk.getBlockX(), chunk.getBlockZ());
            } catch (Throwable t) {
                logger.warn("Chunk generation via Bukkit raised an error", t);
            }

            // Then restore
            for (int x = 0; x < 16; ++x) {
                for (int y = 0; y < (getMaxY() + 1); ++y) {
                    for (int z = 0; z < 16; ++z) {
                        BlockVector3 pt = min.add(x, y, z);
                        int index = y * 16 * 16 + z * 16 + x;

                        // We have to restore the block if it was outside
                        if (!region.contains(pt)) {
                            editSession.smartSetBlock(pt, history[index]);
                        } else { // Otherwise fool with history
                            editSession.getChangeSet().add(new BlockChange(pt, history[index], editSession.getFullBlock(pt)));
                        }
                    }
                }
            }
        }

        return true;
         */
        return editSession.regenerate(region);
    }

    /**
     * Gets the single block inventory for a potentially double chest.
     * Handles people who have an old version of Bukkit.
     * This should be replaced with {@link Chest#getInventory()} ()}
     * in a few months (now = March 2012) // note from future dev - lol
     *
     * @param chest The chest to get a single block inventory for
     * @return The chest's inventory
     */
    private Inventory getBlockInventory(Chest chest) {
        try {
            return chest.getInventory();
        } catch (Throwable t) {
            if (chest.getInventory() instanceof DoubleChestInventory) {
                DoubleChestInventory inven = (DoubleChestInventory) chest.getInventory();
                if (inven.getLeftSide().getHolder().equals(chest)) {
                    return inven.getLeftSide();
                } else if (inven.getRightSide().getHolder().equals(chest)) {
                    return inven.getRightSide();
                } else {
                    return inven;
                }
            } else {
                return chest.getInventory();
            }
        }
    }

    @Override
    public boolean clearContainerBlockContents(BlockVector3 pt) {
        BlockEntity entity = getWorld().getBlockEntity(Vector3i.from(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ()));
        if (!(entity instanceof InventoryHolder)) {
            return false;
        }

        InventoryHolder chest = (InventoryHolder) entity;
        Inventory inven = chest.getInventory();
        if (chest instanceof Chest) {
            inven = getBlockInventory((Chest) chest);
        }
        inven.clearAll();
        return true;
    }

//    /**
//     * An EnumMap that stores which WorldEdit TreeTypes apply to which Bukkit TreeTypes.
//     */
//    private static final EnumMap<TreeGenerator.TreeType, TreeSpecies> treeTypeMapping =
//            new EnumMap<>(TreeGenerator.TreeType.class);
//
//    static {
//        for (TreeGenerator.TreeType type : TreeGenerator.TreeType.values()) {
//            try {
//                TreeType bukkitType = TreeType.valueOf(type.name());
//                treeTypeMapping.put(type, bukkitType);
//            } catch (IllegalArgumentException e) {
//                // Unhandled TreeType
//            }
//        }
//        // Other mappings for WE-specific values
//        treeTypeMapping.put(TreeGenerator.TreeType.SHORT_JUNGLE, TreeType.SMALL_JUNGLE);
//        treeTypeMapping.put(TreeGenerator.TreeType.RANDOM, TreeType.BROWN_MUSHROOM);
//        treeTypeMapping.put(TreeGenerator.TreeType.RANDOM_REDWOOD, TreeType.REDWOOD);
//        treeTypeMapping.put(TreeGenerator.TreeType.PINE, TreeType.REDWOOD);
//        treeTypeMapping.put(TreeGenerator.TreeType.RANDOM_BIRCH, TreeType.BIRCH);
//        treeTypeMapping.put(TreeGenerator.TreeType.RANDOM_JUNGLE, TreeType.JUNGLE);
//        treeTypeMapping.put(TreeGenerator.TreeType.RANDOM_MUSHROOM, TreeType.BROWN_MUSHROOM);
//        for (TreeGenerator.TreeType type : TreeGenerator.TreeType.values()) {
//            if (treeTypeMapping.get(type) == null) {
//                WorldEdit.logger.error("No TreeType mapping for TreeGenerator.TreeType." + type);
//            }
//        }
//    }
//
//    public static TreeType toBukkitTreeType(TreeGenerator.TreeType type) {
//        return treeTypeMapping.get(type);
//    }
//
//    @Override
//    public boolean generateTree(TreeGenerator.TreeType type, EditSession editSession, BlockVector3 pt) {
//        World world = getWorld();
//        TreeType bukkitType = toBukkitTreeType(type);
//        if (bukkitType == TreeType.CHORUS_PLANT) {
//            pt = pt.add(0, 1, 0); // bukkit skips the feature gen which does this offset normally, so we have to add it back
//        }
//        return type != null && world.generateTree(CloudburstAdapter.adapt(world, pt), bukkitType,
//                new EditSessionBlockChangeDelegate(editSession));
//    }

    @Override
    public void dropItem(Vector3 pt, BaseItemStack item) {
        Level world = getWorld();
        world.dropItem(CloudburstAdapter.adapt(world, pt).getPosition(), CloudburstAdapter.adapt(item));
    }

    @Override
    public void checkLoadedChunk(BlockVector3 pt) {
        Level world = getWorld();
        int chunkX = pt.getBlockX() >> 4;
        int chunkZ = pt.getBlockZ() >> 4;
        world.getChunkFuture(chunkX, chunkZ); // Run async
    }

    @Override
    public boolean equals(Object other) {
        final Level ref = worldRef.get();
        if (ref == null) {
            return false;
        } else if (other == null) {
            return false;
        } else if ((other instanceof CloudburstWorld)) {
            Level otherWorld = ((CloudburstWorld) other).worldRef.get();
            return ref.equals(otherWorld);
        } else if (other instanceof com.sk89q.worldedit.world.World) {
            return ((com.sk89q.worldedit.world.World) other).getName().equals(ref.getName());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return getWorld().hashCode();
    }

    @Override
    public int getMaxY() {
        return getWorld().getDimension() == 1 ? 127 : 255;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void fixAfterFastMode(Iterable<BlockVector2> chunks) {
        // FIXME: Cloudburst needs a method for this
//        Level world = getWorld();
//        for (BlockVector2 chunkPos : chunks) {
//            world.refreshChunk(chunkPos.getBlockX(), chunkPos.getBlockZ());
//        }
    }

    @Override
    public boolean playEffect(Vector3 position, int type, int data) {
        Level world = getWorld();

        final Effect effect = EFFECTS.get(type);
        if (effect == null) {
            return false;
        }

        world.addParticleEffect(CloudburstAdapter.adapt(world, position).getPosition(), effect, data);

        return true;
    }

    @Override
    public WeatherType getWeather() {
        if (getWorld().isThundering()) {
            return WeatherTypes.THUNDER_STORM;
        } else if (getWorld().isRaining()) {
            return WeatherTypes.RAIN;
        }

        return WeatherTypes.CLEAR;
    }

    @Override
    public long getRemainingWeatherDuration() {
        return getWorld().getRainTime();
    }

    @Override
    public void setWeather(WeatherType weatherType) {
        if (weatherType == WeatherTypes.THUNDER_STORM) {
            getWorld().setThundering(true);
        } else if (weatherType == WeatherTypes.RAIN) {
            getWorld().setRaining(true);
        } else {
            getWorld().setRaining(false);
            getWorld().setThundering(false);
        }
    }

    @Override
    public void setWeather(WeatherType weatherType, long duration) {
        // Who named these methods...
        if (weatherType == WeatherTypes.THUNDER_STORM) {
            getWorld().setThundering(true);
            getWorld().setThunderTime((int) duration);
            getWorld().setRainTime((int) duration);
        } else if (weatherType == WeatherTypes.RAIN) {
            getWorld().setRaining(true);
            getWorld().setRainTime((int) duration);
        } else {
            getWorld().setRaining(false);
            getWorld().setThundering(false);
            getWorld().setRainTime((int) duration);
        }
    }

    @Override
    public BlockVector3 getSpawnPosition() {
        return CloudburstAdapter.asBlockVector(getWorld().getSpawnLocation());
    }

    @Override
    public void simulateBlockMine(BlockVector3 pt) {
        getWorld().useBreakOn(Vector3i.from(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ()));
    }

    private static volatile boolean hasWarnedImplError = false;

    @Override
    public com.sk89q.worldedit.world.block.BlockState getBlock(BlockVector3 position) {
        BukkitImplAdapter adapter = WorldEditPlugin.getInstance().getAdapter();
        if (adapter != null) {
            try {
                return adapter.getBlock(CloudburstAdapter.adapt(getWorld(), position)).toImmutableState();
            } catch (Exception e) {
                if (!hasWarnedImplError) {
                    hasWarnedImplError = true;
                    logger.warn("Unable to retrieve block via impl adapter", e);
                }
            }
        }
        BlockState state = getWorld().getBlockAt(position.getBlockX(), position.getBlockY(), position.getBlockZ());
        return CloudburstAdapter.adapt(state);
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block, SideEffectSet sideEffects) {
        getWorld().setBlockAt(position.getBlockX(), position.getBlockY(), position.getBlockZ(), CloudburstAdapter.adapt(block));
        return true;
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        com.sk89q.worldedit.world.block.BlockState state = CloudburstAdapter.adapt(
                getWorld().getBlockAt(position.getBlockX(), position.getBlockY(), position.getBlockZ()));

        CompoundTag nbtData = null;
        BlockEntity entity = getWorld().getBlockEntity(Vector3i.from(position.getX(), position.getY(), position.getZ()));
        if (entity != null) {
            nbtData = CloudburstAdapter.adapt(entity.getChunkTag());
        }
        return new BaseBlock(state, nbtData);
    }

    @Override
    public Set<SideEffect> applySideEffects(BlockVector3 position, com.sk89q.worldedit.world.block.BlockState previousType,
            SideEffectSet sideEffectSet) {
//        if (worldNativeAccess != null) {
//            worldNativeAccess.applySideEffects(position, previousType, sideEffectSet);
//            return Sets.intersection(
//                    WorldEditPlugin.getInstance().getInternalPlatform().getSupportedSideEffects(),
//                    sideEffectSet.getSideEffectsToApply()
//            );
//        }

        return ImmutableSet.of();
    }

    @Override
    public boolean useItem(BlockVector3 position, BaseItem item, Direction face) {
        getWorld().useItemOn(Vector3i.from(position.getX(), position.getY(), position.getZ()),
                CloudburstAdapter.adapt(item), CloudburstAdapter.adapt(face));

        return false;
    }

    @Override
    public BiomeType getBiome(BlockVector2 position) {
        return CloudburstAdapter.adapt(BiomeRegistry.get().getBiome(getWorld().getBiomeId(position.getBlockX(), position.getBlockZ())));
    }

    @Override
    public boolean setBiome(BlockVector2 position, BiomeType biome) {
        getWorld().setBiomeId(position.getBlockX(), position.getBlockZ(),
                (byte) BiomeRegistry.get().getRuntimeId(CloudburstAdapter.adapt(biome)));
        return true;
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block) throws WorldEditException {
        return setBlock(BlockVector3.at(x, y, z), block);
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tile) throws WorldEditException {
        return false;
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        return setBiome(BlockVector2.at(x, z), biome);
    }

    @Override
    public void refreshChunk(int chunkX, int chunkZ) {
        // FIXME: Doesn't exist in Cloudburst
//        getWorld().refreshChunk(chunkX, chunkZ);
    }

    @Override
    public IChunkGet get(int chunkX, int chunkZ) {
        return CloudburstAdapter.adapt(getWorldChecked(), chunkX, chunkZ);
    }

    @Override
    public void sendFakeChunk(Player player, ChunkPacket packet) {
        org.cloudburstmc.server.player.Player cloudburstPlayer = CloudburstAdapter.adapt(player);
        CloudburstAdapter.sendFakeChunk(getWorld(), cloudburstPlayer, packet);
    }
}
