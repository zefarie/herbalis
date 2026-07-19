package io.github.zefarie.herbalis.application.usecase;

import io.github.zefarie.herbalis.application.port.LampRepository;
import io.github.zefarie.herbalis.domain.geo.BlockPos;

/**
 * Pose d'une lampe horticole UV.
 */
public final class PlaceLampUseCase {

    private final LampRepository lamps;

    public PlaceLampUseCase(LampRepository lamps) {
        this.lamps = lamps;
    }

    /** @return vrai si la lampe a ete posee, faux si la position est occupee */
    public boolean execute(BlockPos pos) {
        if (lamps.has(pos)) {
            return false;
        }
        lamps.add(pos);
        return true;
    }
}
