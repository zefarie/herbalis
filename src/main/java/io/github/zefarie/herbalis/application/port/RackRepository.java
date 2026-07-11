package io.github.zefarie.herbalis.application.port;

import io.github.zefarie.herbalis.domain.drying.DryingRack;
import io.github.zefarie.herbalis.domain.geo.BlockPos;

import java.util.Collection;
import java.util.Optional;

/**
 * Acces aux racks de sechage poses dans le monde.
 */
public interface RackRepository {

    Optional<DryingRack> at(BlockPos pos);

    void put(DryingRack rack);

    void remove(BlockPos pos);

    Collection<DryingRack> all();
}
