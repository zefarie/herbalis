package io.github.zefarie.herbalis.infrastructure.scheduler;

import io.github.zefarie.herbalis.application.usecase.GrowPlantsUseCase;
import io.github.zefarie.herbalis.domain.drug.DrugRegistry;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.plant.Plant;
import io.github.zefarie.herbalis.domain.plant.PlantEvent;
import io.github.zefarie.herbalis.infrastructure.fx.Fx;
import io.github.zefarie.herbalis.infrastructure.render.DisplayRenderer;
import io.github.zefarie.herbalis.infrastructure.render.PosCodec;
import org.bukkit.Location;

/**
 * Tick global de croissance : une seule tache pour toutes les plantes.
 * Traduit les evenements du domaine en modeles, particules et sons.
 */
public final class GrowthTicker implements Runnable {

    private final GrowPlantsUseCase growPlants;
    private final DrugRegistry drugs;
    private final DisplayRenderer renderer;
    private final Fx fx;

    public GrowthTicker(GrowPlantsUseCase growPlants, DrugRegistry drugs,
                        DisplayRenderer renderer, Fx fx) {
        this.growPlants = growPlants;
        this.drugs = drugs;
        this.renderer = renderer;
        this.fx = fx;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        for (GrowPlantsUseCase.PlantTickReport report : growPlants.tick(now)) {
            Plant plant = report.plant();
            BlockPos pos = plant.pos();
            Location loc = PosCodec.corner(pos).orElse(null);
            if (loc == null) {
                continue;
            }
            for (PlantEvent event : report.events()) {
                switch (event) {
                    case PlantEvent.StageAdvanced advanced -> {
                        renderer.updatePlant(pos, plant, plant.drugId(), true);
                        if (advanced.isFinal()) {
                            fx.bloomed(loc);
                        } else {
                            fx.stageUp(loc);
                        }
                    }
                    case PlantEvent.Withered ignored -> {
                        renderer.updatePlant(pos, plant, plant.drugId(), false);
                        fx.withered(loc);
                    }
                    case PlantEvent.Recovered ignored -> {
                        renderer.updatePlant(pos, plant, plant.drugId(), false);
                        fx.recovered(loc);
                    }
                    case PlantEvent.Died ignored -> {
                        renderer.updatePlant(pos, plant, plant.drugId(), false);
                        fx.died(loc);
                    }
                    case PlantEvent.HarvestWindowClosed ignored -> {
                        // Silencieux : le HUD signale la fenetre depassee.
                    }
                }
            }
        }
    }
}
