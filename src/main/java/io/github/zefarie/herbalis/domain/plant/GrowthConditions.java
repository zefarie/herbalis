package io.github.zefarie.herbalis.domain.plant;

/**
 * Photo de l'environnement d'une plante au moment d'un tick.
 *
 * @param lightLevel      niveau de lumiere au bloc de la plante (0 a 15)
 * @param deltaMillis     temps ecoule depuis le tick precedent
 * @param pestRoll        tirage aleatoire 0 a 1 pour l'infestation (1 = jamais)
 * @param hydrationFactor multiplicateur de la perte d'hydratation
 *                        (0.5 avec un goutte-a-goutte)
 */
public record GrowthConditions(int lightLevel, long deltaMillis,
                               double pestRoll, double hydrationFactor) {

    /** Conditions neutres : pas de tirage de nuisibles, perte d'eau pleine. */
    public GrowthConditions(int lightLevel, long deltaMillis) {
        this(lightLevel, deltaMillis, 1.0, 1.0);
    }
}
