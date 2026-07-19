package io.github.zefarie.herbalis.application.port;

import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.irrigation.WaterTank;

import java.util.Collection;
import java.util.Optional;

/**
 * Acces aux caissons d'eau poses dans le monde.
 */
public interface TankRepository {

    Optional<WaterTank> at(BlockPos pos);

    void put(WaterTank tank);

    void remove(BlockPos pos);

    Collection<WaterTank> all();
}
