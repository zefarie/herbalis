package io.github.zefarie.herbalis.application.usecase;

import io.github.zefarie.herbalis.application.port.PlantRepository;
import io.github.zefarie.herbalis.application.port.PotRepository;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.plant.Plant;

import java.util.Optional;

/**
 * Casse d'un pot, avec la plante qu'il portait le cas echeant.
 */
public final class BreakPotUseCase {

    /**
     * @param potExisted vrai si un pot etait bien present
     * @param plant      plante qui poussait dessus, si presente
     */
    public record Result(boolean potExisted, Optional<Plant> plant) {
    }

    private final PotRepository pots;
    private final PlantRepository plants;

    public BreakPotUseCase(PotRepository pots, PlantRepository plants) {
        this.pots = pots;
        this.plants = plants;
    }

    public Result execute(BlockPos pos) {
        if (!pots.exists(pos)) {
            return new Result(false, Optional.empty());
        }
        Optional<Plant> plant = plants.at(pos);
        plant.ifPresent(p -> plants.remove(pos));
        pots.remove(pos);
        return new Result(true, plant);
    }
}
