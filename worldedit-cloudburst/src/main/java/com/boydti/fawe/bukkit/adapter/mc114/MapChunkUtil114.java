package com.boydti.fawe.bukkit.adapter.mc114;

import com.boydti.fawe.bukkit.adapter.MapChunkUtil;
import net.minecraft.server.v1_14_R1.PacketPlayOutMapChunk;

public class MapChunkUtil114 extends MapChunkUtil<PacketPlayOutMapChunk> {
    public MapChunkUtil114() throws NoSuchFieldException {
        fieldX = PacketPlayOutMapChunk.class.getDeclaredField("a");
        fieldZ = PacketPlayOutMapChunk.class.getDeclaredField("b");
        fieldBitMask = PacketPlayOutMapChunk.class.getDeclaredField("c");
        fieldHeightMap = PacketPlayOutMapChunk.class.getDeclaredField("d");
        fieldChunkData = PacketPlayOutMapChunk.class.getDeclaredField("e");
        fieldBlockEntities = PacketPlayOutMapChunk.class.getDeclaredField("f");
        fieldFull = PacketPlayOutMapChunk.class.getDeclaredField("g");
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
