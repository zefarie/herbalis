package io.github.zefarie.herbalis.application.usecase;

import io.github.zefarie.herbalis.application.port.RackRepository;
import io.github.zefarie.herbalis.domain.drying.DryingRack;
import io.github.zefarie.herbalis.domain.geo.BlockPos;

import java.util.Optional;

/**
 * Pose d'un rack de sechage.
 */
public final class PlaceRackUseCase {

    private final RackRepository racks;

    public PlaceRackUseCase(RackRepository racks) {
        this.racks = racks;
    }

    /** @return le rack pose, ou vide si la position est occupee */
    public Optional<DryingRack> execute(BlockPos pos) {
        if (racks.at(pos).isPresent()) {
            return Optional.empty();
        }
        DryingRack rack = DryingRack.empty(pos);
        racks.put(rack);
        return Optional.of(rack);
    }
}
