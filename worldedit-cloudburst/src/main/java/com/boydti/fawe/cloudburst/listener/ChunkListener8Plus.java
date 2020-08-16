package com.boydti.fawe.cloudburst.listener;


import org.cloudburstmc.server.event.EventHandler;
import org.cloudburstmc.server.event.EventPriority;
import org.cloudburstmc.server.event.Listener;
import org.cloudburstmc.server.event.entity.EntityExplodeEvent;

public class ChunkListener8Plus implements Listener {

    private final ChunkListener listener;

    public ChunkListener8Plus(ChunkListener listener) {
        this.listener = listener;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(EntityExplodeEvent event) {
        listener.reset();
    }
}
