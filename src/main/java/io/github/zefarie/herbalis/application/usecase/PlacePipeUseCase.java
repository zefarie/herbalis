package io.github.zefarie.herbalis.application.usecase;

import io.github.zefarie.herbalis.application.port.PipeRepository;
import io.github.zefarie.herbalis.domain.geo.BlockPos;

/**
 * Pose d'un tuyau d'irrigation.
 */
public final class PlacePipeUseCase {

    private final PipeRepository pipes;

    public PlacePipeUseCase(PipeRepository pipes) {
        this.pipes = pipes;
    }

    /** @return vrai si le tuyau a ete pose, faux si la position est occupee */
    public boolean execute(BlockPos pos) {
        if (pipes.has(pos)) {
            return false;
        }
        pipes.add(pos);
        return true;
    }
}
