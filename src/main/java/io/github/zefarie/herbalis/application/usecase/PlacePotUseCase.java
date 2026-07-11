package io.github.zefarie.herbalis.application.usecase;

import io.github.zefarie.herbalis.application.port.PotRepository;
import io.github.zefarie.herbalis.domain.geo.BlockPos;

/**
 * Pose d'un pot de culture.
 */
public final class PlacePotUseCase {

    private final PotRepository pots;

    public PlacePotUseCase(PotRepository pots) {
        this.pots = pots;
    }

    /** @return vrai si le pot a ete pose, faux si la position est occupee */
    public boolean execute(BlockPos pos) {
        if (pots.exists(pos)) {
            return false;
        }
        pots.add(pos);
        return true;
    }
}
