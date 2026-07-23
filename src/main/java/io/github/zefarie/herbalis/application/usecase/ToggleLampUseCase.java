package io.github.zefarie.herbalis.application.usecase;

import io.github.zefarie.herbalis.application.port.LampRepository;
import io.github.zefarie.herbalis.domain.geo.BlockPos;

import java.util.Optional;

/**
 * Allume ou eteint une lampe horticole UV.
 */
public final class ToggleLampUseCase {

    private final LampRepository lamps;

    public ToggleLampUseCase(LampRepository lamps) {
        this.lamps = lamps;
    }

    /** @return le nouvel etat, ou vide si aucune lampe a cette position */
    public Optional<Boolean> execute(BlockPos pos) {
        if (!lamps.has(pos)) {
            return Optional.empty();
        }
        boolean enabled = !lamps.isEnabled(pos);
        lamps.setEnabled(pos, enabled);
        return Optional.of(enabled);
    }
}
