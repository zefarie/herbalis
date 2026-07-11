package io.github.zefarie.herbalis.application.usecase;

import io.github.zefarie.herbalis.application.port.PlantRepository;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.plant.Plant;

import java.util.Optional;

/**
 * Destruction d'une plante (arrachage, explosion) sans recolte.
 * Le pot reste en place.
 */
public final class BreakPlantUseCase {

    private final PlantRepository plants;

    public BreakPlantUseCase(PlantRepository plants) {
        this.plants = plants;
    }

    /** @return la plante detruite, si elle existait */
    public Optional<Plant> execute(BlockPos pos) {
        Optional<Plant> plant = plants.at(pos);
        plant.ifPresent(p -> plants.remove(pos));
        return plant;
    }
}
