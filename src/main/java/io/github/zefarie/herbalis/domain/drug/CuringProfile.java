package io.github.zefarie.herbalis.domain.drug;

import java.time.Duration;

/**
 * Parametres d'affinage (curing) en jarre. Comme le sechage, l'affinage
 * repose sur des timestamps et continue serveur eteint.
 *
 * @param duration   duree d'affinage complet d'une tete
 * @param moldDelay  delai apres affinage avant que la jarre ne moisisse
 * @param bonusStars etoiles gagnees par un affinage complet
 * @param capacity   nombre de tetes par jarre
 */
public record CuringProfile(
        Duration duration,
        Duration moldDelay,
        int bonusStars,
        int capacity
) {

    public static final CuringProfile DEFAULT = new CuringProfile(
            Duration.ofMinutes(45), Duration.ofMinutes(90), 1, 6);
}
