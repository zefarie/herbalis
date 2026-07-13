package io.github.zefarie.herbalis.infrastructure.render;

import io.github.zefarie.herbalis.domain.drug.DrugType;
import io.github.zefarie.herbalis.domain.plant.Plant;
import io.github.zefarie.herbalis.domain.plant.PlantState;

import java.util.Optional;

/**
 * Traduit l'etat d'une plante en parametres visuels : echelle continue
 * de croissance et modele de pot (le terreau raconte le soin).
 */
public final class PlantVisuals {

    /** Echelle en debut de stage; la plante grandit ensuite jusqu'a 1. */
    private static final float STAGE_START_SCALE = 0.8f;

    private PlantVisuals() {
    }

    /**
     * Echelle du display de plante : progresse continument au sein du
     * stage courant, pour une croissance visible en direct.
     */
    public static float scaleOf(Plant plant, DrugType drug) {
        if (drug.growth().isFinalStage(plant.stage())) {
            return 1.0f;
        }
        long total = Math.max(1, drug.growth().durationOf(plant.stage()).toMillis());
        double progress = Math.clamp(
                plant.stageGrowthMillis() / (double) total, 0.0, 1.0);
        return (float) (STAGE_START_SCALE + (1.0 - STAGE_START_SCALE) * progress);
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
