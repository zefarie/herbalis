package io.github.zefarie.herbalis.domain.drug;

import java.time.Duration;
import java.util.List;

/**
 * Parametres de croissance d'une drogue.
 *
 * @param stageCount     nombre de stages de croissance (4 pour la weed)
 * @param stageDurations duree de chaque stage, index 0 = stage 1
 * @param minLight       niveau de lumiere minimum pour que la plante pousse
 * @param yieldMin       nombre de tetes recoltees au minimum (qualite 1)
 * @param yieldMax       nombre de tetes recoltees au maximum (qualite 5)
 */
public record GrowthProfile(
        int stageCount,
        List<Duration> stageDurations,
        int minLight,
        int yieldMin,
        int yieldMax
) {

    public GrowthProfile {
        if (stageCount < 2) {
            throw new IllegalArgumentException("Un type de drogue doit avoir au moins 2 stages");
        }
        if (stageDurations.size() != stageCount) {
            throw new IllegalArgumentException(
                    "stageDurations doit contenir exactement " + stageCount + " durees");
        }
        stageDurations = List.copyOf(stageDurations);
    }

    /** Duree du stage donne (1-indexe). */
    public Duration durationOf(int stage) {
        return stageDurations.get(stage - 1);
    }

    public boolean isFinalStage(int stage) {
        return stage >= stageCount;
    }

    /** Rendement pour une qualite donnee, interpolation lineaire entre min et max. */
    public int yieldFor(int stars) {
        double t = (stars - 1) / 4.0;
        return (int) Math.round(yieldMin + (yieldMax - yieldMin) * t);
    }
}
