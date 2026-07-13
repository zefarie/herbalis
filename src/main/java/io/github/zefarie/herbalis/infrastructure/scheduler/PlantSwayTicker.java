package io.github.zefarie.herbalis.infrastructure.scheduler;

import io.github.zefarie.herbalis.application.port.PlantRepository;
import io.github.zefarie.herbalis.domain.plant.Plant;
import io.github.zefarie.herbalis.infrastructure.config.HerbalisConfig;
import io.github.zefarie.herbalis.infrastructure.render.DisplayRenderer;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;

/**
 * Oscillation douce des plantes : une legere inclinaison qui derive en
 * continu grace a l'interpolation des Display entities. Chaque plante a
 * sa propre phase (hash de position) pour desynchroniser le champ.
 */
public final class PlantSwayTicker implements Runnable {

    /** Doit correspondre a la periode de planification (en ticks). */
    public static final long PERIOD_TICKS = 30L;

    private static final float AMPLITUDE_RADIANS = 0.038f; // environ 2.2 degres

    private final DisplayRenderer renderer;
    private final PlantRepository plants;
    private final HerbalisConfig config;
    private int cycle;

    public PlantSwayTicker(DisplayRenderer renderer, PlantRepository plants,
                           HerbalisConfig config) {
        this.renderer = renderer;
        this.plants = plants;
        this.config = config;
    }

    @Override
    public void run() {
        if (!config.plantSwayEnabled()) {
            return;
        }
        cycle++;
        renderer.forEachPlantDisplay((pos, display) -> {
            Plant plant = plants.at(pos).orElse(null);
            if (plant == null || plant.isDead()) {
                return;
            }
            int hash = (pos.x() * 73856093) ^ (pos.z() * 19349663) ^ (pos.y() * 83492791);
            double phase = cycle * 0.9 + (hash & 15) * 0.42;
            float leanZ = (float) (AMPLITUDE_RADIANS * Math.sin(phase));
            float leanX = (float) (AMPLITUDE_RADIANS * 0.6 * Math.cos(phase * 0.73));

            Transformation current = display.getTransformation();
            display.setInterpolationDelay(0);
            display.setInterpolationDuration((int) PERIOD_TICKS);
            display.setTransformation(new Transformation(
                    current.getTranslation(),
                    new Quaternionf().rotationXYZ(leanX, 0f, leanZ),
                    current.getScale(),
                    new Quaternionf()));
        });
    }
}
