package io.github.zefarie.herbalis.application.usecase;

import io.github.zefarie.herbalis.application.port.TankRepository;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.irrigation.TankSize;
import io.github.zefarie.herbalis.domain.irrigation.WaterTank;

import java.util.Optional;

/**
 * Pose d'un caisson d'eau, vide.
 */
public final class PlaceTankUseCase {

    private final TankRepository tanks;

    public PlaceTankUseCase(TankRepository tanks) {
        this.tanks = tanks;
    }

    /** @return le caisson cree, ou vide si la position est occupee */
    public Optional<WaterTank> execute(BlockPos pos, TankSize size) {
        if (tanks.at(pos).isPresent()) {
            return Optional.empty();
        }
        WaterTank tank = WaterTank.empty(pos, size);
        tanks.put(tank);
        return Optional.of(tank);
    }
}
