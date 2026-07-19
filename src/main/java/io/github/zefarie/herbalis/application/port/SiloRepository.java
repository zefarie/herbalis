package io.github.zefarie.herbalis.application.port;

import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.irrigation.FertilizerSilo;

import java.util.Collection;
import java.util.Optional;

/**
 * Acces aux silos d'engrais poses dans le monde.
 */
public interface SiloRepository {

    Optional<FertilizerSilo> at(BlockPos pos);

    void put(FertilizerSilo silo);

    void remove(BlockPos pos);

    Collection<FertilizerSilo> all();
}
