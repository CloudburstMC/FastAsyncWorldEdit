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

import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.util.TaskManager;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.packet.UpdateBlockPacket;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.extension.platform.AbstractPlayerActor;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.internal.cui.CUIEvent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.session.SessionKey;
import com.sk89q.worldedit.util.HandSide;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.formatting.WorldEditText;
import com.sk89q.worldedit.util.formatting.component.TextUtils;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.gamemode.GameMode;
import com.sk89q.worldedit.world.gamemode.GameModes;
import org.cloudburstmc.server.AdventureSettings;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.event.player.PlayerDropItemEvent;
import org.cloudburstmc.server.inventory.PlayerInventory;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.utils.TextFormat;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CloudburstPlayer extends AbstractPlayerActor {

    private final Player player;
    private final WorldEditPlugin plugin;

    public CloudburstPlayer(Player player) {
        super(getExistingMap(WorldEditPlugin.getInstance(), player));
        this.plugin = WorldEditPlugin.getInstance();
        this.player = player;
    }

    public CloudburstPlayer(WorldEditPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        if (Settings.IMP.CLIPBOARD.USE_DISK) {
            loadClipboardFromDisk();
        }
    }

    private static Map<String, Object> getExistingMap(WorldEditPlugin plugin, Player player) {
        CloudburstPlayer cached = plugin.getCachedPlayer(player);
        if (cached != null) {
            return cached.getRawMeta();
        }
        return new ConcurrentHashMap<>();
    }

    @Override
    public UUID getUniqueId() {
        return player.getServerId();
    }

    @Override
    public BaseItemStack getItemInHand(HandSide handSide) {
        Item itemStack = handSide == HandSide.MAIN_HAND
                ? player.getInventory().getItemInHand()
                : player.getInventory().getOffHand();
        return CloudburstAdapter.adapt(itemStack);
    }

    @Override
    public BaseBlock getBlockInHand(HandSide handSide) throws WorldEditException {
        Item itemStack = handSide == HandSide.MAIN_HAND
                ? player.getInventory().getItemInHand()
                : player.getInventory().getOffHand();
        return CloudburstAdapter.asBlockState(itemStack).toBaseBlock();
    }

    @Override
    public String getName() {
        return player.getName();
    }

    @Override
    public String getDisplayName() {
        return player.getDisplayName();
    }

    @Override
    public void giveItem(BaseItemStack itemStack) {
        final PlayerInventory inv = player.getInventory();
        Item newItem = CloudburstAdapter.adapt(itemStack);
        if (itemStack.getType().getId().equalsIgnoreCase(WorldEdit.getInstance().getConfiguration().wandItem)) {
            inv.remove(newItem);
        }
        final Item item = player.getInventory().getItemInHand();
        player.getInventory().setItemInHand(newItem);
        Item[] overflow = inv.addItem(item);
        if (overflow.length != 0) {
            TaskManager.IMP.sync(new RunnableVal<Object>() {
                @Override
                public void run(Object value) {
                    for (Item stack : overflow) {
                        if (stack.getId() != org.cloudburstmc.server.block.BlockTypes.AIR && stack.getCount() > 0) {
                            PlayerDropItemEvent event = new PlayerDropItemEvent(player, stack);
                            Server.getInstance().getPluginManager().callEvent(event);
                            if (event.isCancelled()) continue;
                            player.getLevel().dropItem(player.getPosition(), stack);
                        }
                    }
                }
            });
        }
    }

    @Override
    @Deprecated
    public void printRaw(String msg) {
        for (String part : msg.split("\n")) {
            player.sendMessage(part);
        }
    }

    @Override
    @Deprecated
    public void print(String msg) {
        for (String part : msg.split("\n")) {
            player.sendMessage("§d" + part);
        }
    }

    @Override
    @Deprecated
    public void printDebug(String msg) {
        for (String part : msg.split("\n")) {
            player.sendMessage("§7" + part);
        }
    }

    @Override
    @Deprecated
    public void printError(String msg) {
        for (String part : msg.split("\n")) {
            player.sendMessage("§c" + part);
        }
    }

    @Override
    public void print(Component component) {
        player.sendMessage(TextFormat.colorize(WorldEditText.reduceToText(component, getLocale())));
    }

    @Override
    public boolean trySetPosition(Vector3 pos, float pitch, float yaw) {
        return TaskManager.IMP.sync(() ->
                player.teleport(CloudburstAdapter.adapt(new Location(CloudburstAdapter.adapt(player.getLevel()), pos, pitch, yaw))));
    }

    @Override
    public String[] getGroups() {
        return plugin.getPermissionsResolver().getGroups(player);
    }

    @Override
    public BlockBag getInventoryBlockBag() {
        return new CloudburstPlayerBlockBag(player);
    }

    @Override
    public GameMode getGameMode() {
        return GameModes.get(player.getGamemode().getName().toLowerCase(getLocale()));
    }

    @Override
    public void setGameMode(GameMode gameMode) {
        player.setGamemode(org.cloudburstmc.server.player.GameMode.from(gameMode.getId()));
    }

    @Override
    public boolean hasPermission(String perm) {
        return (!plugin.getLocalConfiguration().noOpPermissions && player.isOp())
                || plugin.getPermissionsResolver().hasPermission(
                player.getLevel().getName(), player, perm);
    }

    @Override
    public void setPermission(String permission, boolean value) {

        // TODO
        /*
         *  Permissions are used to managing WorldEdit region restrictions
         *   - The `/wea` command will give/remove the required bypass permission
         */
//        if (Fawe.<FaweCloudburst>imp().getVault() == null || Fawe.<FaweCloudburst>imp().getVault().permission == null) {
//            player.addAttachment(plugin).setPermission(permission, value);
//        } else if (value) {
//            if (!Fawe.<FaweCloudburst>imp().getVault().permission.playerAdd(player, permission)) {
//                player.addAttachment(plugin).setPermission(permission, value);
//            }
//        } else if (!Fawe.<FaweCloudburst>imp().getVault().permission.playerRemove(player, permission)) {
//            player.addAttachment(plugin).setPermission(permission, value);
//        }
    }

    @Override
    public World getWorld() {
        return CloudburstAdapter.adapt(player.getLevel());
    }

    @Override
    public void dispatchCUIEvent(CUIEvent event) {
//        String[] params = event.getParameters();
//        String send = event.getTypeId();
//        if (params.length > 0) {
//            send = send + "|" + StringUtil.joinString(params, "|");
//        }
//        player.sendPluginMessage(plugin, WorldEditPlugin.CUI_PLUGIN_CHANNEL, send.getBytes(CUIChannelListener.UTF_8_CHARSET));
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public boolean isAllowedToFly() {
        return player.getAllowFlight();
    }

    @Override
    public void setFlying(boolean flying) {
        player.getAdventureSettings().set(AdventureSettings.Type.FLYING, flying);
        player.getAdventureSettings().update();
    }

    @Override
    public BaseEntity getState() {
        throw new UnsupportedOperationException("Cannot create a state from this object");
    }

    @Override
    public com.sk89q.worldedit.util.Location getLocation() {
        return CloudburstAdapter.adapt(player.getLocation());
    }

    @Override
    public boolean setLocation(com.sk89q.worldedit.util.Location location) {
        return player.teleport(CloudburstAdapter.adapt(location));
    }

    @Override
    public Locale getLocale() {
        return TextUtils.getLocaleByMinecraftTag(player.getLoginChainData().getLanguageCode());
    }

    @Override
    public void sendAnnouncements() {
//        if (WorldEditPlugin.getInstance().getAdapter() == null) {
//            printError(TranslatableComponent.of("worldedit.version.bukkit.unsupported-adapter",
//                    TextComponent.of("https://intellectualsites.github.io/download/fawe.html", TextColor.AQUA)
//                        .clickEvent(ClickEvent.openUrl("https://intellectualsites.github.io/download/fawe.html"))));
//        }
    }

    @Nullable
    @Override
    public <T> T getFacet(Class<? extends T> cls) {
        return null;
    }

    @Override
    public SessionKey getSessionKey() {
        return new SessionKeyImpl(player.getServerId(), player.getName());
    }

    private static class SessionKeyImpl implements SessionKey {
        // If not static, this will leak a reference

        private final UUID uuid;
        private final String name;

        private SessionKeyImpl(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        @Override
        public UUID getUniqueId() {
            return uuid;
        }

        @Nullable
        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isActive() {
            // This is a thread safe call on CraftBukkit because it uses a
            // CopyOnWrite list for the list of players, but the Bukkit
            // specification doesn't require thread safety (though the
            // spec is extremely incomplete)
            return Server.getInstance().getPlayer(uuid) != null;
        }

        @Override
        public boolean isPersistent() {
            return true;
        }

    }

    @Override
    public <B extends BlockStateHolder<B>> void sendFakeBlock(BlockVector3 pos, B block) {
        UpdateBlockPacket packet = new UpdateBlockPacket();
        packet.setBlockPosition(Vector3i.from(pos.getX(), pos.getY(), pos.getZ()));
        packet.setDataLayer(0);
        packet.setRuntimeId(BlockRegistry.get().getRuntimeId(CloudburstAdapter.adapt(block)));
        player.sendPacket(packet);
    }

    @Override
    public void sendTitle(Component title, Component sub) {
        String titleStr = WorldEditText.reduceToText(title, getLocale());
        String subStr = WorldEditText.reduceToText(sub, getLocale());
        player.sendTitle(titleStr, subStr, 0, 70, 20);
    }

    @Override
    public void unregister() {
        player.removeMetadata("WE", WorldEditPlugin.getInstance());
        super.unregister();
    }

}
