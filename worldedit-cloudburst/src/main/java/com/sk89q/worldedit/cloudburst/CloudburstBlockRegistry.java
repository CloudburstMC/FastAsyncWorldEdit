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

import com.sk89q.worldedit.cloudburst.adapter.CloudburstImplAdapter;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import com.sk89q.worldedit.world.registry.BundledBlockRegistry;
import com.sk89q.worldedit.world.registry.PassthroughBlockMaterial;
import org.cloudburstmc.server.block.BlockCategories;
import org.cloudburstmc.server.block.BlockCategory;
import org.cloudburstmc.server.utils.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.OptionalInt;

import static org.cloudburstmc.server.block.BlockTypes.AIR;

public class CloudburstBlockRegistry extends BundledBlockRegistry {

    private CloudburstBlockMaterial[] materialMap;

    @Override
    public Component getRichName(BlockType blockType) {
        if (WorldEditPlugin.getInstance().getAdapter() != null) {
            return WorldEditPlugin.getInstance().getAdapter().getRichBlockName(blockType);
        }
        return super.getRichName(blockType);
    }

    @Nullable
    @Override
    public BlockMaterial getMaterial(BlockType blockType) {
        CloudburstImplAdapter adapter = WorldEditPlugin.getInstance().getAdapter();
        if (adapter != null) {
            BlockMaterial result = adapter.getMaterial(blockType);
            if (result != null) {
                return result;
            }
        }
        Identifier mat = CloudburstAdapter.adapt(blockType);
        if (mat == null) {
            return new PassthroughBlockMaterial(null);
        }
        if (materialMap == null) {
            materialMap = new CloudburstBlockMaterial[Material.values().length];
        }
        CloudburstBlockMaterial result = materialMap[mat.ordinal()];
        if (result == null) {
            result = new CloudburstBlockMaterial(CloudburstBlockRegistry.super.getMaterial(blockType), mat);
            materialMap[mat.ordinal()] = result;
        }
        return result;
    }

    @Nullable
    @Override
    public BlockMaterial getMaterial(BlockState state) {
        CloudburstImplAdapter adapter = WorldEditPlugin.getInstance().getAdapter();
        if (adapter != null) {
            BlockMaterial result = adapter.getMaterial(state);
            if (result != null) {
                return result;
            }
        }
        return super.getMaterial(state);
    }

    @Override
    public OptionalInt getInternalBlockStateId(BlockState state) {
        if (WorldEditPlugin.getInstance().getAdapter() != null) {
            return WorldEditPlugin.getInstance().getAdapter().getInternalBlockStateId(state);
        }
        return OptionalInt.empty();
    }

    @Nullable
    @Override
    public Map<String, ? extends Property<?>> getProperties(BlockType blockType) {
        CloudburstImplAdapter adapter = WorldEditPlugin.getInstance().getAdapter();
        if (adapter != null) {
            return adapter.getProperties(blockType);
        }
        return super.getProperties(blockType);
    }

    public static class CloudburstBlockMaterial extends PassthroughBlockMaterial {

        private final Identifier material;

        public CloudburstBlockMaterial(@Nullable BlockMaterial material, Identifier bukkitMaterial) {
            super(material);
            this.material = bukkitMaterial;
        }

        @Override
        public boolean isAir() {
            return material == AIR;
        }

        @Override
        public boolean isSolid() {
            return BlockCategories.inCategory(material, BlockCategory.SOLID);
        }

        @Override
        public boolean isBurnable() {
            return material.isBurnable();
        }

        @Override
        public boolean isTranslucent() {
            return BlockCategories.inCategory(material, BlockCategory.TRANSPARENT);
        }
    }

    @Override
    public Collection<String> values() {
        ArrayList<String> blocks = new ArrayList<>();
        for (Material m : Material.values()) {
            if (!m.isLegacy() && m.isBlock()) {
                BlockData blockData = m.createBlockData();
                blocks.add(blockData.getAsString());
            }
        }
        return blocks;
    }
}
