package com.boydti.fawe.cloudburst.adapter;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.beta.implementation.blocks.CharBlocks;
import com.boydti.fawe.beta.implementation.blocks.CharGetBlocks;
import com.boydti.fawe.beta.implementation.queue.QueueHandler;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.collection.AdaptedMap;
import com.google.common.base.Suppliers;
import com.google.common.collect.Iterables;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.sk89q.jnbt.*;
import com.sk89q.worldedit.cloudburst.CloudburstAdapter;
import com.sk89q.worldedit.internal.Constants;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.Location;
import org.cloudburstmc.server.level.biome.Biome;
import org.cloudburstmc.server.level.chunk.BlockStorage;
import org.cloudburstmc.server.level.chunk.Chunk;
import org.cloudburstmc.server.level.chunk.ChunkSection;
import org.cloudburstmc.server.level.chunk.bitarray.BitArray;
import org.cloudburstmc.server.registry.BiomeRegistry;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.registry.EntityRegistry;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.NibbleArray;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CloudburstGetBlocks extends CharGetBlocks {

    private static final Logger log = LoggerFactory.getLogger(CloudburstGetBlocks.class);

    private static final Function<Vector3i, BlockVector3> posNms2We = v -> BlockVector3.at(v.getX(), v.getY(), v.getZ());
    private static final Function<BlockEntity, CompoundTag> nmsTile2We = tileEntity -> new LazyCompoundTag(Suppliers.memoize(() -> {
        NbtMapBuilder builder = NbtMap.builder();
        tileEntity.saveAdditionalData(builder);

        return builder.build();
    }));
    public ChunkSection[] sections;
    public Chunk cloudChunk;
    public Level world;
    public int chunkX;
    public int chunkZ;
    public NibbleArray[] blockLight = new NibbleArray[16];
    public NibbleArray[] skyLight = new NibbleArray[16];

    public CloudburstGetBlocks(Level world, int chunkX, int chunkZ) {
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        Biome biome = BiomeRegistry.get().getBiome(this.getChunk().getBiome(x & 0xf, z & 0xf));
        return CloudburstAdapter.adapt(biome);
    }

    @Override
    public CompoundTag getTile(int x, int y, int z) {
        BlockEntity entity = this.getChunk().getBlockEntity((x & 15), y, (z & 15));
        if (entity == null) {
            return null;
        }
        return CloudburstAdapter.adapt(entity.getChunkTag());
    }

    @Override
    public Map<BlockVector3, CompoundTag> getTiles() {
        Map<Vector3i, BlockEntity> nmsTiles = this.getChunk().getBlockEntities().stream()
                .collect(Collectors.toMap(BlockEntity::getPosition, be -> be));
        if (nmsTiles.isEmpty()) {
            return Collections.emptyMap();
        }
        return AdaptedMap.immutable(nmsTiles, posNms2We, nmsTile2We);
    }

    @Override
    public int getSkyLight(int x, int y, int z) {
        return this.getChunk().getSkyLight(x & 0xf, y, z & 0xf);
    }

    @Override
    public int getEmmittedLight(int x, int y, int z) {
        return this.getChunk().getBlockLight(x & 0xf, y, z & 0xf);
    }

    @Override
    public CompoundTag getEntity(UUID uuid) {
        Entity entity = world.getEntity(uuid.getLeastSignificantBits());
        if (entity != null) {
            return CloudburstAdapter.adapt(entity).getState().getNbtData();
        }
//        for (List<Entity> entry : this.getChunk().getEntitySlices()) {
//            if (entry != null) {
//                for (Entity ent : entry) {
//                    if (uuid.equals(ent.getUniqueID())) {
//                        org.bukkit.entity.Entity bukkitEnt = ent.getBukkitEntity();
//                        return BukkitAdapter.adapt(bukkitEnt).getState().getNbtData();
//                    }
//                }
//            }
//        }
        return null;
    }

    @Override
    public Set<CompoundTag> getEntities() {
        Set<Entity> entities = this.getChunk().getEntities();
        int size = entities.size();
        if (size == 0) {
            return Collections.emptySet();
        }
        int finalSize = size;
        return new AbstractSet<CompoundTag>() {
            @Override
            public int size() {
                return finalSize;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public boolean contains(Object get) {
                if (!(get instanceof CompoundTag)) {
                    return false;
                }
                CompoundTag getTag = (CompoundTag) get;
                Map<String, Tag> value = getTag.getValue();
                long uniqueId = ((LongTag) value.get("UniqueId")).getValue();
                for (Entity entity : CloudburstGetBlocks.this.getChunk().getEntities()) {
                    long id = entity.getUniqueId();
                    if (id == uniqueId) {
                        return true;
                    }
                }

                return false;
            }

            @NotNull
            @Override
            public Iterator<CompoundTag> iterator() {
                Iterable<CompoundTag> result = Iterables.transform(Iterables.concat(entities), input -> {
                    NbtMapBuilder builder = NbtMap.builder();
                    input.saveAdditionalData(builder);
                    return (CompoundTag) CloudburstAdapter.adapt(builder.build());
                });
                return result.iterator();
            }
        };
    }

    private void updateGet(CloudburstGetBlocks get, Chunk nmsChunk, ChunkSection[] sections, ChunkSection section, char[] arr, int layer) {
        synchronized (get) {
            if (this.cloudChunk != nmsChunk) {
                this.cloudChunk = nmsChunk;
                this.sections = sections.clone();
                this.reset();
            }
            if (this.sections == null) {
                this.sections = sections.clone();
            }
            if (this.sections[layer] != section) {
                this.sections[layer] = new ChunkSection[]{section}.clone()[0];
            }
            this.blocks[layer] = arr;
        }
    }

    private void removeEntity(Entity entity) {
        entity.close();
    }

    public Chunk ensureLoaded(int chunkX, int chunkZ) {
        return this.world.getChunk(chunkX, chunkZ);
    }

    @Override
    public <T extends Future<T>> T call(IChunkSet set, Runnable finalizer) {
        try {
            Level level = world;
            Chunk chunk = this.ensureLoaded(chunkX, chunkZ);
            boolean fastmode = set.isFastMode() && Settings.IMP.QUEUE.NO_TICK_FASTMODE;

            // Remove existing tiles
            {
                // Create a copy so that we can remove blocks
                Set<BlockEntity> blockEntities = chunk.getBlockEntities();
                if (!blockEntities.isEmpty()) {
                    for (BlockEntity blockEntity : blockEntities) {
                        Vector3i pos = blockEntity.getPosition();
                        final int lx = pos.getX() & 15;
                        final int ly = pos.getY();
                        final int lz = pos.getZ() & 15;
                        final int layer = ly >> 4;
                        if (!set.hasSection(layer)) {
                            continue;
                        }

                        int ordinal = set.getBlock(lx, ly, lz).getOrdinal();
                        if (ordinal != 0) {
                            chunk.removeBlockEntity(blockEntity);
                        }
                    }
                }
            }

            int bitMask = 0;
            synchronized (chunk) {
                ChunkSection[] sections = chunk.getSections();

                for (int layer = 0; layer < 16; layer++) {
                    if (!set.hasSection(layer)) {
                        continue;
                    }

                    bitMask |= 1 << layer;

                    char[] setArr = set.load(layer);
                    ChunkSection newSection;
                    ChunkSection existingSection = sections[layer];
                    if (existingSection == null) {
                        newSection = CloudburstAdapter.newChunkSection(layer, setArr, fastmode);
                        if (CloudburstAdapter.setSectionAtomic(sections, null, newSection, layer)) {
                            this.updateGet(this, chunk, sections, newSection, setArr, layer);
                            continue;
                        } else {
                            existingSection = sections[layer];
                            if (existingSection == null) {
                                log.error("Skipping invalid null section. chunk:" + chunkX + "," + chunkZ + " layer: " + layer);
                                continue;
                            }
                        }
                    }

                    // FIXME: We don't have this in cloudburst
//                    BukkitAdapter1161.fieldTickingBlockCount.set(existingSection, (short) 0);

                    //ensure that the server doesn't try to tick the chunksection while we're editing it.
                    Lock lock = this.getChunk().writeLockable();

                    synchronized (this) {
                        lock.lock();
                        try {
                            if (this.cloudChunk != chunk) {
                                this.cloudChunk = chunk;
                                this.sections = null;
                                this.reset();
                            } else if (existingSection != this.getSections()[layer]) {
                                this.sections[layer] = existingSection;
                                this.reset();
                            } else if (!Arrays.equals(this.update(layer, new char[4096]), this.load(layer))) {
                                this.reset(layer);
                            }
                            newSection = CloudburstAdapter.newChunkSection(layer, this::load, setArr, fastmode);
                            if (!CloudburstAdapter.setSectionAtomic(sections, existingSection, newSection, layer)) {
                                log.error("Failed to set chunk section:" + chunkX + "," + chunkZ + " layer: " + layer);
                            } else {
                                this.updateGet(this, chunk, sections, newSection, setArr, layer);
                            }
                        } finally {
                            lock.unlock();
                        }
                    }
                }

                // Biomes
                BiomeType[] biomes = set.getBiomes();
                if (biomes != null) {
                    // set biomes
                    for (int z = 0, i = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++, i++) {
                            final BiomeType biome = biomes[i];
                            if (biome != null) {
                                final Biome cloudBiome = CloudburstAdapter.adapt(biome);

                                for (int y = 0; y < FaweCache.IMP.WORLD_HEIGHT; y++) {
                                    chunk.setBiome(x >> 2, z >> 2, BiomeRegistry.get().getRuntimeId(cloudBiome));
                                }
                            }
                        }
                    }
                }

                boolean lightUpdate = false;

                // Lighting
                char[][] light = set.getLight();
                if (light != null) {
                    lightUpdate = true;
                    try {
                        this.fillLightNibble(light, false);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }

                char[][] skyLight = set.getSkyLight();
                if (skyLight != null) {
                    lightUpdate = true;
                    try {
                        this.fillLightNibble(skyLight, true);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }

                Runnable[] syncTasks = null;

                int bx = chunkX << 4;
                int bz = chunkZ << 4;

                Set<UUID> entityRemoves = set.getEntityRemoves();
                if (entityRemoves != null && !entityRemoves.isEmpty()) {
                    if (syncTasks == null) {
                        syncTasks = new Runnable[3];
                    }

                    syncTasks[2] = () -> {
                        final Set<Entity> entities = chunk.getEntities();


                        if (!entities.isEmpty()) {
                            final Iterator<Entity> iter = entities.iterator();
                            while (iter.hasNext()) {
                                final Entity entity = iter.next();
                                if (entityRemoves.contains(new UUID(0, entity.getUniqueId()))) {
                                    iter.remove();
                                    this.removeEntity(entity);
                                }
                            }
                        }

                    };
                }

                Set<CompoundTag> entities = set.getEntities();
                if (entities != null && !entities.isEmpty()) {
                    if (syncTasks == null) {
                        syncTasks = new Runnable[2];
                    }

                    syncTasks[1] = () -> {
                        EntityRegistry registry = EntityRegistry.get();
                        for (final CompoundTag nativeTag : entities) {
                            final Map<String, Tag> entityTagMap = nativeTag.getValue();
                            final StringTag idTag = (StringTag) entityTagMap.get("Id");
                            final ListTag posTag = (ListTag) entityTagMap.get("Pos");
                            final ListTag rotTag = (ListTag) entityTagMap.get("Rotation");
                            if (idTag == null || posTag == null || rotTag == null) {
                                log.debug("Unknown entity tag: " + nativeTag);
                                continue;
                            }
                            final float x = posTag.getFloat(0);
                            final float y = posTag.getFloat(1);
                            final float z = posTag.getFloat(2);
                            final float yaw = rotTag.getFloat(0);
                            final float pitch = rotTag.getFloat(1);
                            final String id = idTag.getValue();

                            EntityType<?> type = registry.getEntityType(Identifier.fromString(id));
                            if (type != null) {
                                Entity entity = registry.newEntity(type, Location.from(
                                        x,
                                        y,
                                        z,
                                        yaw,
                                        pitch,
                                        level
                                ));
                                if (entity != null) {
                                    entityTagMap.put("UniqueId", new LongTag(entity.getUniqueId()));

                                    final NbtMapBuilder tag = CloudburstAdapter.adapt(nativeTag).toBuilder();
                                    for (final String name : Constants.NO_COPY_ENTITY_NBT_FIELDS) {
                                        tag.remove(name);
                                    }

                                    entity.loadAdditionalData(tag.build());
                                }
                            }
                        }
                    };

                }

                // set tiles
                Map<BlockVector3, CompoundTag> tiles = set.getTiles();
                if (tiles != null && !tiles.isEmpty()) {
                    if (syncTasks == null) {
                        syncTasks = new Runnable[1];
                    }

                    syncTasks[0] = () -> {
                        for (final Map.Entry<BlockVector3, CompoundTag> entry : tiles.entrySet()) {
                            final CompoundTag nativeTag = entry.getValue();
                            final BlockVector3 blockHash = entry.getKey();
                            final int x = blockHash.getX() + bx;
                            final int y = blockHash.getY();
                            final int z = blockHash.getZ() + bz;
                            final Vector3i pos = Vector3i.from(x, y, z);

                            synchronized (level) {
                                BlockEntity tileEntity = level.getBlockEntity(pos);
                                if (tileEntity != null && tileEntity.isClosed()) {
                                    level.removeBlockEntity(tileEntity);
                                    tileEntity = level.getBlockEntity(pos);
                                }
                                if (tileEntity != null) {
                                    final NbtMap tag = CloudburstAdapter.adapt(nativeTag);
                                    tileEntity.loadAdditionalData(tag);
                                }
                            }
                        }
                    };
                }

                Runnable callback;
                if (bitMask == 0 && biomes == null && !lightUpdate) {
                    callback = null;
                } else {
                    int finalMask = bitMask != 0 ? bitMask : lightUpdate ? set.getBitMask() : 0;
                    boolean finalLightUpdate = lightUpdate;
                    callback = () -> {
                        // Set Modified
                        chunk.setDirty(); // Set Modified
                        // send to player
                        CloudburstAdapter.sendChunk(level, chunkX, chunkZ, finalMask, finalLightUpdate);
                        if (finalizer != null) {
                            finalizer.run();
                        }
                    };
                }
                if (syncTasks != null) {
                    QueueHandler queueHandler = Fawe.get().getQueueHandler();
                    Runnable[] finalSyncTasks = syncTasks;

                    // Chain the sync tasks and the callback
                    Callable<Future> chain = () -> {
                        try {
                            // Run the sync tasks
                            for (Runnable task : finalSyncTasks) {
                                if (task != null) {
                                    task.run();
                                }
                            }
                            if (callback == null) {
                                if (finalizer != null) {
                                    finalizer.run();
                                }
                                return null;
                            } else {
                                return queueHandler.async(callback, null);
                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                            throw e;
                        }
                    };
                    return (T) (Future) queueHandler.sync(chain);
                } else {
                    if (callback == null) {
                        if (finalizer != null) {
                            finalizer.run();
                        }
                    } else {
                        callback.run();
                    }
                }
            }
            return null;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    private static final Field STORAGE_BIT_ARRAY;
    private static final Field STORAGE_PALETTE;

    static {
        try {
            STORAGE_BIT_ARRAY = BlockStorage.class.getDeclaredField("bitArray");
            STORAGE_BIT_ARRAY.setAccessible(true);
            STORAGE_PALETTE = BlockStorage.class.getDeclaredField("palette");
            STORAGE_PALETTE.setAccessible(true);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public synchronized char[] update(int layer, char[] data) {
        ChunkSection section = this.getSections()[layer];
        // Section is null, return empty array
        if (section == null) {
            data = new char[4096];
            Arrays.fill(data, (char) 1);
            return data;
        }
        if (data == null || data == FaweCache.IMP.EMPTY_CHAR_4096) {
            data = new char[4096];
            Arrays.fill(data, (char) 1);
        }
        Lock lock = this.getChunk().writeLockable();
        lock.lock();
        try {
            // Efficiently convert ChunkSection to raw data
            try {
                BlockStorage storage = section.getBlockStorageArray()[0];
                BitArray array = (BitArray) STORAGE_BIT_ARRAY.get(storage);
                @SuppressWarnings("unchecked")
                List<Integer> palette = (List<Integer>) STORAGE_PALETTE.get(storage);

                int bitsPerEntry = array.getVersion().getId();
                int num_palette = palette.size();

                char[] paletteToOrdinal = FaweCache.IMP.PALETTE_TO_BLOCK_CHAR.get();
                try {
                    if (num_palette != 1) {
                        for (int i = 0; i < num_palette; i++) {
                            char ordinal = this.ordinal(palette.get(i));
                            paletteToOrdinal[i] = ordinal;
                        }
                        for (int i = 0; i < 4096; i++) {
                            char paletteVal = data[i];
                            char val = paletteToOrdinal[paletteVal];
                            if (val == Character.MAX_VALUE) {
                                val = this.ordinal(palette.get(i));
                                paletteToOrdinal[i] = val;
                            }
                            // Don't read "empty".
                            if (val == 0) {
                                val = 1;
                            }
                            data[i] = val;
                        }
                    } else {
                        char ordinal = this.ordinal(palette.get(0));
                        // Don't read "empty".
                        if (ordinal == 0) {
                            ordinal = 1;
                        }
                        Arrays.fill(data, ordinal);
                    }
                } finally {
                    for (int i = 0; i < num_palette; i++) {
                        paletteToOrdinal[i] = Character.MAX_VALUE;
                    }
                }
                return data;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        } finally {
            lock.unlock();
        }
    }

    private final char ordinal(int runtimeId) {
        BlockState state = BlockRegistry.get().getBlock(runtimeId);
        return CloudburstAdapter.adapt(state).getOrdinalChar();
    }

    public ChunkSection[] getSections() {
        ChunkSection[] tmp = sections;
        if (tmp == null) {
            synchronized (this) {
                tmp = sections;
                if (tmp == null) {
                    Chunk chunk = this.getChunk();
                    sections = tmp = chunk.getSections();
                }
            }
        }
        return tmp;
    }

    public Chunk getChunk() {
        Chunk tmp = cloudChunk;
        if (tmp == null) {
            synchronized (this) {
                tmp = cloudChunk;
                if (tmp == null) {
                    cloudChunk = tmp = this.ensureLoaded(chunkX, chunkZ);
                }
            }
        }
        return tmp;
    }

    private void fillLightNibble(char[][] light, boolean skyLight) {
        Lock lock = cloudChunk.writeLockable();
        lock.lock();
        try {
            for (int Y = 0; Y < 16; Y++) {
                if (light[Y] == null) {
                    continue;
                }
                ChunkSection section = cloudChunk.getSection(Y);
                if (section == null) continue;

                NibbleArray nibble = skyLight ? section.getSkyLightArray() : section.getBlockLightArray();
                for (int i = 0; i < 4096; i++) {
                    if (light[Y][i] < 16) {
                        nibble.set(i, (byte) light[Y][i]);
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean hasSection(int layer) {
        return this.getSections()[layer] != null;
    }

    @Override
    public boolean trim(boolean aggressive) {
        skyLight = new NibbleArray[16];
        blockLight = new NibbleArray[16];
        if (aggressive) {
            sections = null;
            cloudChunk = null;
            return super.trim(true);
        } else {
            for (int i = 0; i < 16; i++) {
                if (!this.hasSection(i) || super.sections[i] == CharBlocks.EMPTY) {
                    continue;
                }
                ChunkSection existing = this.getSections()[i];
                try {
                    BlockStorage storage = existing.getBlockStorageArray()[0];
                    @SuppressWarnings("unchecked")
                    List<Integer> palette = (List<Integer>) STORAGE_PALETTE.get(storage);

                    if (palette.size() == 1) {
                        //If the cached palette size is 1 then no blocks can have been changed i.e. do not need to update these chunks.
                        continue;
                    }
                    super.trim(false, i);
                } catch (IllegalAccessException ignored) {
                    super.trim(false, i);
                }
            }
            return true;
        }
    }
}