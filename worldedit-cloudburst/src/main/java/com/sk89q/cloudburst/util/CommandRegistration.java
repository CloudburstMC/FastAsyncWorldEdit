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

package com.sk89q.cloudburst.util;

import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandExecutor;
import org.cloudburstmc.server.command.PluginIdentifiableCommand;
import org.cloudburstmc.server.plugin.Plugin;
import org.cloudburstmc.server.registry.CommandRegistry;

import java.util.List;
import java.util.Map.Entry;

public class CommandRegistration {

//    static {
//        Bukkit.getServer().getHelpMap().registerHelpTopicFactory(DynamicPluginCommand.class,
//                new DynamicPluginCommandHelpTopic.Factory());
//    }

    protected final Plugin plugin;
    protected final CommandExecutor executor;

    public CommandRegistration(Plugin plugin) {
        this(plugin, plugin);
    }

    public CommandRegistration(Plugin plugin, CommandExecutor executor) {
        this.plugin = plugin;
        this.executor = executor;
    }

    public Plugin getCommandOwner(String label) {
        PluginIdentifiableCommand command = CommandRegistry.get().getPluginCommand(label);

        if (command != null) {
            return command.getPlugin();
        }

        return null;
    }

    public boolean register(List<CommandInfo> registered) {
        if (registered == null) {
            return false;
        }

        CommandRegistry registry = CommandRegistry.get();
        for (CommandInfo command : registered) {
            String[] perms = command.getPermissions();

            if (perms == null) {
                perms = new String[0];
            }

            DynamicPluginCommand cmd = new DynamicPluginCommand(command.getAliases(), perms,
                    command.getDesc(), "/" + command.getAliases()[0] + " " + command.getUsage(), executor, command.getRegisteredWith(), plugin);

            registry.register(plugin, cmd);
        }
        return true;
    }

    public boolean unregisterCommands() {
        CommandRegistry registry = CommandRegistry.get();

        for (Entry<String, Command> entry : registry.getRegisteredCommands().entrySet()) {
            Command cmd = entry.getValue();

            if (cmd instanceof PluginIdentifiableCommand && ((PluginIdentifiableCommand) cmd).getPlugin() == plugin) {
                registry.unregister(plugin, entry.getKey());
            }
        }
        return true;
    }

}
