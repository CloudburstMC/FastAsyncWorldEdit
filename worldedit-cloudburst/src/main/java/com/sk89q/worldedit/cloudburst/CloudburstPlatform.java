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

import com.google.common.collect.Sets;
import com.sk89q.cloudburst.util.CommandInfo;
import com.sk89q.cloudburst.util.CommandRegistration;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.util.PermissionCondition;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.*;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.concurrency.LazyReference;
import com.sk89q.worldedit.world.DataFixer;
import com.sk89q.worldedit.world.registry.Registries;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.registry.EntityRegistry;
import org.cloudburstmc.server.utils.Identifier;
import org.enginehub.piston.CommandManager;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.sk89q.worldedit.util.formatting.WorldEditText.reduceToText;

public class CloudburstPlatform extends AbstractPlatform implements MultiUserPlatform {


    public final Server server;
    public final WorldEditPlugin plugin;
    private final CommandRegistration dynamicCommands;
    private final LazyReference<Watchdog> watchdog;
    private boolean hookingEvents;

    public CloudburstPlatform(WorldEditPlugin plugin, Server server) {
        this.plugin = plugin;
        this.server = server;
        this.dynamicCommands = new CommandRegistration(plugin);
        this.watchdog = LazyReference.from(() -> null);
    }

    CommandRegistration getDynamicCommands() {
        return dynamicCommands;
    }

    boolean isHookingEvents() {
        return hookingEvents;
    }

    @Override
    public Registries getRegistries() {
        return CloudburstRegistries.getInstance();
    }

    @Override
    public int getDataVersion() {
        return -1;
    }

    @Override
    public DataFixer getDataFixer() {
        return null;
    }

    @Override
    public boolean isValidMobType(String type) {
        if (!type.startsWith("minecraft:")) {
            return false;
        }

        final EntityType<?> entityType = EntityRegistry.get().getEntityType(Identifier.fromString(type));
        return entityType != null;
    }

    @Override
    public void reload() {
        plugin.loadConfiguration();
    }

    @Override
    public int schedule(long delay, long period, Runnable task) {
        return server.getScheduler().scheduleDelayedRepeatingTask(plugin, task, (int) delay, (int) period).getTaskId();
    }

    @Override
    public Watchdog getWatchdog() {
        return watchdog.getValue();
    }

    @Override
    public List<com.sk89q.worldedit.world.World> getWorlds() {
        Set<Level> worlds = server.getLevels();
        List<com.sk89q.worldedit.world.World> ret = new ArrayList<>(worlds.size());

        for (Level level : worlds) {
            ret.add(CloudburstAdapter.adapt(level));
        }

        return ret;
    }

    @Nullable
    @Override
    public Player matchPlayer(Player player) {
        if (player instanceof CloudburstPlayer) {
            return player;
        } else {
            org.cloudburstmc.server.player.Player bukkitPlayer = server.getPlayerExact(player.getName());
            return bukkitPlayer != null ? WorldEditPlugin.getInstance().wrapPlayer(bukkitPlayer) : null;
        }
    }

    @Nullable
    @Override
    public CloudburstWorld matchWorld(com.sk89q.worldedit.world.World world) {
        if (world instanceof CloudburstWorld) {
            return (CloudburstWorld) world;
        } else {
            Level level = server.getLevelByName(world.getName());
            return level != null ? new CloudburstWorld(level) : null;
        }
    }

    @Override
    public void registerCommands(CommandManager dispatcher) {
        CloudburstCommandInspector inspector = new CloudburstCommandInspector(plugin, dispatcher);

        dynamicCommands.register(dispatcher.getAllCommands()
            .map(command -> {
                String[] permissionsArray = command.getCondition()
                    .as(PermissionCondition.class)
                    .map(PermissionCondition::getPermissions)
                    .map(s -> s.toArray(new String[0]))
                    .orElseGet(() -> new String[0]);

                String[] aliases = Stream.concat(
                    Stream.of(command.getName()),
                    command.getAliases().stream()
                ).toArray(String[]::new);
                // TODO Handle localisation correctly
                return new CommandInfo(reduceToText(command.getUsage(), WorldEdit.getInstance().getConfiguration().defaultLocale),
                    reduceToText(command.getDescription(), WorldEdit.getInstance().getConfiguration().defaultLocale), aliases,
                    inspector, permissionsArray);
            }).collect(Collectors.toList()));
    }

    @Override
    public void registerGameHooks() {
        hookingEvents = true;
    }

    @Override
    public LocalConfiguration getConfiguration() {
        return plugin.getLocalConfiguration();
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String getPlatformName() {
        return "Cloudburst-Official";
    }

    @Override
    public String getPlatformVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public Map<Capability, Preference> getCapabilities() {
        Map<Capability, Preference> capabilities = new EnumMap<>(Capability.class);
        capabilities.put(Capability.CONFIGURATION, Preference.NORMAL);
        capabilities.put(Capability.WORLDEDIT_CUI, Preference.PREFER_OTHERS);
        capabilities.put(Capability.GAME_HOOKS, Preference.PREFERRED);
        capabilities.put(Capability.PERMISSIONS, Preference.PREFERRED);
        capabilities.put(Capability.USER_COMMANDS, Preference.PREFERRED);
        capabilities.put(Capability.WORLD_EDITING, Preference.PREFER_OTHERS);
        return capabilities;
    }

    private static final Set<SideEffect> SUPPORTED_SIDE_EFFECTS = Sets.immutableEnumSet(
            SideEffect.NEIGHBORS
    );

    @Override
    public Set<SideEffect> getSupportedSideEffects() {
        return SUPPORTED_SIDE_EFFECTS;
    }

    public void unregisterCommands() {
        dynamicCommands.unregisterCommands();
    }

    @Override
    public Collection<Actor> getConnectedUsers() {
        List<Actor> users = new ArrayList<>();
        for (org.cloudburstmc.server.player.Player player : server.getOnlinePlayers().values()) {
            users.add(WorldEditPlugin.getInstance().wrapPlayer(player));
        }
        return users;
    }
}
