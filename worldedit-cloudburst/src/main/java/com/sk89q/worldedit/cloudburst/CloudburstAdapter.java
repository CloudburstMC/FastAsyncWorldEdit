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

package com.sk89q.worldedit.cloudburst;

import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.implementation.packet.ChunkPacket;
import com.boydti.fawe.cloudburst.adapter.CloudburstGetBlocks;
import com.google.common.collect.ImmutableBiMap;
import com.nukkitx.nbt.NbtList;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import com.sk89q.jnbt.*;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.PlayerProxy;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldedit.world.entity.EntityTypes;
import com.sk89q.worldedit.world.gamemode.GameMode;
import com.sk89q.worldedit.world.item.ItemType;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.command.CommandSender;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.level.biome.Biome;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BiomeRegistry;
import org.cloudburstmc.server.registry.EntityRegistry;
import org.cloudburstmc.server.utils.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Adapts between Cloudburst and WorldEdit equivalent objects.
 */
public class CloudburstAdapter {

    private CloudburstAdapter() {
    }

    private static final ParserContext TO_BLOCK_CONTEXT = new ParserContext();

    static {
        TO_BLOCK_CONTEXT.setRestricted(false);
    }

    /**
     * Checks equality between a WorldEdit BlockType and a Bukkit Material.
     *
     * @param blockType The WorldEdit BlockType
     * @param type The Bukkit Material
     * @return If they are equal
     */
    public static boolean equals(BlockType blockType, Identifier type) {
        return Identifier.fromString(blockType.getId()) == type;
    }

    /**
     * Convert any WorldEdit world into an equivalent wrapped Bukkit world.
     *
     * <p>If a matching world cannot be found, a {@link RuntimeException}
     * will be thrown.</p>
     *
     * @param world the world
     * @return a wrapped Bukkit world
     */
    public static Level asCloudburstWorld(World world) {
        return getAdapter().asBukkitWorld(world);
    }

    /**
     * Create a WorldEdit world from a Bukkit world.
     *
     * @param world the Bukkit world
     * @return a WorldEdit world
     */
    public static World adapt(org.bukkit.World world) {
        return getAdapter().adapt(world);
    }

    public static IChunkGet adapt(org.cloudburstmc.server.level.Level level, int chunkX, int chunkZ) {
        return new CloudburstGetBlocks(level, chunkX, chunkZ);
    }

    /**
     * Create a WorldEdit Actor from a Bukkit CommandSender.
     *
     * @param sender The Bukkit CommandSender
     * @return The WorldEdit Actor
     */
    public static Actor adapt(CommandSender sender) {
        return WorldEditPlugin.getInstance().wrapCommandSender(sender);
    }

    /**
     * Create a WorldEdit Player from a Bukkit Player.
     *
     * @param player The Bukkit player
     * @return The WorldEdit player
     */
    public static CloudburstPlayer adapt(Player player) {
        return WorldEditPlugin.getInstance().wrapPlayer(player);
    }

    /**
     * Create a Bukkit CommandSender from a WorldEdit Actor.
     *
     * @param actor The WorldEdit actor
     * @return The Bukkit command sender
     */
    public static CommandSender adapt(Actor actor) {
        if (actor instanceof com.sk89q.worldedit.entity.Player) {
            return adapt((com.sk89q.worldedit.entity.Player) actor);
        } else if (actor instanceof BukkitBlockCommandSender) {
            return ((BukkitBlockCommandSender) actor).getSender();
        }
        return ((BukkitCommandSender) actor).getSender();
    }

    /**
     * Create a Bukkit Player from a WorldEdit Player.
     *
     * @param player The WorldEdit player
     * @return The Bukkit player
     */
    public static Player adapt(com.sk89q.worldedit.entity.Player player) {
        player = PlayerProxy.unwrap(player);
        return player == null ? null : ((Player) player).getPlayer();
    }

    /**
     * Create a WorldEdit Direction from a Bukkit BlockFace.
     *
     * @param face the Bukkit BlockFace
     * @return a WorldEdit direction
     */
    public static Direction adapt(@Nullable org.cloudburstmc.server.math.Direction face) {
        if (face == null) {
            return null;
        }
        switch (face) {
            case NORTH: return Direction.NORTH;
            case SOUTH: return Direction.SOUTH;
            case WEST: return Direction.WEST;
            case EAST: return Direction.EAST;
            case DOWN: return Direction.DOWN;
            case UP:
            default:
                return Direction.UP;
        }
    }

    /**
     * Create a Cloudburst Direction from a WorldEdit Direction.
     *
     * @param direction the Bukkit BlockFace
     * @return a WorldEdit direction
     */
    public static org.cloudburstmc.server.math.Direction adapt(@Nullable Direction direction) {
        if (direction == null) {
            return null;
        }
        switch (direction) {
            case NORTH: return org.cloudburstmc.server.math.Direction.NORTH;
            case SOUTH: return org.cloudburstmc.server.math.Direction.SOUTH;
            case WEST: return org.cloudburstmc.server.math.Direction.WEST;
            case EAST: return org.cloudburstmc.server.math.Direction.EAST;
            case DOWN: return org.cloudburstmc.server.math.Direction.DOWN;
            case UP:
            default:
                return org.cloudburstmc.server.math.Direction.UP;
        }
    }

    /**
     * Create a Bukkit world from a WorldEdit world.
     *
     * @param world the WorldEdit world
     * @return a Bukkit world
     */
    public static org.cloudburstmc.server.level.Level adapt(World world) {
        return getAdapter().adapt(world);
    }

    /**
     * Create a WorldEdit location from a Bukkit location.
     *
     * @param location the Bukkit location
     * @return a WorldEdit location
     */
    public static Location adapt(org.bukkit.Location location) {
        checkNotNull(location);
        Vector3 position = asVector(location);
        return new Location(
                adapt(location.getWorld()),
                position,
                location.getYaw(),
                location.getPitch());
    }

    /**
     * Create a Bukkit location from a WorldEdit location.
     *
     * @param location the WorldEdit location
     * @return a Bukkit location
     */
    public static org.cloudburstmc.server.level.Location adapt(Location location) {
        checkNotNull(location);
        Vector3 position = location;
        return new org.bukkit.Location(
                adapt((World) location.getExtent()),
                position.getX(), position.getY(), position.getZ(),
                location.getYaw(),
                location.getPitch());
    }

    /**
     * Create a Bukkit location from a WorldEdit position with a Bukkit world.
     *
     * @param world the Bukkit world
     * @param position the WorldEdit position
     * @return a Bukkit location
     */
    public static org.cloudburstmc.server.level.Location adapt(org.cloudburstmc.server.level.Level world, Vector3 position) {
        checkNotNull(world);
        checkNotNull(position);
        return org.cloudburstmc.server.level.Location.from((float) position.getX(), (float) position.getY(), (float) position.getZ(),
                world);
    }

    /**
     * Create a Bukkit location from a WorldEdit position with a Bukkit world.
     *
     * @param world the Bukkit world
     * @param position the WorldEdit position
     * @return a Bukkit location
     */
    public static org.cloudburstmc.server.level.Location adapt(org.cloudburstmc.server.level.Level world, BlockVector3 position) {
        checkNotNull(world);
        checkNotNull(position);
        return org.cloudburstmc.server.level.Location.from((float) position.getX(), (float) position.getY(), (float) position.getZ(),
                world);
    }

    /**
     * Create a Bukkit location from a WorldEdit location with a Bukkit world.
     *
     * @param world the Bukkit world
     * @param location the WorldEdit location
     * @return a Bukkit location
     */
    public static org.cloudburstmc.server.level.Location adapt(org.cloudburstmc.server.level.Level world, Location location) {
        checkNotNull(world);
        checkNotNull(location);
        return org.cloudburstmc.server.level.Location.from((float) location.getX(), (float) location.getY(), (float) location.getZ(),
                location.getYaw(), location.getPitch(), world);
    }

    /**
     * Create a WorldEdit Vector from a Bukkit location.
     *
     * @param location The Bukkit location
     * @return a WorldEdit vector
     */
    public static Vector3 asVector(org.cloudburstmc.server.level.Location location) {
        checkNotNull(location);
        return Vector3.at(location.getX(), location.getY(), location.getZ());
    }

    /**
     * Create a WorldEdit BlockVector from a Bukkit location.
     *
     * @param location The Bukkit location
     * @return a WorldEdit vector
     */
    public static BlockVector3 asBlockVector(org.cloudburstmc.server.level.Location location) {
        checkNotNull(location);
        return BlockVector3.at(location.getX(), location.getY(), location.getZ());
    }

    /**
     * Create a WorldEdit entity from a Bukkit entity.
     *
     * @param entity the Bukkit entity
     * @return a WorldEdit entity
     */
    public static Entity adapt(org.cloudburstmc.server.entity.Entity entity) {
        return getAdapter().adapt(entity);
    }

    /**
     * Create a Bukkit Material form a WorldEdit ItemType.
     *
     * @param itemType The WorldEdit ItemType
     * @return The Bukkit Material
     */
    public static Identifier adapt(ItemType itemType) {
        return Identifier.fromString(itemType.getId());
    }

    /**
     * Create a Bukkit Material form a WorldEdit BlockType.
     *
     * @param blockType The WorldEdit BlockType
     * @return The Bukkit Material
     */
    public static Identifier adapt(BlockType blockType) {
        return Identifier.fromString(blockType.getId());
    }

    /**
     * Create a WorldEdit GameMode from a Bukkit one.
     *
     * @param gameMode Bukkit GameMode
     * @return WorldEdit GameMode
     */
    public static GameMode adapt(org.cloudburstmc.server.player.GameMode gameMode) {
        return GameMode.REGISTRY.get(gameMode.getName());
    }

    /**
     * Create a WorldEdit BiomeType from a Bukkit one.
     *
     * @param biome Bukkit Biome
     * @return WorldEdit BiomeType
     */
    public static BiomeType adapt(Biome biome) {
        return BiomeType.REGISTRY.get(biome.getId().toString());
    }

    public static Biome adapt(BiomeType biomeType) {
        return BiomeRegistry.get().getBiome(Identifier.fromString(biomeType.getId()));
    }

    /**
     * Create a WorldEdit EntityType from a Bukkit one.
     *
     * @param entityType Bukkit EntityType
     * @return WorldEdit EntityType
     */
    public static EntityType adapt(org.cloudburstmc.server.entity.EntityType<?> entityType) {
        return EntityTypes.get(entityType.getIdentifier().toString());
    }

    public static org.cloudburstmc.server.entity.EntityType<?> adapt(EntityType entityType) {
        return EntityRegistry.get().getEntityType(Identifier.fromString(entityType.getId()));
    }

    /**
     * Converts a Material to a BlockType.
     *
     * @param identifier The material
     * @return The blocktype
     */
    @Nullable
    public static BlockType asBlockType(Identifier identifier) {
        return BlockType.REGISTRY.get(identifier.toString());
    }

    /**
     * Converts a Material to a ItemType.
     *
     * @param identifier The material
     * @return The itemtype
     */
    @Nullable
    public static ItemType asItemType(Identifier identifier) {
        return ItemType.REGISTRY.get(identifier.toString());
    }

    private static final Int2ObjectMap<BlockState> blockStateCache = new Int2ObjectOpenHashMap<>();
    private static final Map<String, BlockState> blockStateStringCache = new HashMap<>();

    /**
     * Create a WorldEdit BlockState from a Bukkit BlockData.
     *
     * @param blockData The Bukkit BlockData
     * @return The WorldEdit BlockState
     */
    public static BlockState adapt(@NotNull org.cloudburstmc.server.block.BlockState blockData) {
        return BlockState.getFromInternalId(BlockPalette.INSTANCE.getRuntimeId(blockData));
    }

    /**
     * Create a Cloudburst BlockState from a WorldEdit BlockStateHolder.
     *
     * @param block The WorldEdit BlockStateHolder
     * @return The Cloudburst BlockState
     */
    public static <B extends BlockStateHolder<B>> org.cloudburstmc.server.block.BlockState adapt(@NotNull B block) {
        checkNotNull(block);
        int runtimeId = block.getInternalId();

        return BlockPalette.INSTANCE.getBlockState(runtimeId);
    }

    /**
     * Create a WorldEdit BlockState from a Bukkit ItemStack.
     *
     * @param itemStack The Bukkit ItemStack
     * @return The WorldEdit BlockState
     */
    public static BlockState asBlockState(Item itemStack) throws WorldEditException {
        return adapt(itemStack.getBlock());
    }

    /**
     * Create a WorldEdit BaseItemStack from a Bukkit ItemStack.
     *
     * @param item The Bukkit ItemStack
     * @return The WorldEdit BaseItemStack
     */
    public static BaseItemStack adapt(Item item) {
        return new BaseItemStack(asItemType(item.getId()), CloudburstAdapter.adapt(item.getTag()), item.getCount());
    }

    /**
     * Create a Cloudburst Item from a WorldEdit BaseItemStack.
     *
     * @param itemStack The WorldEdit BaseItemStack
     * @return The Bukkit ItemStack
     */
    public static Item adapt(BaseItemStack itemStack) {
        return Item.get(Identifier.fromString(itemStack.getType().getId()), 0, itemStack.getAmount(),
                CloudburstAdapter.adapt(itemStack.getNbtData()));
    }

    public static CompoundTag adapt(NbtMap nbtMap) {
        return adaptTag(nbtMap);
    }

    public static NbtMap adapt(CompoundTag compoundTag) {
        return (NbtMap) adaptTag(compoundTag);
    }

    private static final ImmutableBiMap<NbtType<?>, Class<? extends Tag>> TAG_CLASSES = ImmutableBiMap.<NbtType<?>, Class<? extends Tag>>builder()
            .put(NbtType.COMPOUND, CompoundTag.class)
            .put(NbtType.LIST, ListTag.class)
            .put(NbtType.BYTE_ARRAY, ByteArrayTag.class)
            .put(NbtType.INT_ARRAY, IntArrayTag.class)
            .put(NbtType.LONG_ARRAY, LongArrayTag.class)
            .put(NbtType.BYTE, ByteTag.class)
            .put(NbtType.SHORT, ShortTag.class)
            .put(NbtType.INT, IntTag.class)
            .put(NbtType.LONG, LongTag.class)
            .put(NbtType.FLOAT, FloatTag.class)
            .put(NbtType.DOUBLE, DoubleTag.class)
            .put(NbtType.STRING, StringTag.class)
            .build();

    @SuppressWarnings("unchecked")
    private static <T extends Tag> T adaptTag(Object tag) {
        if (tag instanceof NbtMap) {
            NbtMap nbtMap = (NbtMap) tag;
            Map<String, Tag> tags = new LinkedHashMap<>();
            nbtMap.forEach((key, value) -> tags.put(key, adaptTag(value)));
            return (T) new CompoundTag(tags);
        } else if (tag instanceof NbtList) {
            NbtList<?> nbtList = (NbtList<?>) tag;
            Class<? extends Tag> clazz = TAG_CLASSES.get(nbtList.getType());
            List<? extends Tag> list = new ArrayList<>(nbtList.size());
            for (Object value : nbtList) {
                list.add(adaptTag(value));
            }
            return (T) new ListTag(clazz, list);
        } else if (tag instanceof byte[]) {
            return (T) new ByteArrayTag((byte[]) tag);
        } else if (tag instanceof int[]) {
            return (T) new IntArrayTag((int[]) tag);
        } else if (tag instanceof long[]) {
            return (T) new LongArrayTag((long[]) tag);
        } else if (tag instanceof Byte) {
            return (T) new ByteTag((byte) tag);
        } else if (tag instanceof Short) {
            return (T) new ShortTag((short) tag);
        } else if (tag instanceof Integer) {
            return (T) new IntTag((int) tag);
        } else if (tag instanceof Long) {
            return (T) new LongTag((long) tag);
        } else if (tag instanceof Float) {
            return (T) new FloatTag((float) tag);
        } else if (tag instanceof Double) {
            return (T) new DoubleTag((double) tag);
        } else if (tag instanceof String) {
            return (T) new StringTag((String) tag);
        }
        throw new IllegalArgumentException("Unknown tag type " + tag.getClass().getName());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object adaptTag(Tag tag) {
        if (tag instanceof CompoundTag) {
            NbtMapBuilder builder = NbtMap.builder();
            ((CompoundTag) tag).getValue().forEach((key, value) -> builder.put(key, adaptTag(value)));
            return builder.build();
        } else if (tag instanceof ListTag) {
            ListTag listTag = (ListTag) tag;
            List<Object> list = new ArrayList<>(listTag.getValue().size());
            for (Tag value : listTag.getValue()) {
                list.add(adaptTag(value));
            }
            NbtType<?> type = TAG_CLASSES.inverse().get(listTag.getType());
            return new NbtList(type, list);
        } else {
            return tag.getValue();
        }
    }

    public static void sendFakeChunk(org.cloudburstmc.server.level.Level world, Player cloudburstPlayer, ChunkPacket packet) {

    }
}
