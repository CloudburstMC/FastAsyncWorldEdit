package com.boydti.fawe.cloudburst.listener;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.util.TaskManager;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.event.EventHandler;
import org.cloudburstmc.server.event.EventPriority;
import org.cloudburstmc.server.event.Listener;
import org.cloudburstmc.server.event.player.PlayerJoinEvent;
import org.cloudburstmc.server.event.player.PlayerMoveEvent;
import org.cloudburstmc.server.event.player.PlayerQuitEvent;
import org.cloudburstmc.server.event.player.PlayerTeleportEvent;
import org.cloudburstmc.server.level.Location;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.plugin.Plugin;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RenderListener implements Listener {

    private final Map<UUID, int[]> views = new ConcurrentHashMap<>();
    private Iterator<Map.Entry<UUID, int[]>> entrySet;
    private int offset = 6;

    public RenderListener(Plugin plugin) {
        Server.getInstance().getPluginManager().registerEvents(this, plugin);
        TaskManager.IMP.repeat(new Runnable() {
            private long last = 0;

            @Override
            public void run() {
                if (views.isEmpty()) {
                    return;
                }

                long now = System.currentTimeMillis();
                int tps32 = (int) (Math.round(Fawe.get().getTimer().getTPS()) * 32);
                long diff = now - last;
                last = now;
                if (diff > 75) {
                    offset = diff > 100 ? 0 : 4;
                    return;
                }
                int timeOut;
                if (diff < 55 && tps32 > 608) {
                    offset = 8;
                    timeOut = 2;
                } else {
                    offset = 1 + (tps32 / 102400);
                    timeOut = 162 - (tps32 / 2560);
                }
                if (entrySet == null || !entrySet.hasNext()) {
                    entrySet = views.entrySet().iterator();
                }
                int nowTick = (int) (Fawe.get().getTimer().getTick());
                while (entrySet.hasNext()) {
                    Map.Entry<UUID, int[]> entry = entrySet.next();
                    Optional<Player> player = Server.getInstance().getPlayer(entry.getKey());
                    if (player.isPresent()) {
                        int[] value = entry.getValue();
                        if (nowTick - value[1] >= timeOut) {
                            value[1] = nowTick + 1;
                            setViewDistance(player.get(), Math.max(4, value[0] + 1));
                            long spent = System.currentTimeMillis() - now;
                            if (spent > 5) {
                                if (spent > 10) {
                                    value[1] = nowTick + 20;
                                }
                                return;
                            }
                        }
                    }
                }
            }
        }, 1);
    }

    private void setViewDistance(Player player, int value) {
        UUID uuid = player.getServerId();
        if (value == Settings.IMP.EXPERIMENTAL.DYNAMIC_CHUNK_RENDERING) {
            views.remove(uuid);
        } else {
            int[] val = views.get(uuid);
            if (val == null) {
                val = new int[] {value, (int) Fawe.get().getTimer().getTick()};
                UUID uid = player.getServerId();
                views.put(uid, val);
            } else {
                if (value <= val[0]) {
                    val[1] = (int) Fawe.get().getTimer().getTick();
                }
                if (val[0] == value) {
                    return;
                } else {
                    val[0] = value;
                }
            }
        }
        player.setChunkRadius(value);
    }

    private int getViewDistance(Player player) {
        int[] value = views.get(player.getServerId());
        return value == null ? Settings.IMP.EXPERIMENTAL.DYNAMIC_CHUNK_RENDERING : value[0];
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        setViewDistance(event.getPlayer(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getFloorX() >> offset != to.getFloorX() >> offset
            || from.getFloorZ() >> offset != to.getFloorZ() >> offset) {
            Player player = event.getPlayer();
            int currentView = getViewDistance(player);
            setViewDistance(player, Math.max(currentView - 1, 1));
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        setViewDistance(player, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uid = player.getServerId();
        views.remove(uid);
    }
}
