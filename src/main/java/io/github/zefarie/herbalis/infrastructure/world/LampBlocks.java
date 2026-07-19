package io.github.zefarie.herbalis.infrastructure.world;

import io.github.zefarie.herbalis.domain.geo.BlockPos;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Light;

import java.util.Optional;

/**
 * La lampe UV emet une vraie lumiere : un bloc {@code minecraft:light}
 * (invisible, traversable) est pose sous le visuel pour que le moteur
 * de croissance et le monde voient un niveau de lumiere reel.
 */
public final class LampBlocks {

    private LampBlocks() {
    }

    /** Pose (ou repose) le bloc lumineux au niveau demande. */
    public static void place(BlockPos pos, int level) {
        block(pos).filter(b -> b.getType().isAir()
                || b.getType() == Material.LIGHT).ifPresent(b -> {
            b.setType(Material.LIGHT, false);
            if (b.getBlockData() instanceof Light light) {
                light.setLevel(Math.clamp(level, 1, 15));
                b.setBlockData(light, true);
            }
        });
    }

    /** Retire le bloc lumineux, sans toucher a autre chose que la lumiere. */
    public static void remove(BlockPos pos) {
        block(pos).filter(b -> b.getType() == Material.LIGHT)
                .ifPresent(b -> b.setType(Material.AIR, true));
    }

    private static Optional<Block> block(BlockPos pos) {
        World world = Bukkit.getWorld(pos.worldId());
        return world == null ? Optional.empty()
                : Optional.of(world.getBlockAt(pos.x(), pos.y(), pos.z()));
    }
}
