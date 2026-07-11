package io.github.zefarie.herbalis.domain.quality;

import io.github.zefarie.herbalis.domain.drug.DrugType;
import io.github.zefarie.herbalis.domain.plant.Plant;

/**
 * Calcul de la qualite d'une recolte et des malus post-recolte.
 * Logique pure, entierement testable hors serveur.
 */
public final class QualityCalculator {

    private QualityCalculator() {
    }

    /**
     * Qualite d'une plante recoltee maintenant. Combine l'hydratation
     * moyenne sur la vie de la plante, l'usage d'engrais et le timing de
     * recolte selon les poids du type de drogue.
     */
    public static Quality harvestQuality(Plant plant, DrugType drug) {
        QualityWeights weights = drug.qualityWeights();

        double hydrationScore = plant.averageHydration() / 100.0;

        int maxFertilizer = drug.growth().stageCount();
        double fertilizerScore = Math.clamp(
                plant.fertilizerUses() / (double) maxFertilizer, 0.0, 1.0);

        double timingScore = drug.harvestWindow().timingScore(plant.ripenMillis());

        double score = weights.hydration() * hydrationScore
                + weights.fertilizer() * fertilizerScore
                + weights.timing() * timingScore;

        return Quality.fromScore(score);
    }

    /**
     * Qualite apres sechage. {@code completionRatio} est la fraction de
     * sechage accomplie (1 ou plus = sechage complet, aucun malus).
     * Un retrait anticipe coute jusqu'a 4 etoiles, proportionnellement.
     */
    public static Quality afterDrying(Quality quality, double completionRatio) {
        if (completionRatio >= 1.0) {
            return quality;
        }
        double missing = Math.clamp(1.0 - completionRatio, 0.0, 1.0);
        int malus = (int) Math.round(missing * (Quality.MAX - Quality.MIN));
        return Quality.of(quality.stars() - malus);
    }
}
