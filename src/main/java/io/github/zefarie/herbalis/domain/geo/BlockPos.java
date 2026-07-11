package io.github.zefarie.herbalis.domain.geo;

import java.util.UUID;

/**
 * Position de bloc independante de Bukkit, identifiee par le monde et
 * des coordonnees entieres.
 */
public record BlockPos(UUID worldId, int x, int y, int z) {

    public int chunkX() {
        return x >> 4;
    }

    public int chunkZ() {
        return z >> 4;
    }

    public BlockPos above() {
        return new BlockPos(worldId, x, y + 1, z);
    }

    public BlockPos below() {
        return new BlockPos(worldId, x, y - 1, z);
    }
}
