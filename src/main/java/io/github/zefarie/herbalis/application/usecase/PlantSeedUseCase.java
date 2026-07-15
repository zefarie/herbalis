package io.github.zefarie.herbalis.application.usecase;

import io.github.zefarie.herbalis.application.port.PlantRepository;
import io.github.zefarie.herbalis.application.port.PotRepository;
import io.github.zefarie.herbalis.domain.drug.DrugRegistry;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.plant.Plant;

/**
 * Plantation d'une graine dans un pot.
 */
public final class PlantSeedUseCase {

    public sealed interface Result {
        record Success(Plant plant) implements Result {
        }

        record NoPot() implements Result {
        }

        record AlreadyPlanted() implements Result {
        }

        record UnknownDrug(String drugId) implements Result {
        }
    }

    private final PotRepository pots;
    private final PlantRepository plants;
    private final DrugRegistry drugs;

    public PlantSeedUseCase(PotRepository pots, PlantRepository plants, DrugRegistry drugs) {
        this.pots = pots;
        this.plants = plants;
        this.drugs = drugs;
    }

    public Result execute(BlockPos potPos, String drugId, int seedQuality, long now) {
        if (drugs.byId(drugId).isEmpty()) {
            return new Result.UnknownDrug(drugId);
        }
        if (!pots.exists(potPos)) {
            return new Result.NoPot();
        }
        if (plants.at(potPos).isPresent()) {
            return new Result.AlreadyPlanted();
        }
        Plant plant = Plant.plant(drugId, potPos, now, seedQuality);
        plants.put(plant);
        return new Result.Success(plant);
    }
}
