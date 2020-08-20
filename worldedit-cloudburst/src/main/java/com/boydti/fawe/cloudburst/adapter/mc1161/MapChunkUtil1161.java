package com.boydti.fawe.bukkit.adapter.mc1161;

import com.boydti.fawe.cloudburst.adapter.MapChunkUtil;
import net.minecraft.server.v1_16_R1.PacketPlayOutMapChunk;

public class MapChunkUtil1161 extends MapChunkUtil<PacketPlayOutMapChunk> {
    public MapChunkUtil1161() throws NoSuchFieldException {
        fieldX = PacketPlayOutMapChunk.class.getDeclaredField("a");
        fieldZ = PacketPlayOutMapChunk.class.getDeclaredField("b");
        fieldBitMask = PacketPlayOutMapChunk.class.getDeclaredField("c");
        fieldHeightMap = PacketPlayOutMapChunk.class.getDeclaredField("d");
        fieldChunkData = PacketPlayOutMapChunk.class.getDeclaredField("f");
        fieldBlockEntities = PacketPlayOutMapChunk.class.getDeclaredField("g");
        fieldFull = PacketPlayOutMapChunk.class.getDeclaredField("h");
        fieldX.setAccessible(true);
        fieldZ.setAccessible(true);
        fieldBitMask.setAccessible(true);
        fieldHeightMap.setAccessible(true);
        fieldChunkData.setAccessible(true);
        fieldBlockEntities.setAccessible(true);
        fieldFull.setAccessible(true);
    }

    @Override
    public PacketPlayOutMapChunk createPacket() {
        return new PacketPlayOutMapChunk();
    }
}
