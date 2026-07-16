package io.github.zefarie.herbalis.domain.drug;

import java.time.Duration;

/**
 * Nuisibles : une plante peut attraper une infestation qui ralentit sa
 * croissance et, sans traitement au pulverisateur, finit par l'abimer.
 *
 * @param dailyChance chance d'infestation par plante et par jour (0 a 1);
 *                    0 desactive completement les nuisibles
 * @param slowdown    multiplicateur de croissance pendant l'infestation
 * @param damageDelay delai avant que l'infestation n'abime la plante
 * @param damageStars etoiles perdues a la recolte une fois la plante abimee
 */
public record PestProfile(
        double dailyChance,
        double slowdown,
        Duration damageDelay,
        int damageStars
) {

    /** Valeurs livrees avec la weed. */
    public static final PestProfile DEFAULT =
            new PestProfile(0.15, 0.5, Duration.ofHours(12), 1);

    /** Nuisibles desactives : defaut des configs anterieures. */
    public static final PestProfile DISABLED =
            new PestProfile(0.0, 1.0, Duration.ofHours(12), 0);

    public PestProfile {
        if (dailyChance < 0 || dailyChance > 1) {
            throw new IllegalArgumentException(
                    "chance-par-jour doit etre entre 0 et 1");
        }
        if (slowdown < 0 || slowdown > 1) {
            throw new IllegalArgumentException(
                    "ralentissement doit etre entre 0 et 1");
        }
    }

    public boolean isEnabled() {
        return dailyChance > 0;
    }

    /** Probabilite d'infestation sur une duree donnee. */
    public double chanceOver(long deltaMillis) {
        return dailyChance * deltaMillis / 86_400_000.0;
    }
}
