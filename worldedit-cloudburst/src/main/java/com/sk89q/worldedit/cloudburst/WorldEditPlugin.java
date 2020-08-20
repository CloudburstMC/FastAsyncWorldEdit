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


import com.boydti.fawe.Fawe;
import com.boydti.fawe.cloudburst.FaweCloudburst;
import com.boydti.fawe.util.MainUtil;
import com.google.common.base.Joiner;
import com.sk89q.util.yaml.YAMLProcessor;
import com.sk89q.wepif.PermissionsResolverManager;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.event.platform.PlatformReadyEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.internal.anvil.ChunkDeleter;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldedit.world.gamemode.GameModes;
import com.sk89q.worldedit.world.weather.WeatherTypes;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandSender;
import org.cloudburstmc.server.event.EventHandler;
import org.cloudburstmc.server.event.EventPriority;
import org.cloudburstmc.server.event.Listener;
import org.cloudburstmc.server.event.level.LevelInitEvent;
import org.cloudburstmc.server.level.biome.Biome;
import org.cloudburstmc.server.metadata.MetadataValue;
import org.cloudburstmc.server.plugin.Plugin;
import org.cloudburstmc.server.plugin.PluginBase;
import org.cloudburstmc.server.registry.BiomeRegistry;
import org.cloudburstmc.server.registry.EntityRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sk89q.worldedit.internal.anvil.ChunkDeleter.DELCHUNKS_FILE_NAME;

/**
 * Plugin for Cloudburst.
 */
public class WorldEditPlugin extends PluginBase {

    private static final Logger log = LoggerFactory.getLogger(WorldEditPlugin.class);
    public static final String CUI_PLUGIN_CHANNEL = "worldedit:cui";
    private static WorldEditPlugin INSTANCE;
    ///The BSTATS_ID needs to be modified for FAWE to prevent contaminating WorldEdit stats
    private static final int BSTATS_PLUGIN_ID = 1403;

    private CloudburstServerInterface server;
    private CloudburstConfiguration config;

    private static Map<String, Plugin> lookupNames;

    public WorldEditPlugin() {
        //init();
    }

    private void init() {
        if (lookupNames != null) {
            lookupNames.putIfAbsent("FastAsyncWorldEdit".toLowerCase(Locale.ROOT), this);
            lookupNames.putIfAbsent("WorldEdit".toLowerCase(Locale.ROOT), this);
            lookupNames.putIfAbsent("FastAsyncWorldEdit", this);
            lookupNames.putIfAbsent("WorldEdit", this);
            rename();
        }
        setEnabled(true);
    }

    @Override
    public void onLoad() {
        if (INSTANCE != null) {
            return;
        }
        rename();
        INSTANCE = this;
        FaweCloudburst imp = new FaweCloudburst(this);

        //noinspection ResultOfMethodCallIgnored
        getDataFolder().mkdirs();

        WorldEdit worldEdit = WorldEdit.getInstance();

        // Setup platform
        server = new CloudburstServerInterface(this, getServer());
        worldEdit.getPlatformManager().register(server);

        Path delChunks = Paths.get(getDataFolder().getPath(), DELCHUNKS_FILE_NAME);
        if (Files.exists(delChunks)) {
            ChunkDeleter.runFromFile(delChunks, true);
        }

        fail(() -> PermissionsResolverManager.initialize(INSTANCE), "Failed to initialize permissions resolver");
    }

    /**
     * Called on plugin enable.
     */
    @Override
    public void onEnable() {
        if (INSTANCE != null) {
            return;
        }
        onLoad();

        PermissionsResolverManager.initialize(this); // Setup permission resolver

        // Register CUI
//        fail(() -> {
//            getServer().getMessenger().registerIncomingPluginChannel(this, CUI_PLUGIN_CHANNEL, new CUIChannelListener(this));
//            getServer().getMessenger().registerOutgoingPluginChannel(this, CUI_PLUGIN_CHANNEL);
//        }, "Failed to register CUI");

        // Now we can register events
        getServer().getPluginManager().registerEvents(new WorldEditListener(this), this);
        // register async tab complete, if available

        initializeRegistries(); // this creates the objects matching Bukkit's enums - but doesn't fill them with data yet
        if (Server.getInstance().getLevels().isEmpty()) {
            setupPreWorldData();
            // register this so we can load world-dependent data right as the first world is loading
            getServer().getPluginManager().registerEvents(new WorldInitListener(), this);
        } else {
            getLogger().warn("Server reload detected. This may cause various issues with WorldEdit and dependent plugins.");
            try {
                setupPreWorldData();
                // since worlds are loaded already, we can do this now
                setupWorldData();
            } catch (Throwable ignored) {
            }
        }

        // Enable metrics
//        new Metrics(this, BSTATS_PLUGIN_ID);
    }

    private void setupPreWorldData() {
        loadConfig();
        WorldEdit.getInstance().loadMappings();
    }

    private void setupWorldData() {
        WorldEdit.getInstance().getEventBus().post(new PlatformReadyEvent());
    }

    @SuppressWarnings({"deprecation", "unchecked"})
    private void initializeRegistries() {
        // Biome
        Int2ObjectMap<Biome> biomes;
        try {
            Field biomesField = BiomeRegistry.class.getDeclaredField("runtimeToBiomeMap");
            biomesField.setAccessible(true);
            biomes = (Int2ObjectMap<Biome>) biomesField.get(BiomeRegistry.get());
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
        for (Int2ObjectMap.Entry<Biome> entry : biomes.int2ObjectEntrySet()) {
            String type = entry.getValue().getId().toString();
            BiomeType biomeType = BiomeType.REGISTRY.register(type, new BiomeType(type));

            biomeType.setLegacyId(entry.getIntKey());
        }
        // Block & Item
        /*for (Material material : Material.values()) {
            if (material.isBlock() && !material.isLegacy()) {
                BlockType.REGISTRY.register(material.getKey().toString(), new BlockType(material.getKey().toString(), blockState -> {
                    // TODO Use something way less hacky than this.
                    ParserContext context = new ParserContext();
                    context.setPreferringWildcard(true);
                    context.setTryLegacy(false);
                    context.setRestricted(false);
                    try {
                        FuzzyBlockState state = (FuzzyBlockState) WorldEdit.getInstance().getBlockFactory().parseFromInput(
                                BukkitAdapter.adapt(blockState.getBlockType()).createBlockData().getAsString(), context
                        ).toImmutableState();
                        BlockState defaultState = blockState.getBlockType().getAllStates().get(0);
                        for (Map.Entry<Property<?>, Object> propertyObjectEntry : state.getStates().entrySet()) {
                            //noinspection unchecked
                            defaultState = defaultState.with((Property<Object>) propertyObjectEntry.getKey(), propertyObjectEntry.getValue());
                        }
                        return defaultState;
                    } catch (InputParseException e) {
                        getLogger().log(Level.WARNING, "Error loading block state for " + material.getKey(), e);
                        return blockState;
                    }
                }));
            }
            if (material.isItem() && !material.isLegacy()) {
                ItemType.REGISTRY.register(material.getKey().toString(), new ItemType(material.getKey().toString()));
            }
        }
        */
        // Entity
        for (org.cloudburstmc.server.entity.EntityType<?> entityType : EntityRegistry.get().getEntityTypes()) {
            String mcid = entityType.getIdentifier().toString();
            if (mcid != null) {
                EntityType.REGISTRY.register(mcid, new EntityType(mcid));
            }
        }
        // ... :|
        GameModes.get("");
        WeatherTypes.get("");
    }

    private void rename() {
        System.out.println("DataFolder: " + getDataFolder());
        File dir = new File(getDataFolder().getParentFile(), "FastAsyncWorldEdit");
        try {
            Field descriptionField = PluginBase.class.getDeclaredField("dataFolder");
            descriptionField.setAccessible(true);
            descriptionField.set(this, dir);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            File pluginsFolder = MainUtil.getJarFile().getParentFile();

            for (File file : pluginsFolder.listFiles()) {
                if (file.length() == 2052) {
                    return;
                }
            }
            Plugin plugin = getServer().getPluginManager().getPlugin("FastAsyncWorldEdit");
            File dummy = MainUtil.copyFile(MainUtil.getJarFile(), "DummyFawe.src", pluginsFolder, "DummyFawe.jar");
            if (dummy != null && dummy.exists() && plugin == this) {
//                try {
//                    getServer().getPluginManager().loadPlugin(dummy);
//                } catch (Throwable e) {
//                    if (getServer().getUpdateFolderFile().mkdirs()) {
//                        MainUtil.copyFile(MainUtil.getJarFile(), "DummyFawe.src", pluginsFolder, getServer().getUpdateFolder() + File.separator + "DummyFawe.jar");
//                    } else {
//                        getLogger().info("Please delete DummyFawe.jar and restart");
//                    }
//                }
                getLogger().info("Please restart the server if you have any plugins which depend on FAWE.");
            } else if (dummy == null) {
                MainUtil.copyFile(MainUtil.getJarFile(), "DummyFawe.src", pluginsFolder, "update" + File.separator + "DummyFawe.jar");
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void fail(Runnable run, String message) {
        try {
            run.run();
        } catch (Throwable e) {
            getLogger().error(message);
            e.printStackTrace();
        }
    }

    private void loadConfig() {
        createDefaultConfiguration("config-legacy.yml"); // Create the default configuration file

        config = new CloudburstConfiguration(new YAMLProcessor(new File(getDataFolder(), "config-legacy.yml"), true), this);
        config.load();
        // Create schematics folder
        WorldEdit worldEdit = WorldEdit.getInstance();
        File dir = worldEdit.getWorkingDirectoryFile(worldEdit.getConfiguration().saveDir);
        dir.mkdirs();
    }

    /**
     * Called on plugin disable.
     */
    @Override
    public void onDisable() {
        Fawe.get().onDisable();
        WorldEdit worldEdit = WorldEdit.getInstance();
        worldEdit.getSessionManager().unload();
        worldEdit.getPlatformManager().unregister(server);
        if (config != null) {
            config.unload();
        }
        if (server != null) {
            server.unregisterCommands();
        }
        this.getServer().getScheduler().cancelTask(this);
    }

    /**
     * Loads and reloads all configuration.
     */
    protected void loadConfiguration() {
        config.unload();
        config.load();
        getPermissionsResolver().load();
    }

    /**
     * Create a default configuration file from the .jar.
     *
     * @param name the filename
     */
    protected void createDefaultConfiguration(String name) {
        File actual = new File(getDataFolder(), name);
        if (!actual.exists()) {
            try (InputStream stream = getResource("defaults/" + name)) {
                if (stream == null) {
                    throw new FileNotFoundException();
                }
                copyDefaultConfig(stream, actual, name);
            } catch (IOException e) {
                getLogger().error("Unable to read default configuration: " + name);
            }
        }
    }

    private void copyDefaultConfig(InputStream input, File actual, String name) {
        try (FileOutputStream output = new FileOutputStream(actual)) {
            byte[] buf = new byte[8192];
            int length;
            while ((length = input.read(buf)) > 0) {
                output.write(buf, 0, length);
            }

            getLogger().info("Default configuration file written: " + name);
        } catch (IOException e) {
            getLogger().warn("Failed to write default config file", e);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        // Add the command to the array because the underlying command handling
        // code of WorldEdit expects it
        String[] split = new String[args.length + 1];
        System.arraycopy(args, 0, split, 1, args.length);
        split[0] = commandLabel;

        CommandEvent event = new CommandEvent(wrapCommandSender(sender), Joiner.on(" ").join(split));
        getWorldEdit().getEventBus().post(event);

        return true;
    }

    /*
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        int plSep = commandLabel.indexOf(":");
        if (plSep >= 0 && plSep < commandLabel.length() + 1) {
            commandLabel = commandLabel.substring(plSep + 1);
        }

        StringBuilder sb = new StringBuilder("/").append(commandLabel);
        if (args.length > 0) {
            sb.append(" ");
        }
        String arguments = Joiner.on(" ").appendTo(sb, args).toString();
        CommandSuggestionEvent event = new CommandSuggestionEvent(wrapCommandSender(sender), arguments);
        getWorldEdit().getEventBus().post(event);
        return CommandUtil.fixSuggestions(arguments, event.getSuggestions());
    }
    */

    /**
     * Gets the session for the player.
     *
     * @param player a player
     * @return a session
     */
    public LocalSession getSession(org.cloudburstmc.server.player.Player player) {
        return WorldEdit.getInstance().getSessionManager().get(wrapPlayer(player));
    }

    /**
     * Gets the session for the player.
     *
     * @param player a player
     * @return a session
     */
    public EditSession createEditSession(org.cloudburstmc.server.player.Player player) {
        com.sk89q.worldedit.entity.Player wePlayer = wrapPlayer(player);
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(wePlayer);
        BlockBag blockBag = session.getBlockBag(wePlayer);

        EditSession editSession = WorldEdit.getInstance().getEditSessionFactory()
                .getEditSession(wePlayer.getWorld(), session.getBlockChangeLimit(), blockBag, wePlayer);
        editSession.enableStandardMode();

        return editSession;
    }

    /**
     * Remember an edit session.
     *
     * @param player      a player
     * @param editSession an edit session
     */
    public void remember(org.cloudburstmc.server.player.Player player, EditSession editSession) {
        com.sk89q.worldedit.entity.Player wePlayer = wrapPlayer(player);
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(wePlayer);

        session.remember(editSession);
        editSession.flushSession();

        WorldEdit.getInstance().flushBlockBag(wePlayer, editSession);
    }

    /**
     * Returns the configuration used by WorldEdit.
     *
     * @return the configuration
     */
    public CloudburstConfiguration getLocalConfiguration() {
        return config;
    }

    /**
     * Get the permissions resolver in use.
     *
     * @return the permissions resolver
     */
    public PermissionsResolverManager getPermissionsResolver() {
        return PermissionsResolverManager.getInstance();
    }

    /**
     * Used to wrap a Bukkit Player as a WorldEdit Player.
     *
     * @param player a player
     * @return a wrapped player
     */
    public CloudburstPlayer wrapPlayer(org.cloudburstmc.server.player.Player player) {
        CloudburstPlayer wePlayer = getCachedPlayer(player);
        if (wePlayer == null) {
            synchronized (player) {
                wePlayer = getCachedPlayer(player);
                if (wePlayer == null) {
                    wePlayer = new CloudburstPlayer(this, player);
//                    player.setMetadata("WE", new MetadataValue(this, wePlayer));
                    return wePlayer;
                }
            }
        }
        return wePlayer;
    }

    public CloudburstPlayer getCachedPlayer(org.cloudburstmc.server.player.Player player) {
        List<MetadataValue> meta = player.getMetadata("WE");
        if (meta.isEmpty()) {
            return null;
        }
        return (CloudburstPlayer) meta.get(0).value();
    }

    public Actor wrapCommandSender(CommandSender sender) {
        if (sender instanceof Player) {
            return wrapPlayer((org.cloudburstmc.server.player.Player) sender);
        } /*else if (config.commandBlockSupport && sender instanceof BlockCommandSender) {
            return new CloudburstBlockCommandSender(this, (BlockCommandSender) sender);
        } FIXME: No command blocks in Cloudburst currently */

        return new CloudburstCommandSender(this, sender);
    }

    public CloudburstServerInterface getInternalPlatform() {
        return server;
    }

    /**
     * Get WorldEdit.
     *
     * @return an instance
     */
    public WorldEdit getWorldEdit() {
        return WorldEdit.getInstance();
    }

    /**
     * Gets the instance of this plugin.
     *
     * @return an instance of the plugin
     * @throws NullPointerException if the plugin hasn't been enabled
     */
    public static WorldEditPlugin getInstance() {
        return checkNotNull(INSTANCE);
    }

    private class WorldInitListener implements Listener {
        private boolean loaded = false;

        @EventHandler(priority = EventPriority.LOWEST)
        public void onWorldInit(@SuppressWarnings("unused") LevelInitEvent event) {
            if (loaded) {
                return;
            }
            loaded = true;
            setupWorldData();
        }
    }

}
