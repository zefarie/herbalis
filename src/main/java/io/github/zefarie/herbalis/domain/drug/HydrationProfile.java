package io.github.zefarie.herbalis.domain.drug;

import java.time.Duration;

/**
 * Parametres d'hydratation d'une drogue.
 *
 * <p>L'hydratation est une jauge de 0 a 100. Sous {@code thirstyThreshold},
 * la croissance est figee. A zero, un compteur de secheresse demarre :
 * apres {@code witherDelay} la plante jaunit, apres {@code deathDelay}
 * elle meurt.</p>
 *
 * @param decayPerMinute   points d'hydratation perdus par minute
 * @param waterRestore     points rendus par arrosage
 * @param thirstyThreshold seuil sous lequel la croissance est figee
 * @param witherDelay      duree a sec avant degradation visuelle (jaunissement)
 * @param deathDelay       duree a sec avant la mort de la plante
 */
public record HydrationProfile(
        double decayPerMinute,
        double waterRestore,
        double thirstyThreshold,
        Duration witherDelay,
        Duration deathDelay
) {

    public HydrationProfile {
        if (decayPerMinute < 0 || waterRestore <= 0) {
            throw new IllegalArgumentException("Parametres d'hydratation invalides");
        }
        if (witherDelay.compareTo(deathDelay) > 0) {
            throw new IllegalArgumentException("witherDelay doit preceder deathDelay");
        }
    }
}
