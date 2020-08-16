package com.sk89q.worldedit.cloudburst;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.cloudburst.FaweCloudburst;
import com.boydti.fawe.cloudburst.util.ItemUtil;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.world.item.ItemType;
import org.cloudburstmc.server.item.Item;
import org.jetbrains.annotations.Nullable;

public class CloudburstItemStack extends BaseItemStack {

    private Item stack;
    private boolean loadedNBT;

    public CloudburstItemStack(Item stack) {
        super(CloudburstAdapter.asItemType(stack.getId()));
        this.stack = stack;
    }

    public CloudburstItemStack(ItemType type, Item stack) {
        super(type);
        this.stack = stack;
    }

    @Override
    public int getAmount() {
        return stack.getCount();
    }

    @Nullable
    @Override
    public Item getNativeItem() {
        return stack;
    }

    public Item getCloudburstItem() {
        return stack;
    }

    @Override
    public boolean hasNbtData() {
        if (!loadedNBT) {
            return stack.hasNbtMap();
        }
        return super.hasNbtData();
    }

    @Nullable
    @Override
    public CompoundTag getNbtData() {
        if (!loadedNBT) {
            loadedNBT = true;
            ItemUtil util = Fawe.<FaweCloudburst>imp().getItemUtil();
            if (util != null) {
                super.setNbtData(util.getNBT(stack));
            }
        }
        return super.getNbtData();
    }

    @Override
    public void setNbtData(@Nullable CompoundTag nbtData) {
        ItemUtil util = Fawe.<FaweCloudburst>imp().getItemUtil();
        if (util != null) {
            stack = util.setNBT(stack, nbtData);
        }
        super.setNbtData(nbtData);
    }
}
