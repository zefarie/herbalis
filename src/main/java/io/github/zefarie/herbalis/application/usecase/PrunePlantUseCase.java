package io.github.zefarie.herbalis.application.usecase;

import io.github.zefarie.herbalis.application.port.PlantRepository;
import io.github.zefarie.herbalis.domain.drug.DrugRegistry;
import io.github.zefarie.herbalis.domain.drug.DrugType;
import io.github.zefarie.herbalis.domain.drug.ToppingProfile;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.plant.Plant;

/**
 * Taille (topping) d'une plante au secateur. Bien placee, la coupe
 * augmente le rendement final au prix d'un peu de progression de stage.
 * Mal placee, elle abime la plante et coutera des etoiles.
 */
public final class PrunePlantUseCase {

    public sealed interface Result {
        /** Taille dans la fenetre : rendement bonifie. */
        record Topped(Plant plant) implements Result {
        }

        /** Taille hors fenetre : la plante est abimee. */
        record Missed(Plant plant) implements Result {
        }

        record AlreadyTopped() implements Result {
        }

        record NoPlant() implements Result {
        }

        record PlantDead() implements Result {
        }
    }

    private final PlantRepository plants;
    private final DrugRegistry drugs;

    public PrunePlantUseCase(PlantRepository plants, DrugRegistry drugs) {
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
        if (plant.isToppingAttempted()) {
            return new Result.AlreadyTopped();
        }
        DrugType drug = drugs.byId(plant.drugId()).orElse(null);
        if (drug == null) {
            return new Result.NoPlant();
        }

        ToppingProfile topping = drug.topping();
        long stageDuration = Math.max(1,
                drug.growth().durationOf(plant.stage()).toMillis());
        double progress = plant.stageGrowthMillis() / (double) stageDuration;

        if (topping.isWindowOpen(plant.stage(), progress)) {
            Plant updated = plant.topped(
                    plant.stageGrowthMillis() - topping.setback().toMillis());
            plants.put(updated);
            return new Result.Topped(updated);
        }
        Plant updated = plant.toppingMissed();
        plants.put(updated);
        return new Result.Missed(updated);
    }
}
