package com.boydti.fawe.cloudburst.listener;

import com.boydti.fawe.cloudburst.util.image.CloudburstImageViewer;
import com.boydti.fawe.object.brush.visualization.cfi.HeightMapMCAGenerator;
import com.boydti.fawe.util.image.ImageViewer;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.event.Event;
import org.cloudburstmc.server.event.EventHandler;
import org.cloudburstmc.server.event.EventPriority;
import org.cloudburstmc.server.event.Listener;
import org.cloudburstmc.server.event.entity.EntityDamageByEntityEvent;
import org.cloudburstmc.server.event.player.PlayerEvent;
import org.cloudburstmc.server.event.player.PlayerInteractEntityEvent;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.plugin.Plugin;

public class CloudburstImageListener implements Listener {

//    private Location mutable = new Location(Bukkit.getWorlds().get(0), 0, 0, 0);

    public CloudburstImageListener(Plugin plugin) {
        Server.getInstance().getPluginManager().registerEvents(this, plugin);
    }

    //TODO Fix along with CFI code 2020-02-04
    //    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    //    public void onPlayerInteractEntity(AsyncPlayerChatEvent event) {
    //        Set<Player> recipients = event.getRecipients();
    //        Iterator<Player> iter = recipients.iterator();
    //        while (iter.hasNext()) {
    //            Player player = iter.next();
    //            BukkitPlayer bukkitPlayer = BukkitAdapter.adapt(player);
    //            CFICommands.CFISettings settings = bukkitPlayer.getMeta("CFISettings");
    //            if (player.equals(event.getPlayer()) || !bukkitPlayer.hasMeta() || settings == null || !settings.hasGenerator()) {
    //                continue;
    //            }
    //
    //            String name = player.getName().toLowerCase(Locale.ROOT);
    //            if (!event.getMessage().toLowerCase(Locale.ROOT).contains(name)) {
    //                ArrayDeque<String> buffered = bukkitPlayer.getMeta("CFIBufferedMessages");
    //                if (buffered == null) {
    //                    bukkitPlayer.setMeta("CFIBufferedMessaged", buffered = new ArrayDeque<>());
    //                }
    //                String full = String.format(event.getFormat(), event.getPlayer().getDisplayName(),
    //                    event.getMessage());
    //                buffered.add(full);
    //                iter.remove();
    //            }
    //        }
    //    }

//    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
//    public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
//        if (!(event.getRemover() instanceof Player)) {
//            return;
//        }
//        handleInteract(event, (Player) event.getRemover(), event.getEntity(), false);
//    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        handleInteract(event, (Player) event.getDamager(), event.getEntity(), false);
    }

//    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
//    public void onPlayerInteract(PlayerInteractEvent event) {
//        Player player = event.getPlayer();
//        CloudPlayer cloudPlayer = BukkitAdapter.adapt(player);
//        if (cloudPlayer.getMeta("CFISettings") == null) {
//            return;
//        }
//
//        List<Block> target = player.getLastTwoTargetBlocks(null, 100);
//        if (target.isEmpty()) {
//            return;
//        }
//
//        Block targetBlock = target.get(0);
//        World world = player.getWorld();
//        mutable.setWorld(world);
//        mutable.setX(targetBlock.getX() + 0.5);
//        mutable.setY(targetBlock.getY() + 0.5);
//        mutable.setZ(targetBlock.getZ() + 0.5);
//        Collection<Entity> entities = world.getNearbyEntities(mutable, 0.46875, 0, 0.46875);
//
//        if (!entities.isEmpty()) {
//            Action action = event.getAction();
//            boolean primary =
//                action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
//
//            double minDist = Integer.MAX_VALUE;
//            ItemFrame minItemFrame = null;
//
//            for (Entity entity : entities) {
//                if (entity instanceof ItemFrame) {
//                    ItemFrame itemFrame = (ItemFrame) entity;
//                    Location loc = itemFrame.getLocation();
//                    double dx = loc.getX() - mutable.getX();
//                    double dy = loc.getY() - mutable.getY();
//                    double dz = loc.getZ() - mutable.getZ();
//                    double dist = dx * dx + dy * dy + dz * dz;
//                    if (dist < minDist) {
//                        minItemFrame = itemFrame;
//                        minDist = dist;
//                    }
//                }
//            }
//            if (minItemFrame != null) {
//                handleInteract(event, minItemFrame, primary);
//                if (event.isCancelled()) {
//                    return;
//                }
//            }
//        }
//    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        handleInteract(event, event.getEntity(), true);
    }

    private CloudburstImageViewer get(HeightMapMCAGenerator generator) {
        if (generator == null) {
            return null;
        }

        ImageViewer viewer = generator.getImageViewer();
        if (!(viewer instanceof CloudburstImageViewer)) {
            return null;
        }

        return (CloudburstImageViewer) viewer;
    }

    private void handleInteract(PlayerEvent event, Entity entity, boolean primary) {
        handleInteract(event, event.getPlayer(), entity, primary);
    }

    private void handleInteract(Event event, Player player, Entity entity, boolean primary) {
        //todo fix with cfi code 2020-02-04
        //        if (!(entity instanceof ItemFrame)) {
        //            return;
        //        }
        //        ItemFrame itemFrame = (ItemFrame) entity;
        //
        //        BukkitPlayer bukkitPlayer = BukkitAdapter.adapt(player);
        //        CFICommands.CFISettings settings = bukkitPlayer.getMeta("CFISettings");
        //        HeightMapMCAGenerator generator = settings == null ? null : settings.getGenerator();
        //        BukkitImageViewer viewer = get(generator);
        //        if (viewer == null) {
        //            return;
        //        }
        //
        //        if (itemFrame.getRotation() != Rotation.NONE) {
        //            itemFrame.setRotation(Rotation.NONE);
        //        }
        //
        //        LocalSession session = bukkitPlayer.getSession();
        //        BrushTool tool;
        //        try {
        //            tool = session.getBrushTool(bukkitPlayer, false);
        //        } catch (InvalidToolBindException e) {
        //            return;
        //        }
        //
        //        ItemFrame[][] frames = viewer.getItemFrames();
        //        if (frames == null || tool == null) {
        //            viewer.selectFrame(itemFrame);
        //            player.updateInventory();
        //            TaskManager.IMP.laterAsync(() -> viewer.view(generator), 1);
        //            return;
        //        }
        //
        //        BrushSettings context = primary ? tool.getPrimary() : tool.getSecondary();
        //        Brush brush = context.getBrush();
        //        if (brush == null) {
        //            return;
        //        }
        //        tool.setContext(context);
        //
        //        if (event instanceof Cancellable) {
        //            ((Cancellable) event).setCancelled(true);
        //        }
        //
        //        Location target = itemFrame.getLocation();
        //        Location source = player.getLocation();
        //
        //        double yawRad = Math.toRadians(source.getYaw() + 90d);
        //        double pitchRad = Math.toRadians(-source.getPitch());
        //
        //        double a = Math.cos(pitchRad);
        //        double xRat = Math.cos(yawRad) * a;
        //        double zRat = Math.sin(yawRad) * a;
        //
        //        BlockFace facing = itemFrame.getFacing();
        //        double thickness = 1 / 32D + 1 / 128D;
        //        double modX = facing.getModX();
        //        double modZ = facing.getModZ();
        //        double dx = source.getChunkX() - target.getChunkX() - modX * thickness;
        //        double dy = source.getY() + player.getEyeHeight() - target.getY();
        //        double dz = source.getZ() - target.getZ() - modZ * thickness;
        //
        //        double offset;
        //        double localX;
        //        if (modX != 0) {
        //            offset = dx / xRat;
        //            localX = (-modX) * (dz - offset * zRat);
        //        } else {
        //            offset = dz / zRat;
        //            localX = (modZ) * (dx - offset * xRat);
        //        }
        //        double localY = dy - offset * Math.sin(pitchRad);
        //        int localPixelX = (int) ((localX + 0.5) * 128);
        //        int localPixelY = (int) ((localY + 0.5) * 128);
        //
        //        UUID uuid = itemFrame.getUniqueId();
        //        for (int blockX = 0; blockX < frames.length; blockX++) {
        //            for (int blockY = 0; blockY < frames[0].length; blockY++) {
        //                if (uuid.equals(frames[blockX][blockY].getUniqueId())) {
        //                    int pixelX = localPixelX + blockX * 128;
        //                    int pixelY = (128 * frames[0].length) - (localPixelY + blockY * 128 + 1);
        //
        //                    int width = generator.getWidth();
        //                    int length = generator.getLength();
        //                    int worldX = (int) (pixelX * width / (frames.length * 128d));
        //                    int worldZ = (int) (pixelY * length / (frames[0].length * 128d));
        //
        //                    if (worldX < 0 || worldX > width || worldZ < 0 || worldZ > length) {
        //                        return;
        //                    }
        //
        //                    bukkitPlayer.runAction(() -> {
        //                        BlockVector3 wPos = BlockVector3.at(worldX, 0, worldZ);
        //                        viewer.refresh();
        //                        int topY = generator
        //                            .getNearestSurfaceTerrainBlock(wPos.getBlockX(), wPos.getBlockZ(), 255,
        //                                0, 255);
        //                        wPos = wPos.withY(topY);
        //
        //                        EditSession es = new EditSessionBuilder(bukkitPlayer.getWorld()).player(bukkitPlayer)
        //                            .combineStages(false).autoQueue(false).blockBag(null).limitUnlimited()
        //                            .build();
        //                        ExtentTraverser last = new ExtentTraverser(es.getExtent()).last();
        //                        Extent extent = last.get();
        //                        if (extent instanceof IQueueExtent) {
        //                            last = last.previous();
        //                        }
        //                        last.setNext(generator);
        //                        try {
        //                            brush.build(es, wPos, context.getMaterial(), context.getSize());
        //                        } catch (WorldEditException e) {
        //                            e.printStackTrace();
        //                        }
        //                        es.flushQueue();
        //                        viewer.view(generator);
        //                    }, true, true);
        //
        //                    return;
        //                }
        //            }
        //        }
    }
}
