package com.boydti.fawe.bukkit;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.IFawe;
import com.boydti.fawe.beta.implementation.cache.preloader.AsyncPreloader;
import com.boydti.fawe.beta.implementation.cache.preloader.Preloader;
import com.boydti.fawe.beta.implementation.queue.QueueHandler;
import com.boydti.fawe.bukkit.adapter.BukkitQueueHandler;
import com.boydti.fawe.bukkit.listener.*;
import com.boydti.fawe.bukkit.regions.*;
import com.boydti.fawe.bukkit.util.BukkitTaskMan;
import com.boydti.fawe.bukkit.util.ItemUtil;
import com.boydti.fawe.bukkit.util.VaultUtil;
import com.boydti.fawe.bukkit.util.image.BukkitImageViewer;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.regions.FaweMaskManager;
import com.boydti.fawe.util.Jars;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.util.WEManager;
import com.boydti.fawe.util.image.ImageViewer;
import com.sk89q.worldedit.bukkit.CloudburstAdapter;
import com.sk89q.worldedit.bukkit.CloudburstPlayer;
import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.cloudburstmc.server.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;

public class FaweCloudburst implements IFawe, Listener {

    private static final Logger log = LoggerFactory.getLogger(FaweCloudburst.class);

    private final Plugin plugin;
    private VaultUtil vault;
    private ItemUtil itemUtil;

    private boolean listeningImages;
    private BukkitImageListener imageListener;
    private CFIPacketListener packetListener;
    private final boolean chunksStretched;

    public VaultUtil getVault() {
        return this.vault;
    }

    public FaweCloudburst(Plugin plugin) {
        this.plugin = plugin;
        try {
            Settings.IMP.TICK_LIMITER.ENABLED = !plugin.getServer().hasWhitelist();
            Fawe.set(this);
            Fawe.setupInjector();
            try {
                new BrushListener(plugin);
            } catch (Throwable e) {
                log.debug("Brush Listener Failed", e);
            }
            if (PaperLib.isPaper() && Settings.IMP.EXPERIMENTAL.DYNAMIC_CHUNK_RENDERING > 1) {
                new RenderListener(plugin);
            }
        } catch (final Throwable e) {
            e.printStackTrace();
            Bukkit.getServer().shutdown();
        }

        chunksStretched =
            Integer.parseInt(Bukkit.getBukkitVersion().split("-")[0].split("\\.")[1]) >= 16;

        //Vault is Spigot/Paper only so this needs to be done in the Bukkit module
        setupVault();

        //PlotSquared support is limited to Spigot/Paper as of 02/20/2020
        TaskManager.IMP.later(this::setupPlotSquared, 0);

        // Registered delayed Event Listeners
        TaskManager.IMP.task(() -> {
            // Fix for ProtocolSupport
            Settings.IMP.PROTOCOL_SUPPORT_FIX = Bukkit.getPluginManager()
                                                      .isPluginEnabled("ProtocolSupport");

            // This class
            Bukkit.getPluginManager().registerEvents(FaweCloudburst.this, FaweCloudburst.this.plugin);

            // The tick limiter
            new ChunkListener9();
        });
    }

    @Override // Please don't delete this again, it's WIP
    public void registerPacketListener() {
        PluginManager manager = Bukkit.getPluginManager();
        if (packetListener == null && manager.getPlugin("ProtocolLib") != null) {
            packetListener = new CFIPacketListener(plugin);
        }
    }

    @Override
    public QueueHandler getQueueHandler() {
        return new BukkitQueueHandler();
    }

    @Override
    public synchronized ImageViewer getImageViewer(com.sk89q.worldedit.entity.Player player) {
        if (listeningImages && imageListener == null) {
            return null;
        }
        try {
            listeningImages = true;
            registerPacketListener();
            PluginManager manager = Bukkit.getPluginManager();

            if (manager.getPlugin("PacketListenerApi") == null) {
                File output = new File(plugin.getDataFolder()
                                             .getParentFile(), "PacketListenerAPI_v3.7.6-SNAPSHOT.jar");
                byte[] jarData = Jars.PL_v3_7_6.download();
                try (FileOutputStream fos = new FileOutputStream(output)) {
                    fos.write(jarData);
                }
            }
            if (manager.getPlugin("MapManager") == null) {
                File output = new File(plugin.getDataFolder()
                                             .getParentFile(), "MapManager_v1.7.8-SNAPSHOT.jar");
                byte[] jarData = Jars.MM_v1_7_8.download();
                try (FileOutputStream fos = new FileOutputStream(output)) {
                    fos.write(jarData);
                }
            }
            BukkitImageViewer viewer = new BukkitImageViewer(CloudburstAdapter.adapt(player));
            if (imageListener == null) {
                this.imageListener = new BukkitImageListener(plugin);
            }
            return viewer;
        } catch (Throwable ignored) {
        }
        return null;
    }

    @Override
    public void debug(final String message) {
        log.debug(message);
    }

    @Override
    public File getDirectory() {
        return plugin.getDataFolder();
    }


    public ItemUtil getItemUtil() {
        ItemUtil tmp = itemUtil;
        if (tmp == null) {
            try {
                this.itemUtil = tmp = new ItemUtil();
            } catch (Throwable e) {
                Settings.IMP.EXPERIMENTAL.PERSISTENT_BRUSHES = false;
                log.debug("Persistent Brushes Failed", e);
            }
        }
        return tmp;
    }

    private void setupVault() {
        try {
            this.vault = new VaultUtil();
        } catch (final Throwable ignored) {
        }
    }

    @Override
    public String getDebugInfo() {
        StringBuilder msg = new StringBuilder();
        msg.append("Server Version: ").append(Bukkit.getVersion()).append("\n");
        msg.append("Plugins: \n");
        for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
            msg.append(" - ").append(p.getName()).append(": ")
               .append(p.getDescription().getVersion()).append("\n");
        }
        return msg.toString();
    }

    /**
     * The task manager handles sync/async tasks.
     */
    @Override
    public TaskManager getTaskManager() {
        return new BukkitTaskMan(plugin);
    }

    public Plugin getPlugin() {
        return plugin;
    }

    /**
     * A mask manager handles region restrictions e.g., PlotSquared plots / WorldGuard regions
     */
    @Override
    public Collection<FaweMaskManager> getMaskManagers() {
        final Plugin worldguardPlugin = Bukkit.getServer().getPluginManager()
                                              .getPlugin("WorldGuard");
        final ArrayList<FaweMaskManager> managers = new ArrayList<>();
        if (worldguardPlugin != null && worldguardPlugin.isEnabled()) {
            try {
                managers.add(new Worldguard(worldguardPlugin));
                log.debug("Attempting to use plugin 'WorldGuard'");
            } catch (Throwable ignored) {
            }
        }
        final Plugin townyPlugin = Bukkit.getServer().getPluginManager().getPlugin("Towny");
        if (townyPlugin != null && townyPlugin.isEnabled()) {
            try {
                managers.add(new TownyFeature(townyPlugin));
                log.debug("Attempting to use plugin 'Towny'");
            } catch (Throwable ignored) {
            }
        }
        final Plugin residencePlugin = Bukkit.getServer().getPluginManager().getPlugin("Residence");
        if (residencePlugin != null && residencePlugin.isEnabled()) {
            try {
                managers.add(new ResidenceFeature(residencePlugin, this));
                log.debug("Attempting to use plugin 'Residence'");
            } catch (Throwable ignored) {
            }
        }
        final Plugin griefpreventionPlugin = Bukkit.getServer().getPluginManager()
                                                   .getPlugin("GriefPrevention");
        if (griefpreventionPlugin != null && griefpreventionPlugin.isEnabled()) {
            try {
                managers.add(new GriefPreventionFeature(griefpreventionPlugin));
                log.debug("Attempting to use plugin 'GriefPrevention'");
            } catch (Throwable ignored) {
            }
        }

        if (Settings.IMP.EXPERIMENTAL.FREEBUILD) {
            try {
                managers.add(new FreeBuildRegion());
                log.debug("Attempting to use plugin '<internal.freebuild>'");
            } catch (Throwable ignored) {
            }
        }

        return managers;
    }

    private volatile boolean keepUnloaded;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        if (keepUnloaded) {
            org.bukkit.World world = event.getWorld();
            world.setKeepSpawnInMemory(false);
        }
    }

    public synchronized <T> T createWorldUnloaded(Supplier<T> task) {
        keepUnloaded = true;
        try {
            return task.get();
        } finally {
            keepUnloaded = false;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        CloudburstPlayer wePlayer = CloudburstAdapter.adapt(player);
        wePlayer.unregister();
    }

    @SuppressWarnings("deprecation")
    @Override
    public UUID getUUID(String name) {
        return Bukkit.getOfflinePlayer(name).getUniqueId();
    }

    @Override
    public String getName(UUID uuid) {
        return Bukkit.getOfflinePlayer(uuid).getName();
    }

    @Override
    public Preloader getPreloader() {
        if (PaperLib.isPaper()) {
            return new AsyncPreloader();
        }
        return null;
    }

    @Override
    public boolean isChunksStretched() {
        return chunksStretched;
    }

    private void setupPlotSquared() {
        Plugin plotSquared = this.plugin.getServer().getPluginManager().getPlugin("PlotSquared");
        if (plotSquared == null) {
            return;
        }
        if (plotSquared.getClass().getPackage().toString().contains("intellectualsites")) {
            WEManager.IMP.managers
                .add(new com.boydti.fawe.bukkit.regions.plotsquaredv4.PlotSquaredFeature());
        } else {
            WEManager.IMP.managers
                .add(new com.boydti.fawe.bukkit.regions.plotsquared.PlotSquaredFeature());
        }
        log.info("Plugin 'PlotSquared' found. Using it now.");
    }
}
