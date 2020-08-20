package com.boydti.fawe.bukkit.regions;

import com.boydti.fawe.regions.FaweMask;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.*;
import com.sk89q.worldedit.bukkit.CloudburstAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class TownyFeature extends BukkitMaskManager implements Listener {

    private final Plugin towny;

    public TownyFeature(Plugin townyPlugin) {
        super(townyPlugin.getName());
        this.towny = townyPlugin;
    }

    public boolean isAllowed(Player player, TownBlock block) {
        if (block == null) {
            return false;
        }
        Resident resident;
        try {
            resident = TownyUniverse.getDataSource().getResident(player.getName());
            try {
                if (block.getResident().equals(resident)) {
                    return true;
                }
            } catch (NotRegisteredException ignored) {
            }
            Town town = block.getTown();
            if (town.isMayor(resident)) {
                return true;
            }
            if (!town.hasResident(resident)) {
                return false;
            }
            if (player.hasPermission("fawe.towny.*")) {
                return true;
            }
            for (String rank : resident.getTownRanks()) {
                if (player.hasPermission("fawe.towny." + rank)) {
                    return true;
                }
            }
        } catch (NotRegisteredException ignored) {
        }
        return false;
    }

    @Override
    public FaweMask getMask(final com.sk89q.worldedit.entity.Player wePlayer, MaskType type) {
        final Player player = CloudburstAdapter.adapt(wePlayer);
        final Location location = player.getLocation();
        try {
            final PlayerCache cache = ((Towny) this.towny).getCache(player);
            final WorldCoord mycoord = cache.getLastTownBlock();
            if (mycoord == null) {
                return null;
            }
            final TownBlock myplot = mycoord.getTownBlock();
            if (myplot == null) {
                return null;
            }
            boolean isMember = isAllowed(player, myplot);
            if (isMember) {
                final Chunk chunk = location.getChunk();
                final BlockVector3 pos1 = BlockVector3
                    .at(chunk.getX() << 4, 0, chunk.getZ() << 4);
                final BlockVector3 pos2 = BlockVector3.at(
                    (chunk.getX() << 4) + 15, 156, (chunk.getZ() << 4)
                        + 15);
                return new FaweMask(new CuboidRegion(pos1, pos2)) {
                    @Override
                    public boolean isValid(com.sk89q.worldedit.entity.Player player, MaskType type) {
                        return isAllowed(CloudburstAdapter.adapt(player), myplot);
                    }
                };
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
