package io.github.zefarie.herbalis.infrastructure.world;

import io.github.zefarie.herbalis.domain.geo.BlockPos;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Optional;

/**
 * Les structures (tuyaux, cuves, silos, racks, jarres) sont des Item
 * Display : l'entite Interaction ne fournit que la hitbox de clic, pas
 * de collision physique. Un bloc {@code minecraft:barrier} (invisible,
 * incassable en survie) est pose sous le visuel pour que joueurs et
 * entites ne passent pas au travers.
 */
public final class CollisionBlocks {

    private CollisionBlocks() {
    }

    /** Pose (ou repose) le bloc de collision a cette position. */
    public static void place(BlockPos pos) {
        block(pos).filter(b -> b.getType().isAir()
                || b.getType() == Material.BARRIER)
                .ifPresent(b -> b.setType(Material.BARRIER, false));
    }

    /** Retire le bloc de collision, sans toucher a autre chose. */
    public static void remove(BlockPos pos) {
        block(pos).filter(b -> b.getType() == Material.BARRIER)
                .ifPresent(b -> b.setType(Material.AIR, true));
    }

    private static Optional<Block> block(BlockPos pos) {
        World world = Bukkit.getWorld(pos.worldId());
        return world == null ? Optional.empty()
                : Optional.of(world.getBlockAt(pos.x(), pos.y(), pos.z()));
    }
}
