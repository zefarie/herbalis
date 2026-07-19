package io.github.zefarie.herbalis.application.usecase;

import io.github.zefarie.herbalis.application.port.SiloRepository;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.irrigation.FertilizerSilo;

import java.util.Optional;

/**
 * Pose d'un silo d'engrais, vide.
 */
public final class PlaceSiloUseCase {

    private final SiloRepository silos;

    public PlaceSiloUseCase(SiloRepository silos) {
        this.silos = silos;
    }

    /** @return le silo cree, ou vide si la position est occupee */
    public Optional<FertilizerSilo> execute(BlockPos pos) {
        if (silos.at(pos).isPresent()) {
            return Optional.empty();
        }
        FertilizerSilo silo = FertilizerSilo.empty(pos);
        silos.put(silo);
        return Optional.of(silo);
    }
}
