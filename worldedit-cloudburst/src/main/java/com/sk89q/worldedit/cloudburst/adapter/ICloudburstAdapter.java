package com.sk89q.worldedit.cloudburst.adapter;

import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.cloudburst.*;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldedit.world.entity.EntityTypes;
import com.sk89q.worldedit.world.gamemode.GameMode;
import com.sk89q.worldedit.world.gamemode.GameModes;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.item.ItemTypes;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.biome.Biome;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BiomeRegistry;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.registry.EntityRegistry;
import org.cloudburstmc.server.utils.Identifier;

import static com.google.common.base.Preconditions.checkNotNull;

public interface ICloudburstAdapter {

    /**
     * Convert any WorldEdit world into an equivalent wrapped Bukkit world.
     *
     * <p>If a matching world cannot be found, a {@link RuntimeException}
     * will be thrown.</p>
     *
     * @param world the world
     * @return a wrapped Bukkit world
     */
    default CloudburstWorld asBukkitWorld(World world) {
        if (world instanceof CloudburstWorld) {
            return (CloudburstWorld) world;
        } else {
            CloudburstWorld cloudburstWorld = WorldEditPlugin.getInstance().getInternalPlatform().matchWorld(world);
            if (cloudburstWorld == null) {
                throw new RuntimeException("World '" + world.getName() + "' has no matching version in Bukkit");
            }
            return cloudburstWorld;
        }
    }

    /**
     * Create a Bukkit world from a WorldEdit world.
     *
     * @param world the WorldEdit world
     * @return a Bukkit world
     */
    default Level adapt(World world) {
        checkNotNull(world);
        if (world instanceof CloudburstWorld) {
            return ((CloudburstWorld) world).getWorld();
        } else {
            Level match = Server.getInstance().getLevelByName(world.getName());
            if (match != null) {
                return match;
            } else {
                throw new IllegalArgumentException("Can't find a Bukkit world for " + world);
            }
        }
    }

    /**
     * Create a Bukkit location from a WorldEdit position with a Bukkit world.
     *
     * @param level    the Bukkit world
     * @param position the WorldEdit position
     * @return a Bukkit location
     */
    default org.cloudburstmc.server.level.Location adapt(Level level, Vector3 position) {
        checkNotNull(level);
        checkNotNull(position);
        return org.cloudburstmc.server.level.Location.from(
                (float) position.getX(),
                (float) position.getY(),
                (float) position.getZ(),
                level
        );
    }

    default org.cloudburstmc.server.level.Location adapt(Level world, BlockVector3 position) {
        return adapt(world, position.toVector3());
    }

    /**
     * Create a Bukkit location from a WorldEdit location with a Bukkit world.
     *
     * @param level    the Bukkit world
     * @param location the WorldEdit location
     * @return a Bukkit location
     */
    default org.cloudburstmc.server.level.Location adapt(Level level, Location location) {
        checkNotNull(level);
        checkNotNull(location);
        return org.cloudburstmc.server.level.Location.from(
                (float) location.getX(),
                (float) location.getY(),
                (float) location.getZ(),
                location.getYaw(),
                location.getPitch(),
                level
        );
    }

    /**
     * Create a WorldEdit Vector from a Bukkit location.
     *
     * @param location The Bukkit location
     * @return a WorldEdit vector
     */
    default Vector3 asVector(org.cloudburstmc.server.level.Location location) {
        checkNotNull(location);
        return Vector3.at(location.getX(), location.getY(), location.getZ());
    }

    /**
     * Create a WorldEdit BlockVector from a Bukkit location.
     *
     * @param location The Bukkit location
     * @return a WorldEdit vector
     */
    default BlockVector3 asBlockVector(org.cloudburstmc.server.level.Location location) {
        checkNotNull(location);
        return BlockVector3.at(location.getX(), location.getY(), location.getZ());
    }

    /**
     * Create a WorldEdit entity from a Bukkit entity.
     *
     * @param entity the Bukkit entity
     * @return a WorldEdit entity
     */
    default Entity adapt(org.cloudburstmc.server.entity.Entity entity) {
        checkNotNull(entity);
        return new CloudburstEntity(entity);
    }

    /**
     * Create a Bukkit Material form a WorldEdit ItemType.
     *
     * @param itemType The WorldEdit ItemType
     * @return The Bukkit Material
     */
    default Identifier adapt(ItemType itemType) {
        checkNotNull(itemType);
        if (!itemType.getId().startsWith("minecraft:")) {
            throw new IllegalArgumentException("Cloudburst only supports Minecraft items");
        }

        return Identifier.fromString(itemType.getId());
    }

    /**
     * Create a Bukkit Material form a WorldEdit BlockType.
     *
     * @param blockType The WorldEdit BlockType
     * @return The Bukkit Material
     */
    default Identifier adapt(BlockType blockType) {
        checkNotNull(blockType);
        if (!blockType.getId().startsWith("minecraft:")) {
            throw new IllegalArgumentException("Cloudburst only supports Minecraft blocks");
        }
        return Identifier.fromString(blockType.getId());
    }

    default org.cloudburstmc.server.entity.EntityType<?> adapt(EntityType entityType) {
        if (!entityType.getId().startsWith("minecraft:")) {
            throw new IllegalArgumentException("Cloudburst only supports vanilla entities");
        }
        return EntityRegistry.get().getEntityType(Identifier.fromString(entityType.getId()));
    }

    /**
     * Converts a Material to a BlockType.
     *
     * @param type The material
     * @return The blocktype
     */
    default BlockType asBlockType(Identifier type) {
        checkNotNull(type);
        org.cloudburstmc.server.block.BlockState state = BlockRegistry.get().getBlock(type);

        if (state == null) {
            throw new IllegalArgumentException(type.getName().toString() + " is not a block!") {
                @Override
                public synchronized Throwable fillInStackTrace() {
                    return this;
                }
            };
        }

        return BlockTypes.get(type.getName());
    }


    /**
     * Converts a Material to a ItemType.
     *
     * @param material The material
     * @return The itemtype
     */
    default ItemType asItemType(Identifier material) {
        return ItemTypes.get(material.getName());
    }

    /**
     * Create a WorldEdit BaseItemStack from a Bukkit ItemStack.
     *
     * @param itemStack The Bukkit ItemStack
     * @return The WorldEdit BaseItemStack
     */
    default BaseItemStack adapt(Item itemStack) {
        checkNotNull(itemStack);
        return new CloudburstItemStack(itemStack);
    }

    /**
     * Create a Bukkit ItemStack from a WorldEdit BaseItemStack.
     *
     * @param item The WorldEdit BaseItemStack
     * @return The Bukkit ItemStack
     */
    default Item adapt(BaseItemStack item) {
        checkNotNull(item);
        if (item instanceof CloudburstItemStack) {
            return ((CloudburstItemStack) item).getCloudburstItem();
        }
        return Item.get(adapt(item.getType()), item.getAmount());
    }

    /**
     * Create a WorldEdit Player from a Bukkit Player.
     *
     * @param player The Bukkit player
     * @return The WorldEdit player
     */
    default CloudburstPlayer adapt(Player player) {
        return WorldEditPlugin.getInstance().wrapPlayer(player);
    }

    /**
     * Create a Bukkit Player from a WorldEdit Player.
     *
     * @param player The WorldEdit player
     * @return The Bukkit player
     */
    default Player adapt(com.sk89q.worldedit.entity.Player player) {
        return ((CloudburstPlayer) player).getPlayer();
    }

    default Biome adapt(BiomeType biomeType) {
        if (!biomeType.getId().startsWith("minecraft:")) {
            throw new IllegalArgumentException("Bukkit only supports vanilla biomes");
        }
        try {
            return BiomeRegistry.get().getBiome(Identifier.fromString(biomeType.getId()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    default BiomeType adapt(Biome biome) {
        return BiomeTypes.get(biome.getId().getName());
    }

    /**
     * Checks equality between a WorldEdit BlockType and a Bukkit Material.
     *
     * @param blockType The WorldEdit BlockType
     * @param type      The Bukkit Material
     * @return If they are equal
     */
    default boolean equals(BlockType blockType, Identifier type) {
        return blockType == asItemType(type).getBlockType();
    }

    /**
     * Create a WorldEdit world from a Bukkit world.
     *
     * @param world the Bukkit world
     * @return a WorldEdit world
     */
    default World adapt(Level world) {
        checkNotNull(world);
        return new CloudburstWorld(world);
    }

    /**
     * Create a WorldEdit GameMode from a Bukkit one.
     *
     * @param gameMode Bukkit GameMode
     * @return WorldEdit GameMode
     */
    default GameMode adapt(org.cloudburstmc.server.player.GameMode gameMode) {
        checkNotNull(gameMode);
        return GameModes.get(gameMode.getName());
    }

    /**
     * Create a WorldEdit EntityType from a Bukkit one.
     *
     * @param entityType Bukkit EntityType
     * @return WorldEdit EntityType
     */
    default EntityType adapt(org.cloudburstmc.server.entity.EntityType<?> entityType) {
        return EntityTypes.get(entityType.getIdentifier().getName());
    }
}
