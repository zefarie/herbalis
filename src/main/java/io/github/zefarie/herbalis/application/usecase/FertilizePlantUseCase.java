package io.github.zefarie.herbalis.application.usecase;

import io.github.zefarie.herbalis.application.port.PlantRepository;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.plant.Plant;

/**
 * Application d'un engrais, une seule fois par stage.
 */
public final class FertilizePlantUseCase {

    public sealed interface Result {
        record Success(Plant plant) implements Result {
        }

        record NoPlant() implements Result {
        }

        record PlantDead() implements Result {
        }

        record AlreadyFertilized() implements Result {
        }
    }

    private final PlantRepository plants;

    public FertilizePlantUseCase(PlantRepository plants) {
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
        if (plant.isFertilizedThisStage()) {
            return new Result.AlreadyFertilized();
        }
        Plant fertilized = plant.withFertilizer();
        plants.put(fertilized);
        return new Result.Success(fertilized);
    }
}
