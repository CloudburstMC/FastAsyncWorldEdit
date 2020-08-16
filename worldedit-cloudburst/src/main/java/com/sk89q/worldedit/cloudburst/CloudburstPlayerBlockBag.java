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

import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.extent.inventory.*;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.block.BlockState;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.player.Player;

public class CloudburstPlayerBlockBag extends BlockBag implements SlottableBlockBag {

    private final Player player;
    private Int2ObjectMap<Item> items;

    /**
     * Construct the object.
     *
     * @param player the player
     */
    public CloudburstPlayerBlockBag(Player player) {
        this.player = player;
    }

    /**
     * Loads inventory on first use.
     */
    private void loadInventory() {
        if (items == null) {
            items = new Int2ObjectOpenHashMap<>(player.getInventory().getContents());
        }
    }

    /**
     * Get the player.
     *
     * @return the player
     */
    public Player getPlayer() {
        return player;
    }

    @Override
    public void fetchBlock(BlockState blockState) throws BlockBagException {
        if (blockState.getBlockType().getMaterial().isAir()) {
            throw new IllegalArgumentException("Can't fetch air block");
        }

        loadInventory();

        boolean found = false;

        for (int slot = 0; slot < items.size(); ++slot) {
            Item item = items.get(slot);

            if (item.isNull()) {
                continue;
            }

            if (!CloudburstAdapter.equals(blockState.getBlockType(), item.getId())) {
                // Type id doesn't fit
                continue;
            }

            int currentAmount = item.getCount();
            if (currentAmount < 0) {
                // Unlimited
                return;
            }

            if (currentAmount > 1) {
                item.decrementCount();
                found = true;
            } else {
                items.put(slot, Item.get(BlockTypes.AIR));
                found = true;
            }

            break;
        }

        if (!found) {
            throw new OutOfBlocksException();
        }
    }

    @Override
    public void storeBlock(BlockState blockState, int amount) throws BlockBagException {
        if (blockState.getBlockType().getMaterial().isAir()) {
            throw new IllegalArgumentException("Can't store air block");
        }
        if (!blockState.getBlockType().hasItemType()) {
            throw new IllegalArgumentException("This block cannot be stored");
        }

        loadInventory();

        int freeSlot = -1;

        for (int slot = 0; slot < items.size(); ++slot) {
            Item bukkitItem = items.get(slot);

            if (bukkitItem.isNull()) {
                // Delay using up a free slot until we know there are no stacks
                // of this item to merge into

                if (freeSlot == -1) {
                    freeSlot = slot;
                }
                continue;
            }

            if (!CloudburstAdapter.equals(blockState.getBlockType(), bukkitItem.getId())) {
                // Type id doesn't fit
                continue;
            }

            int currentAmount = bukkitItem.getCount();
            if (currentAmount < 0) {
                // Unlimited
                return;
            }
            if (currentAmount >= 64) {
                // Full stack
                continue;
            }

            int spaceLeft = 64 - currentAmount;
            if (spaceLeft >= amount) {
                bukkitItem.setCount(currentAmount + amount);
                return;
            }

            bukkitItem.setCount(64);
            amount -= spaceLeft;
        }

        if (freeSlot > -1) {
            items.put(freeSlot, CloudburstAdapter.adapt(new BaseItemStack(blockState.getBlockType().getItemType(), amount)));
            return;
        }

        throw new OutOfSpaceException(blockState.getBlockType());
    }

    @Override
    public void flushChanges() {
        if (items != null) {
            player.getInventory().setContents(items);
            items = null;
        }
    }

    @Override
    public void addSourcePosition(Location pos) {
    }

    @Override
    public void addSingleSourcePosition(Location pos) {
    }

    @Override
    public BaseItem getItem(int slot) {
        loadInventory();
        return CloudburstAdapter.adapt(items.get(slot));
    }

    @Override
    public void setItem(int slot, BaseItem block) {
        loadInventory();
        BaseItemStack stack = block instanceof BaseItemStack ? (BaseItemStack) block : new BaseItemStack(block.getType(), block.getNbtData(), 1);
        items.put(slot, CloudburstAdapter.adapt(stack));
    }

}
