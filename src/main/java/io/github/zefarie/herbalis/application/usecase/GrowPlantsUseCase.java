package io.github.zefarie.herbalis.application.usecase;

import io.github.zefarie.herbalis.application.port.PlantEnvironment;
import io.github.zefarie.herbalis.application.port.PlantRepository;
import io.github.zefarie.herbalis.domain.drug.DrugRegistry;
import io.github.zefarie.herbalis.domain.drug.DrugType;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.plant.GrowthConditions;
import io.github.zefarie.herbalis.domain.plant.GrowthEngine;
import io.github.zefarie.herbalis.domain.plant.Plant;
import io.github.zefarie.herbalis.domain.plant.PlantEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Tick global de croissance : avance toutes les plantes dont le chunk est
 * charge et collecte les evenements pour que l'infrastructure produise
 * les retours visuels et sonores.
 */
public final class GrowPlantsUseCase {

    /** Evenements d'une plante lors d'un tick. */
    public record PlantTickReport(Plant plant, List<PlantEvent> events) {
    }

    private final PlantRepository plants;
    private final DrugRegistry drugs;
    private final PlantEnvironment environment;

    private long lastTickAt;

    public GrowPlantsUseCase(PlantRepository plants, DrugRegistry drugs,
                             PlantEnvironment environment, long now) {
        this.plants = plants;
        this.drugs = drugs;
        this.environment = environment;
        this.lastTickAt = now;
    }

    /**
     * Avance toutes les plantes chargees jusqu'a {@code now}.
     *
     * @return les rapports des plantes ayant produit au moins un evenement
     */
    public List<PlantTickReport> tick(long now) {
        long delta = now - lastTickAt;
        lastTickAt = now;
        if (delta <= 0) {
            return List.of();
        }

        List<PlantTickReport> reports = new ArrayList<>();
        for (Plant plant : List.copyOf(plants.all())) {
            BlockPos pos = plant.pos();
            if (!environment.isLoaded(pos)) {
                continue;
            }
            DrugType drug = drugs.byId(plant.drugId()).orElse(null);
            if (drug == null) {
                continue;
            }
            GrowthConditions conditions = new GrowthConditions(
                    environment.lightLevel(pos), delta);
            GrowthEngine.GrowthTick result = GrowthEngine.tick(plant, drug, conditions);
            plants.put(result.plant());
            if (result.hasEvents()) {
                reports.add(new PlantTickReport(result.plant(), result.events()));
            }
        }
        return reports;
    }
}
