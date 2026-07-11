package io.github.zefarie.herbalis.domain.drug;

/**
 * Parametres d'engrais d'une drogue. L'engrais s'applique une seule fois
 * par stage : il accelere le stage en cours et augmente la qualite potentielle.
 *
 * @param growthBoost  multiplicateur de vitesse applique au stage fertilise
 *                     (0.5 = 50 pour cent plus rapide)
 * @param qualityBonus contribution d'un engrais au score de qualite,
 *                     rapportee au nombre de stages
 */
public record FertilizerProfile(
        double growthBoost,
        double qualityBonus
) {

    public FertilizerProfile {
        if (growthBoost < 0) {
            throw new IllegalArgumentException("growthBoost doit etre positif");
        }
    }

    /** Multiplicateur de croissance effectif quand le stage courant est fertilise. */
    public double speedMultiplier() {
        return 1.0 + growthBoost;
    }
}
