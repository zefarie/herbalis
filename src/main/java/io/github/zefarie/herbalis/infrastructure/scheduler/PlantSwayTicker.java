package io.github.zefarie.herbalis.infrastructure.scheduler;

import io.github.zefarie.herbalis.application.port.PlantRepository;
import io.github.zefarie.herbalis.domain.drug.DrugRegistry;
import io.github.zefarie.herbalis.domain.drug.DrugType;
import io.github.zefarie.herbalis.domain.plant.GrowthEngine;
import io.github.zefarie.herbalis.domain.plant.Plant;
import io.github.zefarie.herbalis.domain.plant.PlantState;
import io.github.zefarie.herbalis.infrastructure.config.HerbalisConfig;
import io.github.zefarie.herbalis.infrastructure.fx.Fx;
import io.github.zefarie.herbalis.infrastructure.render.DisplayRenderer;
import io.github.zefarie.herbalis.infrastructure.render.PlantVisuals;
import io.github.zefarie.herbalis.infrastructure.render.PosCodec;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Optional;

/**
 * Animation continue des plantes : oscillation douce (phase propre a
 * chaque plante), croissance en direct via l'echelle interpolee, et
 * scintillement discret des plants en fenetre de recolte optimale.
 */
public final class PlantSwayTicker implements Runnable {

    /** Doit correspondre a la periode de planification (en ticks). */
    public static final long PERIOD_TICKS = 30L;

    private static final float AMPLITUDE_RADIANS = 0.038f; // environ 2.2 degres

    private final DisplayRenderer renderer;
    private final PlantRepository plants;
    private final DrugRegistry drugs;
    private final HerbalisConfig config;
    private final Fx fx;
    private int cycle;

    public PlantSwayTicker(DisplayRenderer renderer, PlantRepository plants,
                           DrugRegistry drugs, HerbalisConfig config, Fx fx) {
        this.renderer = renderer;
        this.plants = plants;
        this.drugs = drugs;
        this.config = config;
        this.fx = fx;
    }

    @Override
    public void run() {
        cycle++;
        boolean sway = config.plantSwayEnabled();
        renderer.forEachPlantDisplay((pos, display) -> {
            Plant plant = plants.at(pos).orElse(null);
            DrugType drug = plant == null
                    ? null : drugs.byId(plant.drugId()).orElse(null);
            if (plant == null || drug == null || plant.isDead()) {
                return;
            }

            // Croissance continue : l'echelle suit la progression du stage.
            float scale = PlantVisuals.scaleOf(plant, drug);

            Quaternionf lean = new Quaternionf();
            if (sway && plant.state() != PlantState.WITHERED) {
                int hash = (pos.x() * 73856093) ^ (pos.z() * 19349663)
                        ^ (pos.y() * 83492791);
                double phase = cycle * 0.9 + (hash & 15) * 0.42;
                float leanZ = (float) (AMPLITUDE_RADIANS * Math.sin(phase));
                float leanX = (float) (AMPLITUDE_RADIANS * 0.6
                        * Math.cos(phase * 0.73));
                lean.rotationXYZ(leanX, 0f, leanZ);
            }

            Transformation current = display.getTransformation();
            display.setInterpolationDelay(0);
            display.setInterpolationDuration((int) PERIOD_TICKS);
            display.setTransformation(new Transformation(
                    current.getTranslation(),
                    lean,
                    new Vector3f(scale, scale, scale),
                    new Quaternionf()));

            // Le terreau suit l'etat courant, meme sans evenement.
            renderer.updatePotModel(pos, PlantVisuals.potModel(
                    Optional.of(plant), Optional.of(drug)));

            // Fenetre optimale : la plante scintille, visible de loin.
            if (GrowthEngine.isHarvestable(plant, drug)
                    && drug.harvestWindow().isOptimal(plant.ripenMillis())
                    && plant.state() == PlantState.HEALTHY) {
                PosCodec.corner(pos).ifPresent(fx::harvestSparkle);
            }
        });
    }
}
