package io.github.zefarie.herbalis.infrastructure.render;

import io.github.zefarie.herbalis.domain.geo.BlockPos;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Optional;
import java.util.UUID;

/**
 * Conversion entre {@link BlockPos} du domaine et le monde Bukkit,
 * plus l'encodage compact utilise dans les PDC d'entites.
 */
public final class PosCodec {

    private PosCodec() {
    }

    public static BlockPos of(Block block) {
        return new BlockPos(block.getWorld().getUID(),
                block.getX(), block.getY(), block.getZ());
    }

    public static Optional<World> world(BlockPos pos) {
        return Optional.ofNullable(Bukkit.getWorld(pos.worldId()));
    }

    /** Coin bas du bloc (pour spawner les Interactions). */
    public static Optional<Location> base(BlockPos pos) {
        return world(pos).map(w ->
                new Location(w, pos.x() + 0.5, pos.y(), pos.z() + 0.5));
    }

    /** Centre exact du bloc (pour les Item Display en transform NONE). */
    public static Optional<Location> center(BlockPos pos) {
        return world(pos).map(w ->
                new Location(w, pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5));
    }

    /** Coin (0,0,0) du bloc, pour les FX. */
    public static Optional<Location> corner(BlockPos pos) {
        return world(pos).map(w -> new Location(w, pos.x(), pos.y(), pos.z()));
    }

    public static String encode(BlockPos pos) {
        return pos.worldId() + ";" + pos.x() + ";" + pos.y() + ";" + pos.z();
    }

    public static Optional<BlockPos> decode(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String[] parts = raw.split(";");
        if (parts.length != 4) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BlockPos(UUID.fromString(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3])));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
