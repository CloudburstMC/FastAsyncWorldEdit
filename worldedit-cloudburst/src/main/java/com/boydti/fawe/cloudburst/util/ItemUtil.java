package com.boydti.fawe.cloudburst.util;

import com.nukkitx.nbt.NbtMap;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.cloudburst.CloudburstAdapter;
import org.cloudburstmc.server.item.Item;

public class ItemUtil {

    public CompoundTag getNBT(Item item) {
        if (!item.hasNbtMap()) {
            return null;
        }

        CloudburstAdapter.adapt(item.getTag());

        return null;
    }

    public Item setNBT(Item item, CompoundTag tag) {
        item.setTag((NbtMap) CloudburstAdapter.adapt(tag));
        return null;
    }
}
