package io.github.zefarie.herbalis.infrastructure.render;

import io.github.zefarie.herbalis.domain.drug.DrugType;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.plant.GrowthEngine;
import io.github.zefarie.herbalis.domain.plant.Plant;
import io.github.zefarie.herbalis.domain.plant.PlantState;

import java.util.Optional;

/**
 * Traduit l'etat d'une plante en parametres visuels : modele (stage,
 * fanee, morte, givree en fenetre optimale), echelle continue de
 * croissance, individualite par position (orientation et taille), et
 * modele de pot (le terreau raconte le soin).
 */
public final class PlantVisuals {

    /** Echelle en debut de stage; la plante grandit ensuite jusqu'a 1. */
    private static final float STAGE_START_SCALE = 0.8f;

    /** Amplitude du jitter d'echelle par plante (+/- 5 %). */
    private static final float SIZE_JITTER = 0.05f;

    private PlantVisuals() {
    }

    /**
     * Modele du display de plante. La variante _prime (buds givres de
     * trichomes) s'affiche pendant la fenetre de recolte optimale.
     * drug peut etre null (definition retiree) : pas de givrage.
     */
    public static String plantModel(Plant plant, DrugType drug) {
        String prefix = "plant_" + plant.drugId();
        if (plant.state() == PlantState.DEAD) {
            return prefix + "_dead";
        }
        if (plant.state() == PlantState.WITHERED && plant.stage() >= 2) {
            return prefix + "_stage_" + plant.stage() + "_dry";
        }
        if (drug != null && plant.state() == PlantState.HEALTHY
                && GrowthEngine.isHarvestable(plant, drug)
                && drug.harvestWindow().isOptimal(plant.ripenMillis())) {
            return prefix + "_stage_" + plant.stage() + "_prime";
        }
        return prefix + "_stage_" + plant.stage();
    }

    /**
     * Echelle du display de plante : progresse continument au sein du
     * stage courant, modulee par le jitter propre a la position pour
     * que deux plants voisins n'aient pas la meme taille.
     */
    public static float scaleOf(Plant plant, DrugType drug) {
        float jitter = 1.0f + SIZE_JITTER * spread(hash(plant.pos()));
        if (drug.growth().isFinalStage(plant.stage())) {
            return jitter;
        }
        long total = Math.max(1, drug.growth().durationOf(plant.stage()).toMillis());
        double progress = Math.clamp(
                plant.stageGrowthMillis() / (double) total, 0.0, 1.0);
        return (float) (STAGE_START_SCALE
                + (1.0 - STAGE_START_SCALE) * progress) * jitter;
    }

    /** Orientation propre a chaque position : les plants ne sont pas des clones. */
    public static float yawOf(BlockPos pos) {
        return Math.floorMod(hash(pos), 360);
    }

    /** Hash de position, aussi utilise pour dephaser les animations. */
    public static int hash(BlockPos pos) {
        return (pos.x() * 73856093) ^ (pos.z() * 19349663) ^ (pos.y() * 83492791);
    }

    /** Etale un hash sur [-1, 1]. */
    private static float spread(int hash) {
        return ((hash >>> 8 & 255) - 127.5f) / 127.5f;
    }

    /**
     * Modele du pot selon l'etat de la plante : terreau humide par
     * defaut, pale et craquele a sec, mouchete apres engrais.
     */
    public static String potModel(Optional<Plant> plant, Optional<DrugType> drug) {
        if (plant.isEmpty() || drug.isEmpty()) {
            return "pot";
        }
        Plant p = plant.get();
        if (p.state() == PlantState.DEAD || p.state() == PlantState.WITHERED
                || p.hydration() <= drug.get().hydration().thirstyThreshold()) {
            return "pot_dry";
        }
        if (p.isFertilizedThisStage()) {
            return "pot_fert";
        }
        return "pot";
    }
}
