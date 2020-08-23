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
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableBiMap;
import com.nukkitx.nbt.NbtList;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import com.sk89q.jnbt.*;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItemStack;
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
import net.jpountz.util.UnsafeUtils;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.command.CommandSender;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.biome.Biome;
import org.cloudburstmc.server.level.chunk.ChunkSection;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BiomeRegistry;
import org.cloudburstmc.server.registry.EntityRegistry;
import org.cloudburstmc.server.utils.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sun.misc.Unsafe;

import java.util.*;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Adapts between Cloudburst and WorldEdit equivalent objects.
 */
public class CloudburstAdapter {

    private CloudburstAdapter() {
    }

    private static final ParserContext TO_BLOCK_CONTEXT = new ParserContext();

    private static final int CHUNKSECTION_BASE;
    private static final int CHUNKSECTION_SHIFT;

    static {
        TO_BLOCK_CONTEXT.setRestricted(false);

        Unsafe unsafe = UnsafeUtils.getUNSAFE();
        CHUNKSECTION_BASE = unsafe.arrayBaseOffset(ChunkSection[].class);
        int scale = unsafe.arrayIndexScale(ChunkSection[].class);
        if ((scale & (scale - 1)) != 0) {
            throw new Error("data type scale not a power of two");
        }
        CHUNKSECTION_SHIFT = 31 - Integer.numberOfLeadingZeros(scale);
    }

    /**
     * Checks equality between a WorldEdit BlockType and a Bukkit Material.
     *
     * @param blockType The WorldEdit BlockType
     * @param type      The Bukkit Material
     * @return If they are equal
     */
    public static boolean equals(BlockType blockType, Identifier type) {
        return Identifier.fromString(blockType.getId()) == type;
    }

    /**
     * Create a WorldEdit world from a Bukkit world.
     *
     * @param level the Bukkit world
     * @return a WorldEdit world
     */
    public static CloudburstWorld adapt(Level level) {
        checkNotNull(level);
        return new CloudburstWorld(level);
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
        } /*else if (actor instanceof CloudburstBlockCommandSender) {
            return ((CloudburstBlockCommandSender) actor).getSender();
        } FIXME: Command blocks don't exist in Cloudburst yet */
        return ((CloudburstCommandSender) actor).getSender();
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
            case NORTH:
                return Direction.NORTH;
            case SOUTH:
                return Direction.SOUTH;
            case WEST:
                return Direction.WEST;
            case EAST:
                return Direction.EAST;
            case DOWN:
                return Direction.DOWN;
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
            case NORTH:
                return org.cloudburstmc.server.math.Direction.NORTH;
            case SOUTH:
                return org.cloudburstmc.server.math.Direction.SOUTH;
            case WEST:
                return org.cloudburstmc.server.math.Direction.WEST;
            case EAST:
                return org.cloudburstmc.server.math.Direction.EAST;
            case DOWN:
                return org.cloudburstmc.server.math.Direction.DOWN;
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
        checkNotNull(world);
        if (world instanceof CloudburstWorld) {
            return ((CloudburstWorld) world).getWorld();
        } else {
            Level match = Server.getInstance().getLevelByName(world.getName());
            if (match != null) {
                return match;
            } else {
                throw new IllegalArgumentException("Can't find a Bukkit world for " + world);
            }
        }
    }

    /**
     * Create a WorldEdit location from a Bukkit location.
     *
     * @param location the Bukkit location
     * @return a WorldEdit location
     */
    public static Location adapt(org.cloudburstmc.server.level.Location location) {
        checkNotNull(location);
        Vector3 position = asVector(location);
        return new Location(
                adapt(location.getLevel()),
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
        return org.cloudburstmc.server.level.Location.from(
                (float) location.getX(),
                (float) location.getY(),
                (float) location.getZ(),
                location.getYaw(),
                location.getPitch(),
                adapt((World) location.getExtent())
        );
    }

    /**
     * Create a Bukkit location from a WorldEdit position with a Bukkit world.
     *
     * @param world    the Bukkit world
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
     * @param world    the Bukkit world
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
     * @param world    the Bukkit world
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
    public static CloudburstEntity adapt(org.cloudburstmc.server.entity.Entity entity) {
        checkNotNull(entity);
        return new CloudburstEntity(entity);
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
        ItemType type = ItemType.REGISTRY.get(identifier.toString().toLowerCase());
        Preconditions.checkNotNull(type, "No type found for %s", identifier);
        return type;
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
        if (compoundTag == null) return NbtMap.EMPTY;
        return (NbtMap) adaptTag(compoundTag);
    }

    public static Tag adapt(Tag tag) {
        return (Tag) adaptTag(tag);
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
    public static <T extends Tag> T adaptTag(Object tag) {
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

    /*
    NMS conversion
     */
    public static ChunkSection newChunkSection(final int layer, final char[] blocks, boolean fastmode) {
        return newChunkSection(layer, null, blocks, fastmode);
    }

    public static ChunkSection newChunkSection(final int layer, final Function<Integer, char[]> get, char[] set, boolean fastmode) {
//        if (set == null) {
//            return newChunkSection(layer);
//        }
//        final int[] blockToPalette = FaweCache.IMP.blockToPalette.get();
//        final int[] paletteToBlock = FaweCache.IMP.paletteToBlock.get();
//        final long[] blockStates = FaweCache.IMP.blockStates.get();
//        final int[] blocksCopy = FaweCache.IMP.sectionBlocks.get();
//        try {
//            int[] num_palette_buffer = new int[1];
//            Map<BlockVector3, Integer> ticking_blocks = new HashMap<>();
//            int air;
//            if (get == null) {
//                air = createPalette(blockToPalette, paletteToBlock, blocksCopy, num_palette_buffer,
//                        set, ticking_blocks, fastmode);
//            } else {
//                air = createPalette(layer, blockToPalette, paletteToBlock, blocksCopy,
//                        num_palette_buffer, get, set, ticking_blocks, fastmode);
//            }
//            int num_palette = num_palette_buffer[0];
//            // BlockStates
//            int bitsPerEntry = MathMan.log2nlz(num_palette - 1);
//            if (Settings.IMP.PROTOCOL_SUPPORT_FIX || num_palette != 1) {
//                bitsPerEntry = Math.max(bitsPerEntry, 4); // Protocol support breaks <4 bits per entry
//            } else {
//                bitsPerEntry = Math.max(bitsPerEntry, 1); // For some reason minecraft needs 4096 bits to store 0 entries
//            }
//
//            final int blocksPerLong = MathMan.floorZero((double) 64 / bitsPerEntry);
//            final int blockBitArrayEnd = MathMan.ceilZero((float) 4096 / blocksPerLong);
//
//            if (num_palette == 1) {
//                for (int i = 0; i < blockBitArrayEnd; i++) {
//                    blockStates[i] = 0;
//                }
//            } else {
//                final BitArrayUnstretched bitArray = new BitArrayUnstretched(bitsPerEntry, blockStates);
//                bitArray.fromRaw(blocksCopy);
//            }
//
//            ChunkSection section = newChunkSection(layer);
//            // set palette & data bits
//            final DataPaletteBlock<IBlockData> dataPaletteBlocks = section.getBlocks();
//            // private DataPalette<T> h;
//            // protected DataBits a;
//            final long[] bits = Arrays.copyOfRange(blockStates, 0, blockBitArrayEnd);
//            final DataBits nmsBits = new DataBits(bitsPerEntry, 4096, bits);
//            //                palette = new DataPaletteHash<>(Block.REGISTRY_ID, bitsPerEntry, dataPaletteBlocks, GameProfileSerializer::d, GameProfileSerializer::a);
//            final DataPalette<IBlockData> palette = new DataPaletteLinear<>(Block.REGISTRY_ID, bitsPerEntry, dataPaletteBlocks, GameProfileSerializer::c);
//
//            // set palette
//            for (int i = 0; i < num_palette; i++) {
//                final int ordinal = paletteToBlock[i];
//                blockToPalette[ordinal] = Integer.MAX_VALUE;
//                final BlockState state = BlockTypesCache.states[ordinal];
//                final IBlockData ibd = ((BlockMaterial1161) state.getMaterial()).getState();
//                palette.a(ibd);
//            }
//            try {
//                fieldBits.set(dataPaletteBlocks, nmsBits);
//                fieldPalette.set(dataPaletteBlocks, palette);
//                fieldSize.set(dataPaletteBlocks, bitsPerEntry);
//                setCount(ticking_blocks.size(), 4096 - air, section);
//                if (!fastmode) {
//                    ticking_blocks.forEach((pos, ordinal) -> section
//                            .setType(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ(),
//                                    Block.getByCombinedId(ordinal)));
//                }
//            } catch (final IllegalAccessException | NoSuchFieldException e) {
//                throw new RuntimeException(e);
//            }
//
//            return section;
//        } catch (final Throwable e) {
//            Arrays.fill(blockToPalette, Integer.MAX_VALUE);
//            throw e;
//        }
        throw new UnsupportedOperationException();
    }

    private static ChunkSection newChunkSection(int layer) {
        return new ChunkSection();
    }

    public static boolean setSectionAtomic(ChunkSection[] sections, ChunkSection expected, ChunkSection value, int layer) {
        long offset = ((long) layer << CHUNKSECTION_SHIFT) + CHUNKSECTION_BASE;
        if (layer >= 0 && layer < sections.length) {
            return UnsafeUtils.getUNSAFE().compareAndSwapObject(sections, offset, expected, value);
        }
        return false;
    }

    public static void sendChunk(Level level, int chunkX, int chunkZ, int mask, boolean lighting) {
//        PlayerChunk playerChunk = getPlayerChunk(level, chunkX, chunkZ);
//        if (playerChunk == null) {
//            return;
//        }
//        if (playerChunk.hasBeenLoaded()) {
//            TaskManager.IMP.sync(() -> {
//                try {
//                    int dirtyBits = fieldDirtyBits.getInt(playerChunk);
//                    if (dirtyBits == 0) {
//                        level.getChunkProvider().playerChunkMap.a(playerChunk);
//                    }
//                    if (mask == 0) {
//                        dirtyBits = 65535;
//                    } else {
//                        dirtyBits |= mask;
//                    }
//
//                    fieldDirtyBits.set(playerChunk, dirtyBits);
//                    fieldDirtyCount.set(playerChunk, 64);
//
//                    if (lighting) {
//                        ChunkCoordIntPair chunkCoordIntPair = new ChunkCoordIntPair(chunkX, chunkZ);
//                        boolean trustEdges = false; //Added in 1.16.1 Not sure what it does.
//                        PacketPlayOutLightUpdate packet = new PacketPlayOutLightUpdate(chunkCoordIntPair, level.getChunkProvider().getLightEngine(), trustEdges);
//                        playerChunk.players.a(chunkCoordIntPair, false).forEach(p -> {
//                            p.playerConnection.sendPacket(packet);
//                        });
//                    }
//
//                } catch (IllegalAccessException e) {
//                    e.printStackTrace();
//                }
//                return null;
//            });
//        }
    }
}
