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

// $Id$

package com.sk89q.worldedit.cloudburst;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.event.EventHandler;
import org.cloudburstmc.server.event.EventPriority;
import org.cloudburstmc.server.event.Listener;
import org.cloudburstmc.server.event.player.PlayerCommandPreprocessEvent;
import org.cloudburstmc.server.event.player.PlayerGameModeChangeEvent;
import org.cloudburstmc.server.event.player.PlayerInteractEvent;
import org.cloudburstmc.server.event.player.PlayerInteractEvent.Action;
import org.cloudburstmc.server.registry.CommandRegistry;
import org.enginehub.piston.CommandManager;
import org.enginehub.piston.inject.InjectedValueStore;
import org.enginehub.piston.inject.Key;
import org.enginehub.piston.inject.MapBackedValueStore;

import java.util.Optional;

/**
 * Handles all events thrown in relation to a Player.
 */
public class WorldEditListener implements Listener {

    private final WorldEditPlugin plugin;

    /**
     * Construct the object.
     *
     * @param plugin the plugin
     */
    public WorldEditListener(WorldEditPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGamemode(PlayerGameModeChangeEvent event) {
        if (!plugin.getInternalPlatform().isHookingEvents()) {
            return;
        }

        // this will automatically refresh their session, we don't have to do anything
        WorldEdit.getInstance().getSessionManager().get(plugin.wrapPlayer(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerCommandSend(PlayerCommandPreprocessEvent event) {
        InjectedValueStore store = MapBackedValueStore.create();
        store.injectValue(Key.of(Actor.class), context ->
                Optional.of(plugin.wrapCommandSender(event.getPlayer())));
        CommandManager commandManager = plugin.getWorldEdit().getPlatformManager().getPlatformCommandManager().getCommandManager();
        CommandRegistry.get().getCommand(event.getEventName());

        if (commandManager.getCommand(event.getMessage().substring(1))
                .filter(command -> !command.getCondition().satisfied(store))
                .isPresent()) {
            event.setMessage("/RANDOM_COMMAND_THAT_SHOULDNT_BE_REGISTERED");
        }
    }

    /**
     * Called when a player interacts.
     *
     * @param event Relevant event details
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!plugin.getInternalPlatform().isHookingEvents()) {
            return;
        }

        final Player player = plugin.wrapPlayer(event.getPlayer());
        final World world = player.getWorld();
        final WorldEdit we = plugin.getWorldEdit();
        final Direction direction = CloudburstAdapter.adapt(event.getFace());

        Action action = event.getAction();
        if (action == Action.LEFT_CLICK_BLOCK) {
            final Block clickedBlock = event.getBlock();
            final Location pos = new Location(world, clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ());

            if (we.handleBlockLeftClick(player, pos, direction)) {
                event.setCancelled(true);
            }

            if (we.handleArmSwing(player)) {
                event.setCancelled(true);
            }

        } else if (action == Action.LEFT_CLICK_AIR) {

            if (we.handleArmSwing(player)) {
                event.setCancelled(true);
            }

        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            final Block clickedBlock = event.getBlock();
            final Location pos = new Location(world, clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ());

            if (we.handleBlockRightClick(player, pos, direction)) {
                event.setCancelled(true);
            }

            if (we.handleRightClick(player)) {
                event.setCancelled(true);
            }
        } else if (action == Action.RIGHT_CLICK_AIR) {
            if (we.handleRightClick(player)) {
                event.setCancelled(true);
            }
        }
    }
}