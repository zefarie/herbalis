package io.github.zefarie.herbalis.domain.drug;

import java.time.Duration;
import java.util.List;

/**
 * Parametres de taille (topping) d'une drogue. Tailler dans la fenetre
 * augmente le rendement mais fait perdre de la progression de stage;
 * tailler au mauvais moment abime la plante et coute des etoiles.
 *
 * @param stages         stages ou la taille est possible (1-indexes)
 * @param windowStart    debut de la fenetre, fraction de progression du stage
 * @param windowEnd      fin de la fenetre, fraction de progression du stage
 * @param setback        progression de stage perdue par la coupe
 * @param bonusYieldMin  tetes supplementaires minimum a la recolte
 * @param bonusYieldMax  tetes supplementaires maximum a la recolte
 * @param missMalusStars etoiles perdues si la taille est ratee
 */
public record ToppingProfile(
        List<Integer> stages,
        double windowStart,
        double windowEnd,
        Duration setback,
        int bonusYieldMin,
        int bonusYieldMax,
        int missMalusStars
) {

    public static final ToppingProfile DEFAULT = new ToppingProfile(
            List.of(2, 3), 0.30, 0.60, Duration.ofMinutes(2), 1, 2, 1);

    public ToppingProfile {
        stages = List.copyOf(stages);
        if (windowStart < 0 || windowEnd > 1 || windowStart >= windowEnd) {
            throw new IllegalArgumentException(
                    "Fenetre de taille invalide : " + windowStart + " - " + windowEnd);
        }
        if (bonusYieldMin > bonusYieldMax) {
            throw new IllegalArgumentException("bonus-tetes-min > bonus-tetes-max");
        }
    }

    /** Vrai si la taille est bien placee pour ce stage et cette progression. */
    public boolean isWindowOpen(int stage, double stageProgress) {
        return stages.contains(stage)
                && stageProgress >= windowStart && stageProgress <= windowEnd;
    }
}
