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

import com.google.common.collect.ImmutableList;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import com.sk89q.worldedit.world.registry.BundledBlockRegistry;
import com.sk89q.worldedit.world.registry.PassthroughBlockMaterial;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.utils.Identifier;

import java.util.*;

public class CloudburstBlockRegistry extends BundledBlockRegistry {

    private final Map<org.cloudburstmc.server.block.BlockState, CloudburstBlockMaterial> materialMap = new HashMap<>();

    static final Set<String> BLOCKS = new HashSet<>();

    static {
        ImmutableList<org.cloudburstmc.server.block.BlockState> states = BlockRegistry.get().getBlockStates();
        for (org.cloudburstmc.server.block.BlockState state : states) {
            BLOCKS.add(state.getType().toString().toLowerCase(Locale.US));
        }
    }

    @Override
    public BlockMaterial getMaterial(BlockType blockType) {
        org.cloudburstmc.server.block.BlockState block = BlockRegistry.get().getBlock(Identifier.fromString(blockType.getId()));
        if (block == null) {
            WorldEditPlugin.getInstance().getLogger().warn("Unable to find block-type for {}", blockType);
            return new PassthroughBlockMaterial(null);
        }
        return materialMap.computeIfAbsent(block, material -> new CloudburstBlockMaterial(CloudburstBlockRegistry.super.getMaterial(blockType), block));
    }

    //TODO
    @Override
    public Map<String, ? extends Property<?>> getProperties(BlockType blockType) {
        return super.getProperties(blockType);
    }

    @Override
    public OptionalInt getInternalBlockStateId(BlockState state) {
        return OptionalInt.of(BlockRegistry.get().getRuntimeId(CloudburstAdapter.adapt(state)));
    }

    @Override
    public Collection<String> values() {
        return BLOCKS;
    }

    public static class CloudburstBlockMaterial extends PassthroughBlockMaterial {

        private final org.cloudburstmc.server.block.BlockState block;

        public CloudburstBlockMaterial(BlockMaterial material, org.cloudburstmc.server.block.BlockState cloudburstBlock) {
            super(material);
            this.block = cloudburstBlock;
        }

        @Override
        public boolean isAir() {
            return block == BlockStates.AIR;
        }

        @Override
        public boolean isSolid() {
            return block.getBehavior().isSolid();
        }

        @Override
        public boolean isBurnable() {
            return block.getBehavior().getBurnAbility() > 0;
        }
    }
}
