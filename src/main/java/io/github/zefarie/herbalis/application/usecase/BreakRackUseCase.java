package io.github.zefarie.herbalis.application.usecase;

import io.github.zefarie.herbalis.application.port.RackRepository;
import io.github.zefarie.herbalis.domain.drying.DryingRack;
import io.github.zefarie.herbalis.domain.geo.BlockPos;

import java.util.Optional;

/**
 * Casse d'un rack de sechage. Les tetes encore dessus sont rendues
 * fraiches (le sechage en cours est perdu).
 */
public final class BreakRackUseCase {

    private final RackRepository racks;

    public BreakRackUseCase(RackRepository racks) {
        this.racks = racks;
    }

    /** @return le rack detruit avec son contenu, si present */
    public Optional<DryingRack> execute(BlockPos pos) {
        Optional<DryingRack> rack = racks.at(pos);
        rack.ifPresent(r -> racks.remove(pos));
        return rack;
    }
}
