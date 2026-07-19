package io.github.zefarie.herbalis.application.usecase;

import io.github.zefarie.herbalis.application.port.TankRepository;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.irrigation.WaterTank;

import java.util.Optional;

/**
 * Casse d'un caisson d'eau : l'eau restante est perdue.
 */
public final class BreakTankUseCase {

    private final TankRepository tanks;

    public BreakTankUseCase(TankRepository tanks) {
        this.tanks = tanks;
    }

    /** @return le caisson casse, pour rendre l'item de la bonne taille */
    public Optional<WaterTank> execute(BlockPos pos) {
        Optional<WaterTank> tank = tanks.at(pos);
        tank.ifPresent(t -> tanks.remove(pos));
        return tank;
    }
}
