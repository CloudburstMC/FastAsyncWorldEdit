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

import com.sk89q.minecraft.util.commands.CommandsManager;
import com.sk89q.wepif.PermissionsResolverManager;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandExecutor;
import org.cloudburstmc.server.command.CommandSender;
import org.cloudburstmc.server.command.PluginCommand;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.player.OfflinePlayer;
import org.cloudburstmc.server.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

/**
 * An implementation of a dynamically registered {@link Command} attached to a plugin.
 */
@SuppressWarnings("deprecation")
public class DynamicPluginCommand extends PluginCommand<Plugin> {

    protected final CommandExecutor owner;
    protected final Object registeredWith;
    protected final Plugin owningPlugin;

    public DynamicPluginCommand(String[] aliases, String[] permissions, String desc, String usage, CommandExecutor owner, Object registeredWith, Plugin plugin) {
        super(plugin, CommandData.builder(aliases[0])
                .setDescription(desc)
                .setUsageMessage(usage)
                .setAliases(Arrays.copyOfRange(aliases, 1, aliases.length))
                .setPermissions(permissions)
                .build());
        this.owner = owner;
        this.owningPlugin = plugin;
        this.registeredWith = registeredWith;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        return owner.onCommand(sender, this, label, args);
    }

    public Object getOwner() {
        return owner;
    }

    public Object getRegisteredWith() {
        return registeredWith;
    }

    @Override
    public Plugin getPlugin() {
        return owningPlugin;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean testPermissionSilent(CommandSender sender) {
        List<String> permissions = getPermissions();
        if (permissions == null || permissions.size() == 0) {
            return true;
        }

        if (registeredWith instanceof CommandInspector) {
            CommandInspector resolver = (CommandInspector) registeredWith;
            return resolver.testPermission(sender, this);
        } else if (registeredWith instanceof CommandsManager<?>) {
            try {
                for (String permission : permissions) {
                    if (((CommandsManager<CommandSender>) registeredWith).hasPermission(sender, permission)) {
                        return true;
                    }
                }
                return false;
            } catch (Throwable ignored) {
            }
        } else if (PermissionsResolverManager.isInitialized() && sender instanceof OfflinePlayer) {
            for (String permission : permissions) {
                if (PermissionsResolverManager.getInstance().hasPermission((OfflinePlayer) sender, permission)) {
                    return true;
                }
            }
            return false;
        }
        return super.testPermissionSilent(sender);
    }
}
