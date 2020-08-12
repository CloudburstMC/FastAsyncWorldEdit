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

package com.sk89q.worldedit.bukkit;

import com.sk89q.worldedit.entity.metadata.EntityProperties;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.ExplosiveMinecart;

import static com.google.common.base.Preconditions.checkNotNull;

class BukkitEntityProperties implements EntityProperties {

    private static final boolean HAS_ABSTRACT_VILLAGER;

    static {
        boolean temp;
        try {
            Class.forName("org.bukkit.entity.AbstractVillager");
            temp = true;
        } catch (ClassNotFoundException e) {
            temp = false;
        }
        HAS_ABSTRACT_VILLAGER = temp;
    }

    private final Entity entity;

    BukkitEntityProperties(Entity entity) {
        checkNotNull(entity);
        this.entity = entity;
    }

    @Override
    public boolean isPlayerDerived() {
        return entity instanceof HumanEntity;
    }

    @Override
    public boolean isProjectile() {
        return entity instanceof Projectile;
    }

    @Override
    public boolean isItem() {
        return entity instanceof Item;
    }

    @Override
    public boolean isFallingBlock() {
        return entity instanceof FallingBlock;
    }

    @Override
    public boolean isPainting() {
        return entity instanceof Painting;
    }

    @Override
    public boolean isItemFrame() {
        return entity instanceof ItemFrame;
    }

    @Override
    public boolean isBoat() {
        return entity instanceof Boat;
    }

    @Override
    public boolean isMinecart() {
        return entity instanceof Minecart;
    }

    @Override
    public boolean isTNT() {
        return entity instanceof TNTPrimed || entity instanceof ExplosiveMinecart;
    }

    @Override
    public boolean isExperienceOrb() {
        return entity instanceof ExperienceOrb;
    }

    @Override
    public boolean isLiving() {
        return entity instanceof LivingEntity;
    }

    @Override
    public boolean isAnimal() {
        return entity instanceof Animals;
    }

    @Override
    public boolean isAmbient() {
        return entity instanceof Ambient;
    }

    @Override
    public boolean isNPC() {
        if (HAS_ABSTRACT_VILLAGER) {
            return entity instanceof AbstractVillager;
        }
        return entity instanceof Villager;
    }

    @Override
    public boolean isGolem() {
        return entity instanceof Golem;
    }

    @Override
    public boolean isTamed() {
        return entity instanceof Tameable && ((Tameable) entity).isTamed();
    }

    @Override
    public boolean isTagged() {
        return entity instanceof LivingEntity && entity.getCustomName() != null;
    }

    @Override
    public boolean isArmorStand() {
        return entity instanceof ArmorStand;
    }

    @Override
    public boolean isPasteable() {
        return !(entity instanceof Player || entity instanceof ComplexEntityPart);
    }
}
