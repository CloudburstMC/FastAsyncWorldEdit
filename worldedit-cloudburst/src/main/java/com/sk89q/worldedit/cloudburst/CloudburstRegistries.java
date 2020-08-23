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

import com.sk89q.worldedit.world.registry.*;

/**
 * World data for the Bukkit platform.
 */
class CloudburstRegistries extends BundledRegistries {

    private static final CloudburstRegistries INSTANCE = new CloudburstRegistries();
    private final BlockRegistry blockRegistry = new CloudburstBlockRegistry();
    private final BiomeRegistry biomeRegistry = new CloudburstBiomeRegistry();
    private final ItemRegistry itemRegistry = new CloudburstItemRegistry();
    private final EntityRegistry entityRegistry = new CloudburstEntityRegistry();
//    private final BlockCategoryRegistry blockCategoryRegistry = new CloudburstBlockCategoryRegistry();
//    private final ItemCategoryRegistry itemCategoryRegistry = new CloudburstItemCategoryRegistry();

    /**
     * Create a new instance.
     */
    CloudburstRegistries() {
    }

    @Override
    public BlockRegistry getBlockRegistry() {
        return blockRegistry;
    }

    @Override
    public BiomeRegistry getBiomeRegistry() {
        return biomeRegistry;
    }

    @Override
    public ItemRegistry getItemRegistry() {
        return itemRegistry;
    }

//    @Override
//    public BlockCategoryRegistry getBlockCategoryRegistry() {
//        return blockCategoryRegistry;
//    }

//    @Override
//    public ItemCategoryRegistry getItemCategoryRegistry() {
//        return itemCategoryRegistry;
//    }

    @Override
    public EntityRegistry getEntityRegistry() {
        return entityRegistry;
    }

    /**
     * Get a static instance.
     *
     * @return an instance
     */
    public static CloudburstRegistries getInstance() {
        return INSTANCE;
    }

}
