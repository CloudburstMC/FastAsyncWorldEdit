package com.sk89q.worldedit.cloudburst;

import com.sk89q.worldedit.world.registry.BundledItemRegistry;
import org.cloudburstmc.server.registry.ItemRegistry;
import org.cloudburstmc.server.utils.Identifier;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class CloudburstItemRegistry extends BundledItemRegistry {

    @Override
    public Collection<String> values() {
        Set<String> items = new HashSet<>();

        for (Identifier identifier : ItemRegistry.get().getItems()) {
            items.add(identifier.toString().toLowerCase(Locale.US));
        }

        items.addAll(CloudburstBlockRegistry.BLOCKS);

        return items;
    }
}
