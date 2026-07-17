package io.github.zefarie.herbalis.infrastructure.scheduler;

import io.github.zefarie.herbalis.application.port.PlantRepository;
import io.github.zefarie.herbalis.application.port.PotRepository;
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
import java.util.concurrent.ThreadLocalRandom;

/**
 * Animation continue des plantes : oscillation douce et respiration
 * (phases propres a chaque plante), croissance en direct via l'echelle
 * interpolee, givrage des buds en fenetre optimale (swap de modele),
 * scintillement discret et rares feuilles qui se detachent.
 */
public final class PlantSwayTicker implements Runnable {

    /** Doit correspondre a la periode de planification (en ticks). */
    public static final long PERIOD_TICKS = 30L;

    private static final float AMPLITUDE_RADIANS = 0.038f; // environ 2.2 degres

    /** Respiration : micro pulsation d'echelle (+/- 1.5 %). */
    private static final float BREATH_AMPLITUDE = 0.015f;

    /** Une chance sur N par cycle qu'une feuille se detache. */
    private static final int LEAF_FALL_ODDS = 30;

    private final DisplayRenderer renderer;
    private final PlantRepository plants;
    private final PotRepository pots;
    private final DrugRegistry drugs;
    private final HerbalisConfig config;
    private final Fx fx;
    private int cycle;

    public PlantSwayTicker(DisplayRenderer renderer, PlantRepository plants,
                           PotRepository pots, DrugRegistry drugs,
                           HerbalisConfig config, Fx fx) {
        this.renderer = renderer;
        this.plants = plants;
        this.pots = pots;
        this.drugs = drugs;
        this.config = config;
        this.fx = fx;
    }

    @Override
    public void run() {
        cycle++;
        boolean animate = config.plantSwayEnabled();
        renderer.forEachPlantDisplay((pos, display) -> {
            Plant plant = plants.at(pos).orElse(null);
            DrugType drug = plant == null
                    ? null : drugs.byId(plant.drugId()).orElse(null);
            if (plant == null || drug == null || plant.isDead()) {
                return;
            }

            // Le modele suit l'etat courant : les buds se givrent en
            // fenetre optimale, et redeviennent normaux apres.
            renderer.updatePlantModel(pos, PlantVisuals.plantModel(plant, drug));

            // Croissance continue : l'echelle suit la progression du stage.
            float scale = PlantVisuals.scaleOf(plant, drug);

            int hash = PlantVisuals.hash(pos);
            Quaternionf lean = new Quaternionf();
            if (animate && plant.state() != PlantState.WITHERED) {
                double phase = cycle * 0.9 + (hash & 15) * 0.42;
                float leanZ = (float) (AMPLITUDE_RADIANS * Math.sin(phase));
                float leanX = (float) (AMPLITUDE_RADIANS * 0.6
                        * Math.cos(phase * 0.73));
                lean.rotationXYZ(leanX, 0f, leanZ);
                // Respiration : la plante gonfle et degonfle a peine.
                if (plant.state() == PlantState.HEALTHY) {
                    scale *= 1f + BREATH_AMPLITUDE
                            * (float) Math.sin(cycle * 0.55 + (hash & 7));
                }
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
            boolean dripper = pots.hasDripper(pos);
            renderer.updatePotModel(pos, PlantVisuals.potModel(
                    Optional.of(plant), Optional.of(drug), dripper));

            // Fenetre optimale : la plante scintille, visible de loin.
            if (GrowthEngine.isHarvestable(plant, drug)
                    && drug.harvestWindow().isOptimal(plant.ripenMillis())
                    && plant.state() == PlantState.HEALTHY) {
                PosCodec.corner(pos).ifPresent(fx::harvestSparkle);
            }

            // Une feuille se detache parfois des plants matures sains.
            if (plant.state() == PlantState.HEALTHY
                    && plant.stage() >= drug.growth().stageCount() - 1
                    && ThreadLocalRandom.current().nextInt(LEAF_FALL_ODDS) == 0) {
                PosCodec.corner(pos).ifPresent(fx::leafFall);
            }

            // Plante infestee : moucherons visibles de loin.
            if (plant.isInfested()) {
                PosCodec.corner(pos).ifPresent(fx::pestAmbient);
            }

            // Goutte-a-goutte : une goutte perle du tuyau de temps en temps.
            if (dripper && (cycle + (hash & 3)) % 4 == 0) {
                PosCodec.corner(pos).ifPresent(fx::dripAmbient);
            }
        });
    }
}
