package com.boydti.fawe.cloudburst.adapter.mc1161;

import com.sk89q.util.ReflectionUtil;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import net.minecraft.server.v1_16_R1.*;
import org.bukkit.craftbukkit.v1_16_R1.block.data.CraftBlockData;

public class BlockMaterial1161 implements BlockMaterial {
    private final Block block;
    private final IBlockData defaultState;
    private final Material material;
    private final boolean isTranslucent;
    private final CraftBlockData craftBlockData;
    private final org.bukkit.Material craftMaterial;

    public BlockMaterial1161(Block block) {
        this(block, block.getBlockData());
    }

    public BlockMaterial1161(Block block, IBlockData defaultState) {
        this.block = block;
        this.defaultState = defaultState;
        this.material = defaultState.getMaterial();
        this.craftBlockData = CraftBlockData.fromData(defaultState);
        this.craftMaterial = craftBlockData.getMaterial();
        this.isTranslucent = !(boolean) ReflectionUtil.getField(Block.class, block, "at"); //TODO Update Mapping for 1.16.1
    }

    public Block getBlock() {
        return block;
    }

    public IBlockData getState() {
        return defaultState;
    }

    public CraftBlockData getCraftBlockData() {
        return craftBlockData;
    }

    public Material getMaterial() {
        return material;
    }

    @Override
    public boolean isAir() {
        return defaultState.isAir();
    }

    @Override
    public boolean isFullCube() {
        return craftMaterial.isOccluding();
    }

    @Override
    public boolean isOpaque() {
        return material.f();
    }

    @Override
    public boolean isPowerSource() {
        return defaultState.isPowerSource();
    }

    @Override
    public boolean isLiquid() {
        return material.isLiquid();
    }

    @Override
    public boolean isSolid() {
        return material.isBuildable();
    }

    @Override
    public float getHardness() {
        return craftBlockData.getState().strength;
    }

    @Override
    public float getResistance() {
        return block.getDurability();
    }

    @Override
    public float getSlipperiness() {
        return block.getFrictionFactor();
    }

    @Override
    public int getLightValue() {
        return defaultState.f();
    }

    @Override
    public int getLightOpacity() {
        return !isTranslucent() ? 15 : 0;
    }

    @Override
    public boolean isFragileWhenPushed() {
        return material.getPushReaction() == EnumPistonReaction.DESTROY;
    }

    @Override
    public boolean isUnpushable() {
        return material.getPushReaction() == EnumPistonReaction.BLOCK;
    }

    @Override
    public boolean isTicksRandomly() {
        return block.isTicking(defaultState);
    }

    @Override
    public boolean isMovementBlocker() {
        return material.isSolid();
    }

    @Override
    public boolean isBurnable() {
        return material.isBurnable();
    }

    @Override
    public boolean isToolRequired() {
        //TODO Removed in 1.16.1 Replacement not found.
        return true;
    }

    @Override
    public boolean isReplacedDuringPlacement() {
        return material.isReplaceable();
    }

    @Override
    public boolean isTranslucent() {
        return isTranslucent;
    }

    @Override
    public boolean hasContainer() {
        return block instanceof ITileEntity;
    }

    @Override
    public int getMapColor() {
        return material.h().rgb;
    }
}
