package io.github.zefarie.herbalis.infrastructure.render;

import io.github.zefarie.herbalis.application.port.PipeRepository;
import io.github.zefarie.herbalis.application.port.PotRepository;
import io.github.zefarie.herbalis.application.port.SiloRepository;
import io.github.zefarie.herbalis.application.port.TankRepository;
import io.github.zefarie.herbalis.domain.geo.BlockPos;

import java.util.List;

/**
 * Auto-connexion visuelle des tuyaux : chaque tuyau affiche le modele
 * correspondant a ses voisins raccordables. Le masque suit l'ordre de
 * {@link BlockPos#neighbors()} : bas, haut, nord, sud, ouest, est.
 */
public final class PipeLayout {

    private final DisplayRenderer renderer;
    private final PipeRepository pipes;
    private final TankRepository tanks;
    private final SiloRepository silos;
    private final PotRepository pots;

    public PipeLayout(DisplayRenderer renderer, PipeRepository pipes,
                      TankRepository tanks, SiloRepository silos,
                      PotRepository pots) {
        this.renderer = renderer;
        this.pipes = pipes;
        this.tanks = tanks;
        this.silos = silos;
        this.pots = pots;
    }

    /** Masque de connexions d'un tuyau a cette position (6 bits). */
    public int maskOf(BlockPos pos) {
        int mask = 0;
        List<BlockPos> neighbors = pos.neighbors();
        for (int bit = 0; bit < neighbors.size(); bit++) {
            if (connectable(neighbors.get(bit))) {
                mask |= 1 << bit;
            }
        }
        return mask;
    }

    /** Reaffiche le tuyau a cette position s'il y en a un. */
    public void refresh(BlockPos pos) {
        if (pipes.has(pos)) {
            renderer.updatePipe(pos, maskOf(pos));
        }
    }

    /** Apres une pose ou une casse : la position et ses six voisins. */
    public void refreshAround(BlockPos pos) {
        refresh(pos);
        pos.neighbors().forEach(this::refresh);
    }

    private boolean connectable(BlockPos pos) {
        return pipes.has(pos)
                || tanks.at(pos).isPresent()
                || silos.at(pos).isPresent()
                || pots.exists(pos);
    }
}
