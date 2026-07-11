package io.github.zefarie.herbalis.infrastructure.listener;

import io.github.zefarie.herbalis.infrastructure.render.WorldSync;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

/**
 * Respawn des Display et Interaction au chargement des chunks, oubli au
 * dechargement. Les entites sont non persistantes : la base fait foi.
 */
public final class ChunkListener implements Listener {

    private final WorldSync worldSync;

    public ChunkListener(WorldSync worldSync) {
        this.worldSync = worldSync;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        worldSync.respawnChunk(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        worldSync.forgetChunk(event.getChunk());
    }
}
