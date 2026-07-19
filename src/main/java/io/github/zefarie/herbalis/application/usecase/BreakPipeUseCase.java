package io.github.zefarie.herbalis.application.usecase;

import io.github.zefarie.herbalis.application.port.PipeRepository;
import io.github.zefarie.herbalis.domain.geo.BlockPos;

/**
 * Casse d'un tuyau d'irrigation.
 */
public final class BreakPipeUseCase {

    private final PipeRepository pipes;

    public BreakPipeUseCase(PipeRepository pipes) {
        this.pipes = pipes;
    }

    /** @return vrai si un tuyau etait bien present */
    public boolean execute(BlockPos pos) {
        if (!pipes.has(pos)) {
            return false;
        }
        pipes.remove(pos);
        return true;
    }
}
