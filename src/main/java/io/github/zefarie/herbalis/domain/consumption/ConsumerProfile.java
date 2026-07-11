package io.github.zefarie.herbalis.domain.consumption;

import java.util.List;

/**
 * Etat de consommation d'un joueur : tolerance, addiction et historique
 * recent. Les jauges vont de 0 a 100 et decroissent en temps reel via
 * les timestamps, y compris hors ligne.
 *
 * @param tolerance          jauge de tolerance, reduit les effets
 * @param addiction          jauge d'addiction, declenche le manque
 * @param lastUpdatedAt      derniere application de la decroissance (epoch ms)
 * @param lastConsumedAt     derniere consommation (epoch ms, 0 = jamais)
 * @param recentConsumptions timestamps des consommations recentes (fenetre d'abus)
 */
public record ConsumerProfile(
        double tolerance,
        double addiction,
        long lastUpdatedAt,
        long lastConsumedAt,
        List<Long> recentConsumptions
) {

    public ConsumerProfile {
        recentConsumptions = List.copyOf(recentConsumptions);
    }

    public static ConsumerProfile fresh(long now) {
        return new ConsumerProfile(0.0, 0.0, now, 0L, List.of());
    }

    public boolean hasEverConsumed() {
        return lastConsumedAt > 0;
    }
}
