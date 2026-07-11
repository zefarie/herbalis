package io.github.zefarie.herbalis.infrastructure.world;

import io.github.zefarie.herbalis.application.port.PlantEnvironment;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import org.bukkit.Bukkit;
import org.bukkit.World;

/**
 * Environnement physique des plantes, adosse au monde Bukkit.
 * Le niveau de lumiere est le niveau combine (blocs + ciel) : une serre
 * eclairee pousse la nuit, une plante en exterieur marque une pause
 * nocturne si rien ne l'eclaire.
 */
public final class BukkitPlantEnvironment implements PlantEnvironment {

    @Override
    public boolean isLoaded(BlockPos pos) {
        World world = Bukkit.getWorld(pos.worldId());
        return world != null && world.isChunkLoaded(pos.chunkX(), pos.chunkZ());
    }

    @Override
    public int lightLevel(BlockPos pos) {
        World world = Bukkit.getWorld(pos.worldId());
        if (world == null) {
            return 0;
        }
        return world.getBlockAt(pos.x(), pos.y(), pos.z()).getLightLevel();
    }
}
