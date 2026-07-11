package io.github.zefarie.herbalis.domain.drug;

import java.time.Duration;

/**
 * Fenetre de recolte au stage final. Pendant {@code optimalDuration},
 * la recolte donne le meilleur score de timing. Ensuite, le score decroit
 * lineairement jusqu'a zero sur {@code decayDuration}.
 *
 * @param optimalDuration duree de la fenetre optimale apres l'entree en floraison
 * @param decayDuration   duree de decroissance de la qualite apres la fenetre
 */
public record HarvestWindow(
        Duration optimalDuration,
        Duration decayDuration
) {

    /**
     * Score de timing entre 0 et 1 selon le temps passe au stade final.
     */
    public double timingScore(long ripenMillis) {
        long optimal = optimalDuration.toMillis();
        if (ripenMillis <= optimal) {
            return 1.0;
        }
        long overdue = ripenMillis - optimal;
        long decay = Math.max(1, decayDuration.toMillis());
        return Math.max(0.0, 1.0 - (double) overdue / decay);
    }

    /** Vrai si la fenetre optimale est encore ouverte. */
    public boolean isOptimal(long ripenMillis) {
        return ripenMillis <= optimalDuration.toMillis();
    }
}
