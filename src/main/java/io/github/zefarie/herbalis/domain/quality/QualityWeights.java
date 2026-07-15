package io.github.zefarie.herbalis.domain.quality;

/**
 * Ponderation des composantes du score de qualite. La somme des quatre
 * poids doit valoir 1.
 *
 * @param hydration  poids de l'hydratation moyenne sur la vie de la plante
 * @param fertilizer poids de l'usage d'engrais
 * @param timing     poids du timing de recolte
 * @param genetics   poids de la qualite de la graine plantee
 */
public record QualityWeights(
        double hydration,
        double fertilizer,
        double timing,
        double genetics
) {

    public static final QualityWeights DEFAULT =
            new QualityWeights(0.40, 0.25, 0.20, 0.15);

    public QualityWeights {
        double sum = hydration + fertilizer + timing + genetics;
        if (Math.abs(sum - 1.0) > 0.001) {
            throw new IllegalArgumentException(
                    "La somme des poids de qualite doit valoir 1 (actuel : " + sum + ")");
        }
    }
}
