package io.github.zefarie.herbalis.application.usecase;

import io.github.zefarie.herbalis.application.port.PlantRepository;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.plant.Plant;

/**
 * Traitement d'une plante infestee au pulverisateur : les nuisibles
 * disparaissent, mais les degats deja subis restent acquis.
 */
public final class TreatPlantUseCase {

    public sealed interface Result {
        /** Infestation eliminee. */
        record Treated(Plant plant) implements Result {
        }

        record NotInfested() implements Result {
        }

        record NoPlant() implements Result {
        }

        record PlantDead() implements Result {
        }
    }

    private final PlantRepository plants;

    public TreatPlantUseCase(PlantRepository plants) {
        this.plants = plants;
    }

    public Result execute(BlockPos pos) {
        Plant plant = plants.at(pos).orElse(null);
        if (plant == null) {
            return new Result.NoPlant();
        }
        if (plant.isDead()) {
            return new Result.PlantDead();
        }
        if (!plant.isInfested()) {
            return new Result.NotInfested();
        }
        Plant treated = plant.pestTreated();
        plants.put(treated);
        return new Result.Treated(treated);
    }
}
