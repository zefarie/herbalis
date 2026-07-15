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
 * Tick global de croissance en temps reel. Chaque plante avance de son
 * propre retard (date de dernier tick), si bien qu'un chunk decharge ou
 * un serveur eteint ne fige rien : au retour, la plante rattrape tout
 * le temps ecoule, tranche par tranche pour que les seuils (soif, mort,
 * passages de stage) tombent au bon moment.
 */
public final class GrowPlantsUseCase {

    /**
     * Taille maximale d'une tranche de rattrapage. Assez fine pour que
     * les seuils restent fideles, assez large pour absorber des jours
     * d'absence en quelques centaines d'iterations.
     */
    private static final long SLICE_MILLIS = 5 * 60_000L;

    /** Evenements d'une plante lors d'un tick. */
    public record PlantTickReport(Plant plant, List<PlantEvent> events) {
    }

    private final PlantRepository plants;
    private final DrugRegistry drugs;
    private final PlantEnvironment environment;

    public GrowPlantsUseCase(PlantRepository plants, DrugRegistry drugs,
                             PlantEnvironment environment) {
        this.plants = plants;
        this.drugs = drugs;
        this.environment = environment;
    }

    /**
     * Avance toutes les plantes chargees jusqu'a {@code now}.
     *
     * @return les rapports des plantes ayant produit au moins un evenement
     */
    public List<PlantTickReport> tick(long now) {
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
            long remaining = now - plant.lastTickAt();
            if (remaining <= 0) {
                continue;
            }
            // La lumiere du rattrapage est celle du moment : approximation
            // honnete, une serre eclairee reste eclairee.
            int light = environment.lightLevel(pos);
            List<PlantEvent> events = new ArrayList<>(2);
            Plant current = plant;
            while (remaining > 0 && !current.isDead()) {
                long step = Math.min(remaining, SLICE_MILLIS);
                remaining -= step;
                GrowthEngine.GrowthTick result = GrowthEngine.tick(
                        current, drug, new GrowthConditions(light, step));
                current = result.plant();
                events.addAll(result.events());
            }
            current = current.withLastTickAt(now);
            plants.put(current);
            if (!events.isEmpty()) {
                reports.add(new PlantTickReport(current, events));
            }
        }
        return reports;
    }
}
