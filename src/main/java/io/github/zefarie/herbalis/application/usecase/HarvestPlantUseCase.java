package io.github.zefarie.herbalis.application.usecase;

import io.github.zefarie.herbalis.application.port.PlantRepository;
import io.github.zefarie.herbalis.domain.drug.DrugRegistry;
import io.github.zefarie.herbalis.domain.drug.DrugType;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.plant.GrowthEngine;
import io.github.zefarie.herbalis.domain.plant.Plant;
import io.github.zefarie.herbalis.domain.quality.Quality;
import io.github.zefarie.herbalis.domain.quality.QualityCalculator;

/**
 * Recolte d'une plante au stade final. La plante disparait, le pot reste.
 */
public final class HarvestPlantUseCase {

    public sealed interface Result {
        /**
         * @param quality qualite des tetes recoltees
         * @param yield   nombre de tetes
         * @param optimal vrai si la recolte a eu lieu dans la fenetre optimale
         */
        record Success(String drugId, Quality quality, int yield, boolean optimal)
                implements Result {
        }

        record NotReady(int stage, int stageCount) implements Result {
        }

        record PlantDead() implements Result {
        }

        record NoPlant() implements Result {
        }
    }

    private final PlantRepository plants;
    private final DrugRegistry drugs;

    public HarvestPlantUseCase(PlantRepository plants, DrugRegistry drugs) {
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
        DrugType drug = drugs.byId(plant.drugId()).orElseThrow();
        if (!GrowthEngine.isHarvestable(plant, drug)) {
            return new Result.NotReady(plant.stage(), drug.growth().stageCount());
        }

        Quality quality = QualityCalculator.harvestQuality(plant, drug);
        int yield = drug.growth().yieldFor(quality.stars());
        boolean optimal = drug.harvestWindow().isOptimal(plant.ripenMillis());

        plants.remove(pos);
        return new Result.Success(drug.id(), quality, yield, optimal);
    }
}
