package io.github.zefarie.herbalis.application.port;

import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.plant.Plant;

import java.util.Collection;
import java.util.Optional;

/**
 * Acces aux plantes. L'implementation tient l'etat en memoire et
 * persiste en arriere-plan.
 */
public interface PlantRepository {

    Optional<Plant> at(BlockPos pos);

    void put(Plant plant);

    void remove(BlockPos pos);

    /** Toutes les plantes connues, chargees ou non. */
    Collection<Plant> all();
}
