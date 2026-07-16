package io.github.zefarie.herbalis.domain.quality;

import io.github.zefarie.herbalis.domain.drug.DrugType;
import io.github.zefarie.herbalis.domain.plant.Plant;

/**
 * Calcul de la qualite d'une recolte et des malus et bonus post-recolte.
 * Logique pure, entierement testable hors serveur.
 */
public final class QualityCalculator {

    private QualityCalculator() {
    }

    /**
     * Qualite d'une plante recoltee maintenant. Combine l'hydratation
     * moyenne sur la vie de la plante, l'usage d'engrais, le timing de
     * recolte et la genetique de la graine selon les poids du type de
     * drogue. Une taille ratee ou une infestation non traitee coutent
     * des etoiles.
     */
    public static Quality harvestQuality(Plant plant, DrugType drug) {
        QualityWeights weights = drug.qualityWeights();

        double hydrationScore = plant.averageHydration() / 100.0;

        int maxFertilizer = drug.growth().stageCount();
        double fertilizerScore = Math.clamp(
                plant.fertilizerUses() / (double) maxFertilizer, 0.0, 1.0);

        double timingScore = drug.harvestWindow().timingScore(plant.ripenMillis());

        double geneticsScore = (plant.seedQuality() - Quality.MIN)
                / (double) (Quality.MAX - Quality.MIN);

        double score = weights.hydration() * hydrationScore
                + weights.fertilizer() * fertilizerScore
                + weights.timing() * timingScore
                + weights.genetics() * geneticsScore;

        Quality quality = Quality.fromScore(score);
        if (plant.topping() < 0) {
            quality = Quality.of(quality.stars() - drug.topping().missMalusStars());
        }
        if (plant.pestDamage() > 0) {
            quality = Quality.of(quality.stars() - drug.pests().damageStars());
        }
        return quality;
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

    /**
     * Qualite apres passage en jarre de curing : bonus si l'affinage est
     * complet, contenu ruine si la jarre a moisi, inchangee si retiree
     * avant la fin.
     */
    public static Quality afterCuring(Quality quality, boolean cured, boolean moldy,
                                      int bonusStars) {
        if (moldy) {
            return Quality.of(Quality.MIN);
        }
        return cured ? Quality.of(quality.stars() + bonusStars) : quality;
    }
}
