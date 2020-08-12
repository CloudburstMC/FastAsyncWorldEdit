package com.boydti.fawe.bukkit.adapter;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.beta.implementation.blocks.CharBlocks;
import com.boydti.fawe.beta.implementation.blocks.CharGetBlocks;
import com.boydti.fawe.beta.implementation.queue.QueueHandler;
import com.boydti.fawe.bukkit.adapter.mc1161.BukkitAdapter1161;
import com.boydti.fawe.bukkit.adapter.mc1161.BukkitGetBlocks1161;
import com.boydti.fawe.bukkit.adapter.mc1161.nbt.LazyCompoundTag1161;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.collection.AdaptedMap;
import com.boydti.fawe.object.collection.BitArrayUnstretched;
import com.google.common.base.Suppliers;
import com.google.common.collect.Iterables;
import com.sk89q.jnbt.*;
import com.sk89q.worldedit.bukkit.CloudburstAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.bukkit.adapter.impl.FAWESpigotV116R1;
import com.sk89q.worldedit.internal.Constants;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.biome.Biome;
import org.cloudburstmc.server.level.chunk.Chunk;
import org.cloudburstmc.server.level.chunk.ChunkSection;
import org.cloudburstmc.server.registry.BiomeRegistry;
import org.cloudburstmc.server.utils.NibbleArray;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Function;

import static org.slf4j.LoggerFactory.getLogger;

public class CloudburstGetBlocks extends CharGetBlocks {

    private static final Logger log = LoggerFactory.getLogger(BukkitGetBlocks1161.class);

    private static final Function<BlockPosition, BlockVector3> posNms2We = v -> BlockVector3.at(v.getX(), v.getY(), v.getZ());
    private static final Function<TileEntity, CompoundTag> nmsTile2We = tileEntity -> new LazyCompoundTag1161(Suppliers.memoize(() -> tileEntity.save(new NBTTagCompound())));
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
        Map<BlockPosition, TileEntity> nmsTiles = this.getChunk().getTileEntities();
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

        List<Entity>[] slices = this.getChunk().getEntitySlices();
        int size = 0;
        for (List<Entity> slice : slices) {
            if (slice != null) {
                size += slice.size();
            }
        }
        if (slices.length == 0) {
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
                CompoundTag getParts = (CompoundTag) value.get("UUID");
                UUID getUUID = new UUID(getParts.getLong("Most"), getParts.getLong("Least"));
                for (List<Entity> slice : slices) {
                    if (slice != null) {
                        for (Entity entity : slice) {
                            UUID uuid = entity.getUniqueID();
                            if (uuid.equals(getUUID)) {
                                return true;
                            }
                        }
                    }
                }
                return false;
            }

            @NotNull
            @Override
            public Iterator<CompoundTag> iterator() {
                Iterable<CompoundTag> result = Iterables.transform(Iterables.concat(slices), new com.google.common.base.Function<Entity, CompoundTag>() {
                    @Nullable
                    @Override
                    public CompoundTag apply(@Nullable Entity input) {
                        BukkitImplAdapter adapter = WorldEditPlugin.getInstance().getBukkitImplAdapter();
                        NBTTagCompound tag = new NBTTagCompound();
                        return (CompoundTag) adapter.toNative(input.save(tag));
                    }
                });
                return result.iterator();
            }
        };
    }

    private void updateGet(BukkitGetBlocks1161 get, Chunk nmsChunk, ChunkSection[] sections, ChunkSection section, char[] arr, int layer) {
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
        entity.die();
        entity.valid = false;
    }

    public Chunk ensureLoaded(int chunkX, int chunkZ) {
        return this.world.getChunk(chunkX, chunkZ);
    }

    @Override
    public <T extends Future<T>> T call(IChunkSet set, Runnable finalizer) {
        try {
            WorldServer nmsWorld = world;
            Chunk nmsChunk = this.ensureLoaded(chunkX, chunkZ);
            boolean fastmode = set.isFastMode() && Settings.IMP.QUEUE.NO_TICK_FASTMODE;

            // Remove existing tiles
            {
                // Create a copy so that we can remove blocks
                Map<BlockPosition, TileEntity> tiles = new HashMap<>(nmsChunk.getTileEntities());
                if (!tiles.isEmpty()) {
                    for (Map.Entry<BlockPosition, TileEntity> entry : tiles.entrySet()) {
                        final BlockPosition pos = entry.getKey();
                        final int lx = pos.getX() & 15;
                        final int ly = pos.getY();
                        final int lz = pos.getZ() & 15;
                        final int layer = ly >> 4;
                        if (!set.hasSection(layer)) {
                            continue;
                        }

                        int ordinal = set.getBlock(lx, ly, lz).getOrdinal();
                        if (ordinal != 0) {
                            TileEntity tile = entry.getValue();
                            nmsChunk.removeTileEntity(tile.getPosition());
                        }
                    }
                }
            }

            int bitMask = 0;
            synchronized (nmsChunk) {
                ChunkSection[] sections = nmsChunk.getSections();

                for (int layer = 0; layer < 16; layer++) {
                    if (!set.hasSection(layer)) {
                        continue;
                    }

                    bitMask |= 1 << layer;

                    char[] setArr = set.load(layer);
                    ChunkSection newSection;
                    ChunkSection existingSection = sections[layer];
                    if (existingSection == null) {
                        newSection = BukkitAdapter1161.newChunkSection(layer, setArr, fastmode);
                        if (BukkitAdapter1161.setSectionAtomic(sections, null, newSection, layer)) {
                            this.updateGet(this, nmsChunk, sections, newSection, setArr, layer);
                            continue;
                        } else {
                            existingSection = sections[layer];
                            if (existingSection == null) {
                                log.error("Skipping invalid null section. chunk:" + chunkX + "," + chunkZ + " layer: " + layer);
                                continue;
                            }
                        }
                    }
                    BukkitAdapter1161.fieldTickingBlockCount.set(existingSection, (short) 0);

                    //ensure that the server doesn't try to tick the chunksection while we're editing it.
                    DelegateLock lock = BukkitAdapter1161.applyLock(existingSection);

                    synchronized (this) {
                        synchronized (lock) {
                            lock.untilFree();
                            if (this.cloudChunk != nmsChunk) {
                                this.cloudChunk = nmsChunk;
                                this.sections = null;
                                this.reset();
                            } else if (existingSection != this.getSections()[layer]) {
                                this.sections[layer] = existingSection;
                                this.reset();
                            } else if (!Arrays.equals(this.update(layer, new char[4096]), this.load(layer))) {
                                this.reset(layer);
                            } else if (lock.isModified()) {
                                this.reset(layer);
                            }
                            newSection = BukkitAdapter1161
                                    .newChunkSection(layer, this::load, setArr, fastmode);
                            if (!BukkitAdapter1161
                                    .setSectionAtomic(sections, existingSection, newSection, layer)) {
                                log.error("Failed to set chunk section:" + chunkX + "," + chunkZ + " layer: " + layer);
                            } else {
                                this.updateGet(this, nmsChunk, sections, newSection, setArr, layer);
                            }
                        }
                    }
                }

                // Biomes
                BiomeType[] biomes = set.getBiomes();
                if (biomes != null) {
                    // set biomes
                    BiomeStorage currentBiomes = nmsChunk.getBiomeIndex();
                    for (int z = 0, i = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++, i++) {
                            final BiomeType biome = biomes[i];
                            if (biome != null) {
                                final Biome craftBiome = BukkitAdapter.adapt(biome);
                                BiomeBase nmsBiome = CraftBlock.biomeToBiomeBase(craftBiome);
                                for (int y = 0; y < FaweCache.IMP.worldHeight; y++) {
                                    currentBiomes.setBiome(x >> 2, y >> 2, z >> 2, nmsBiome);
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
                        this.fillLightNibble(light, EnumSkyBlock.BLOCK);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }

                char[][] skyLight = set.getSkyLight();
                if (skyLight != null) {
                    lightUpdate = true;
                    try {
                        this.fillLightNibble(skyLight, EnumSkyBlock.SKY);
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
                        final List<Entity>[] entities = nmsChunk.getEntitySlices();

                        for (final Collection<Entity> ents : entities) {
                            if (!ents.isEmpty()) {
                                final Iterator<Entity> iter = ents.iterator();
                                while (iter.hasNext()) {
                                    final Entity entity = iter.next();
                                    if (entityRemoves.contains(entity.getUniqueID())) {
                                        iter.remove();
                                        this.removeEntity(entity);
                                    }
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
                        for (final CompoundTag nativeTag : entities) {
                            final Map<String, Tag> entityTagMap = nativeTag.getValue();
                            final StringTag idTag = (StringTag) entityTagMap.get("Id");
                            final ListTag posTag = (ListTag) entityTagMap.get("Pos");
                            final ListTag rotTag = (ListTag) entityTagMap.get("Rotation");
                            if (idTag == null || posTag == null || rotTag == null) {
                                getLogger(
                                        BukkitGetBlocks1161.class).debug("Unknown entity tag: " + nativeTag);
                                continue;
                            }
                            final double x = posTag.getDouble(0);
                            final double y = posTag.getDouble(1);
                            final double z = posTag.getDouble(2);
                            final float yaw = rotTag.getFloat(0);
                            final float pitch = rotTag.getFloat(1);
                            final String id = idTag.getValue();

                            EntityTypes<?> type = EntityTypes.a(id).orElse(null);
                            if (type != null) {
                                Entity entity = type.a(nmsWorld);
                                if (entity != null) {
                                    UUID uuid = entity.getUniqueID();
                                    entityTagMap.put("UUIDMost", new LongTag(uuid.getMostSignificantBits()));
                                    entityTagMap.put("UUIDLeast", new LongTag(uuid.getLeastSignificantBits()));
                                    if (nativeTag != null) {
                                        BukkitImplAdapter adapter = WorldEditPlugin.getInstance().getBukkitImplAdapter();
                                        final NBTTagCompound tag = (NBTTagCompound) adapter.fromNative(nativeTag);
                                        for (final String name : Constants.NO_COPY_ENTITY_NBT_FIELDS) {
                                            tag.remove(name);
                                        }
                                        entity.save(tag);
                                    }
                                    entity.setLocation(x, y, z, yaw, pitch);
                                    nmsWorld.addEntity(entity, CreatureSpawnEvent.SpawnReason.CUSTOM);
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
                            final BlockPosition pos = new BlockPosition(x, y, z);

                            synchronized (nmsWorld) {
                                TileEntity tileEntity = nmsWorld.getTileEntity(pos);
                                if (tileEntity == null || tileEntity.isRemoved()) {
                                    nmsWorld.removeTileEntity(pos);
                                    tileEntity = nmsWorld.getTileEntity(pos);
                                }
                                if (tileEntity != null) {
                                    BukkitImplAdapter adapter = WorldEditPlugin.getInstance().getBukkitImplAdapter();
                                    final NBTTagCompound tag = (NBTTagCompound) adapter.fromNative(nativeTag);
                                    tag.set("x", NBTTagInt.a(x));
                                    tag.set("y", NBTTagInt.a(y));
                                    tag.set("z", NBTTagInt.a(z));
                                    tileEntity.load(tileEntity.getBlock(), tag);
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
                        nmsChunk.d(true); // Set Modified
                        nmsChunk.mustNotSave = false;
                        nmsChunk.markDirty();
                        // send to player
                        BukkitAdapter1161.sendChunk(nmsWorld, chunkX, chunkZ, finalMask, finalLightUpdate);
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

    @Override
    public synchronized char[] update(int layer, char[] data) {
        ChunkSection section = this.getSections()[layer];
        // Section is null, return empty array
        if (section == null) {
            data = new char[4096];
            Arrays.fill(data, (char) 1);
            return data;
        }
        if (data == null || data == FaweCache.IMP.emptyChar4096) {
            data = new char[4096];
            Arrays.fill(data, (char) 1);
        }
        DelegateLock lock = BukkitAdapter1161.applyLock(section);
        synchronized (lock) {
            lock.untilFree();
            lock.setModified(false);
            // Efficiently convert ChunkSection to raw data
            try {
                FAWESpigotV116R1 adapter = ((FAWESpigotV116R1) WorldEditPlugin.getInstance().getBukkitImplAdapter());

                final DataPaletteBlock<IBlockData> blocks = section.getBlocks();
                final DataBits bits = (DataBits) BukkitAdapter1161.fieldBits.get(blocks);
                final DataPalette<IBlockData> palette = (DataPalette<IBlockData>) BukkitAdapter1161.fieldPalette.get(blocks);

                final int bitsPerEntry = (int) BukkitAdapter1161.fieldBitsPerEntry.get(bits);
                final long[] blockStates = bits.a();

                new BitArrayUnstretched(bitsPerEntry, blockStates).toRaw(data);

                int num_palette;
                if (palette instanceof DataPaletteLinear) {
                    num_palette = ((DataPaletteLinear<IBlockData>) palette).b();
                } else if (palette instanceof DataPaletteHash) {
                    num_palette = ((DataPaletteHash<IBlockData>) palette).b();
                } else {
                    num_palette = 0;
                    int[] paletteToBlockInts = FaweCache.IMP.paletteToBlock.get();
                    char[] paletteToBlockChars = FaweCache.IMP.paletteToBlockChar.get();
                    try {
                        for (int i = 0; i < 4096; i++) {
                            char paletteVal = data[i];
                            char ordinal = paletteToBlockChars[paletteVal];
                            if (ordinal == Character.MAX_VALUE) {
                                paletteToBlockInts[num_palette++] = paletteVal;
                                IBlockData ibd = palette.a(data[i]);
                                if (ibd == null) {
                                    ordinal = BlockTypes.AIR.getDefaultState().getOrdinalChar();
                                } else {
                                    ordinal = adapter.adaptToChar(ibd);
                                }
                                paletteToBlockChars[paletteVal] = ordinal;
                            }
                            // Don't read "empty".
                            if (ordinal == 0) {
                                ordinal = 1;
                            }
                            data[i] = ordinal;
                        }
                    } finally {
                        for (int i = 0; i < num_palette; i++) {
                            int paletteVal = paletteToBlockInts[i];
                            paletteToBlockChars[paletteVal] = Character.MAX_VALUE;
                        }
                    }
                    return data;
                }

                char[] paletteToOrdinal = FaweCache.IMP.paletteToBlockChar.get();
                try {
                    if (num_palette != 1) {
                        for (int i = 0; i < num_palette; i++) {
                            char ordinal = this.ordinal(palette.a(i), adapter);
                            paletteToOrdinal[i] = ordinal;
                        }
                        for (int i = 0; i < 4096; i++) {
                            char paletteVal = data[i];
                            char val = paletteToOrdinal[paletteVal];
                            if (val == Character.MAX_VALUE) {
                                val = this.ordinal(palette.a(i), adapter);
                                paletteToOrdinal[i] = val;
                            }
                            // Don't read "empty".
                            if (val == 0) {
                                val = 1;
                            }
                            data[i] = val;
                        }
                    } else {
                        char ordinal = this.ordinal(palette.a(0), adapter);
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
        }
    }

    private final char ordinal(IBlockData ibd, FAWESpigotV116R1 adapter) {
        if (ibd == null) {
            return BlockTypes.AIR.getDefaultState().getOrdinalChar();
        } else {
            return adapter.adaptToChar(ibd);
        }
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

    private void fillLightNibble(char[][] light, EnumSkyBlock skyBlock) {
        for (int Y = 0; Y < 16; Y++) {
            if (light[Y] == null) {
                continue;
            }
            SectionPosition sectionPosition = SectionPosition.a(cloudChunk.getPos(), Y);
            NibbleArray nibble = world.getChunkProvider().getLightEngine().a(skyBlock).a(sectionPosition);
            if (nibble == null) {
                byte[] a = new byte[2048];
                Arrays.fill(a, skyBlock == EnumSkyBlock.SKY ? (byte) 15 : (byte) 0);
                nibble = new NibbleArray(a);
                ((LightEngine) world.getChunkProvider().getLightEngine()).a(skyBlock, sectionPosition, nibble, true);
            }
            synchronized (nibble) {
                for (int i = 0; i < 4096; i++) {
                    if (light[Y][i] < 16) {
                        nibble.a(i, light[Y][i]);
                    }
                }
            }
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
                    final DataPaletteBlock<IBlockData> blocksExisting = existing.getBlocks();

                    final DataPalette<IBlockData> palette = (DataPalette<IBlockData>) BukkitAdapter1161.fieldPalette.get(blocksExisting);
                    int paletteSize;

                    if (palette instanceof DataPaletteLinear) {
                        paletteSize = ((DataPaletteLinear<IBlockData>) palette).b();
                    } else if (palette instanceof DataPaletteHash) {
                        paletteSize = ((DataPaletteHash<IBlockData>) palette).b();
                    } else {
                        super.trim(false, i);
                        continue;
                    }
                    if (paletteSize == 1) {
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