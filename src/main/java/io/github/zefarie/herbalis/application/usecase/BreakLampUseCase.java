package io.github.zefarie.herbalis.application.usecase;

import io.github.zefarie.herbalis.application.port.LampRepository;
import io.github.zefarie.herbalis.domain.geo.BlockPos;

/**
 * Casse d'une lampe horticole UV.
 */
public final class BreakLampUseCase {

    private final LampRepository lamps;

    public BreakLampUseCase(LampRepository lamps) {
        this.lamps = lamps;
    }

    /** @return vrai si une lampe etait bien presente */
    public boolean execute(BlockPos pos) {
        if (!lamps.has(pos)) {
            return false;
        }
        lamps.remove(pos);
        return true;
    }
}
