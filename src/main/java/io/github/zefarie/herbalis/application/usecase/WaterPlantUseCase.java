package io.github.zefarie.herbalis.application.usecase;

import io.github.zefarie.herbalis.application.port.PlantRepository;
import io.github.zefarie.herbalis.domain.drug.DrugRegistry;
import io.github.zefarie.herbalis.domain.drug.DrugType;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.plant.Plant;

/**
 * Arrosage d'une plante avec l'arrosoir.
 */
public final class WaterPlantUseCase {

    public sealed interface Result {
        /** Arrosage effectue. {@code plant} est l'etat apres arrosage. */
        record Success(Plant plant) implements Result {
        }

        record NoPlant() implements Result {
        }

        record PlantDead() implements Result {
        }

        record AlreadyMoist() implements Result {
        }
    }

    private final PlantRepository plants;
    private final DrugRegistry drugs;

    public WaterPlantUseCase(PlantRepository plants, DrugRegistry drugs) {
        this.plants = plants;
        this.drugs = drugs;
    }

    public Result execute(BlockPos pos) {
        Plant plant = plants.at(pos).orElse(null);
        if (plant == null) {
            return new Result.NoPlant();
        }
        if (plant.isDead()) {
            return new Result.PlantDead();
        }
        if (plant.hydration() >= 99.0) {
            return new Result.AlreadyMoist();
        }
        DrugType drug = drugs.byId(plant.drugId()).orElseThrow();
        Plant watered = plant.withHydration(
                plant.hydration() + drug.hydration().waterRestore());
        plants.put(watered);
        return new Result.Success(watered);
    }
}
