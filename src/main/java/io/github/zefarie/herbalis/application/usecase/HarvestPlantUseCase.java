package io.github.zefarie.herbalis.application.usecase;

import io.github.zefarie.herbalis.application.port.PlantRepository;
import io.github.zefarie.herbalis.domain.drug.DrugRegistry;
import io.github.zefarie.herbalis.domain.drug.DrugType;
import io.github.zefarie.herbalis.domain.drug.ToppingProfile;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.plant.GrowthEngine;
import io.github.zefarie.herbalis.domain.plant.Plant;
import io.github.zefarie.herbalis.domain.quality.Quality;
import io.github.zefarie.herbalis.domain.quality.QualityCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Recolte d'une plante au stade final. La plante disparait, le pot reste.
 * La recolte rend aussi 2 ou 3 graines dont la qualite herite de celle
 * de la plante mere (genetique), et une taille reussie bonifie le
 * rendement.
 */
public final class HarvestPlantUseCase {

    public sealed interface Result {
        /**
         * @param quality qualite des tetes recoltees
         * @param yield   nombre de tetes (bonus de taille inclus)
         * @param optimal vrai si la recolte a eu lieu dans la fenetre optimale
         * @param seeds   qualites des graines rendues, une entree par graine
         */
        record Success(String drugId, Quality quality, int yield, boolean optimal,
                       List<Quality> seeds) implements Result {
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
    private final RandomGenerator random;

    public HarvestPlantUseCase(PlantRepository plants, DrugRegistry drugs,
                               RandomGenerator random) {
        this.plants = plants;
        this.drugs = drugs;
        this.random = random;
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
        if (plant.topping() > 0) {
            ToppingProfile topping = drug.topping();
            yield += topping.bonusYieldMin() + random.nextInt(
                    topping.bonusYieldMax() - topping.bonusYieldMin() + 1);
        }
        boolean optimal = drug.harvestWindow().isOptimal(plant.ripenMillis());

        plants.remove(pos);
        return new Result.Success(drug.id(), quality, yield, optimal,
                inheritedSeeds(quality));
    }

    /**
     * 2 ou 3 graines heritees : la plupart gardent les etoiles de la
     * mere, certaines derivent d'une etoile vers le haut ou le bas.
     */
    private List<Quality> inheritedSeeds(Quality mother) {
        int count = 2 + random.nextInt(2);
        List<Quality> seeds = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int drift = switch (random.nextInt(4)) {
                case 0 -> -1;
                case 1 -> 1;
                default -> 0;
            };
            seeds.add(Quality.of(mother.stars() + drift));
        }
        return seeds;
    }
}
