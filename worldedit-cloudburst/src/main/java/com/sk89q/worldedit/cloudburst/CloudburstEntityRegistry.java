package com.sk89q.worldedit.cloudburst;

import com.sk89q.worldedit.world.registry.EntityRegistry;
import org.cloudburstmc.server.entity.EntityType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CloudburstEntityRegistry implements EntityRegistry {
    @Override
    public Collection<String> registerEntities() {
        List<String> types = new ArrayList<>();
        for (EntityType<?> type : org.cloudburstmc.server.registry.EntityRegistry.get().getEntityTypes()) {
            types.add(type.getIdentifier().toString());
        }
        return types;
    }
}
