package io.github.zefarie.herbalis.domain.plant;

/**
 * Photo de l'environnement d'une plante au moment d'un tick.
 *
 * @param lightLevel          niveau de lumiere au bloc de la plante (0 a 15)
 * @param deltaMillis         temps ecoule depuis le tick precedent
 * @param pestRoll            tirage aleatoire 0 a 1 pour l'infestation (1 = jamais)
 * @param hydrationFactor     multiplicateur de la perte d'hydratation
 *                            (0.5 avec un goutte-a-goutte)
 * @param waterAvailable      eau du reseau d'irrigation disponible pour ce
 *                            tick, en points d'hydratation (0 = pas relie)
 * @param fertilizerAvailable vrai si un silo relie peut fournir une dose
 */
public record GrowthConditions(int lightLevel, long deltaMillis,
                               double pestRoll, double hydrationFactor,
                               double waterAvailable, boolean fertilizerAvailable) {

    /** Conditions neutres : pas de tirage de nuisibles, perte d'eau pleine. */
    public GrowthConditions(int lightLevel, long deltaMillis) {
        this(lightLevel, deltaMillis, 1.0, 1.0, 0.0, false);
    }

    /** Conditions sans reseau d'irrigation. */
    public GrowthConditions(int lightLevel, long deltaMillis,
                            double pestRoll, double hydrationFactor) {
        this(lightLevel, deltaMillis, pestRoll, hydrationFactor, 0.0, false);
    }
}
