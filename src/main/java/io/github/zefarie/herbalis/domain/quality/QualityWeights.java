package io.github.zefarie.herbalis.domain.quality;

/**
 * Ponderation des composantes du score de qualite. La somme des trois
 * poids doit valoir 1.
 *
 * @param hydration  poids de l'hydratation moyenne sur la vie de la plante
 * @param fertilizer poids de l'usage d'engrais
 * @param timing     poids du timing de recolte
 */
public record QualityWeights(
        double hydration,
        double fertilizer,
        double timing
) {

    public static final QualityWeights DEFAULT = new QualityWeights(0.45, 0.30, 0.25);

    public QualityWeights {
        double sum = hydration + fertilizer + timing;
        if (Math.abs(sum - 1.0) > 0.001) {
            throw new IllegalArgumentException(
                    "La somme des poids de qualite doit valoir 1 (actuel : " + sum + ")");
        }
    }
}
