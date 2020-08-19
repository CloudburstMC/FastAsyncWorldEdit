package com.boydti.fawe.cloudburst.util.image;

import com.boydti.fawe.util.image.Drawable;
import com.boydti.fawe.util.image.ImageUtil;
import com.boydti.fawe.util.image.ImageViewer;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.blockentity.ItemFrame;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.inventory.PlayerInventory;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.item.ItemMap;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.math.SimpleAxisAlignedBB;
import org.cloudburstmc.server.player.Player;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Set;

public class CloudburstImageViewer implements ImageViewer {
    private final Player player;
    private BufferedImage last;
    private ItemFrame[][] frames;
    private boolean reverse;

    public CloudburstImageViewer(Player player) {
        this.player = player;
    }

    public void selectFrame(ItemFrame start) {
        Vector3i pos1 = start.getPosition();
        Vector3i pos2 = start.getPosition();
        Level level = start.getLevel();

        Direction direction = start.getBlockState().ensureTrait(BlockTraits.FACING_DIRECTION);
        int planeX = direction.getXOffset() == 0 ? 1 : 0;
        int planeY = direction.getYOffset() == 0 ? 1 : 0;
        int planeZ = direction.getZOffset() == 0 ? 1 : 0;

        ItemFrame[][] res = find(level, pos1, pos2, direction);
        Vector3i tmp;
        while (true) {
            if (res != null) {
                frames = res;
            }
            tmp = pos1.sub(planeX, planeY, planeZ);
            if ((res = find(level, tmp, pos2, direction)) != null) {
                pos1 = tmp;
                continue;
            }
            tmp = pos2.add(planeX, planeY, planeZ);
            if ((res = find(level, pos1, tmp, direction)) != null) {
                pos2 = tmp;
                continue;
            }
            tmp = pos1.sub(planeX, 0, planeZ);
            if ((res = find(level, tmp, pos2, direction)) != null) {
                pos1 = tmp;
                continue;
            }
            tmp = pos2.add(planeX, 0, planeZ);
            if ((res = find(level, pos1, tmp, direction)) != null) {
                pos2 = tmp;
                continue;
            }
            tmp = pos1.sub(0, 1, 0);
            if ((res = find(level, tmp, pos2, direction)) != null) {
                pos1 = tmp;
                continue;
            }
            tmp = pos2.add(0, 1, 0);
            if ((res = find(level, pos1, tmp, direction)) != null) {
                pos2 = tmp;
                continue;
            }
            break;
        }
    }

    public ItemFrame[][] getItemFrames() {
        return frames;
    }

    private ItemFrame[][] find(Level level, Vector3i pos1, Vector3i pos2, Direction direction) {
        try {
            Vector3i distance = pos2.sub(pos1).add(1, 1, 1);
            int width = Math.max(distance.getX(), distance.getZ());
            ItemFrame[][] frames = new ItemFrame[width][distance.getY()];

            this.reverse = direction == Direction.NORTH || direction == Direction.EAST;
            int v = 0;
            for (double y = pos1.getY(); y <= pos2.getY(); y++, v++) {
                int h = 0;
                for (double z = pos1.getZ(); z <= pos2.getZ(); z++) {
                    for (double x = pos1.getX(); x <= pos2.getX(); x++, h++) {
                        Vector3f pos = Vector3f.from(x, y, z);
                        AxisAlignedBB bb = new SimpleAxisAlignedBB(pos.sub(0.1, 0.1, 0.1), pos.add(0.1, 0.1, 0.1));
                        Set<Entity> entities = level.getNearbyEntities(bb);
                        boolean contains = false;
                        for (Entity ent : entities) {
                            if (ent instanceof ItemFrame && ent.getDirection() == direction) {
                                ItemFrame itemFrame = (ItemFrame) ent;
                                itemFrame.setItemRotation(0);
                                contains = true;
                                frames[reverse ? width - 1 - h : h][v] = (ItemFrame) ent;
                                break;
                            }
                        }
                        if (!contains) {
                            return null;
                        }
                    }
                }
            }
            return frames;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void view(Drawable drawable) {
        view(null, drawable);
    }

    private void view(@Nullable BufferedImage image, @Nullable Drawable drawable) {
        if (image == null && drawable == null) {
            throw new IllegalArgumentException("An image or drawable must be provided. Both cannot be null");
        }
        boolean initializing = last == null;

        if (this.frames != null) {
            if (image == null && drawable != null) {
                image = drawable.draw();
            }
            last = image;
            int width = frames.length;
            int height = frames[0].length;
            BufferedImage scaled = ImageUtil.getScaledInstance(image, 128 * width, 128 * height, RenderingHints.VALUE_INTERPOLATION_BILINEAR, false);
            // TODO: Add support for this.
//            MapWrapper mapWrapper = mapManager.wrapMultiImage(scaled, width, height);
//            MultiMapController controller = (MultiMapController) mapWrapper.getController();
//            controller.addViewer(player);
//            controller.sendContent(player);
//            controller.showInFrames(player, frames, true);
        } else {
            int slot = getMapSlot(player);
            if (slot == -1) {
                if (initializing) {
                    player.getInventory().setItemInHand(Item.get(ItemIds.MAP));
                } else {
                    return;
                }
            } else if (player.getInventory().getHeldItemSlot() != slot) {
                player.getInventory().setHeldItemSlot(slot);
            }
            if (image == null && drawable != null) {
                image = drawable.draw();
            }
            last = image;
            BufferedImage scaled = ImageUtil.getScaledInstance(image, 128, 128, RenderingHints.VALUE_INTERPOLATION_BILINEAR, false);
//            MapWrapper mapWrapper = mapManager.wrapImage(scaled);
//            MapController controller = mapWrapper.getController();
//            controller.addViewer(player);
//            controller.sendContent(player);
//            controller.showInHand(player, true);
        }
    }

    private int getMapSlot(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < 9; i++) {
            Item item = inventory.getItem(i);
            if (item != null && item instanceof ItemMap) {
                return i;
            }
        }
        return -1;
    }

    public void refresh() {
        if (last != null) {
            view(last, null);
        }
    }

    @Override
    public void close() throws IOException {
        last = null;
    }
}
