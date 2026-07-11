package io.github.zefarie.herbalis.domain.drying;

import io.github.zefarie.herbalis.domain.quality.Quality;

import java.time.Duration;

/**
 * Une tete en cours de sechage sur un rack. Le sechage repose sur des
 * timestamps epoch : il continue serveur eteint et reprend tel quel au
 * redemarrage.
 *
 * @param quality   qualite de la tete deposee
 * @param startedAt debut du sechage (epoch ms)
 */
public record DryingSlot(Quality quality, long startedAt) {

    /** Fraction de sechage accomplie, peut depasser 1. */
    public double completionRatio(long now, Duration dryingDuration) {
        long total = Math.max(1, dryingDuration.toMillis());
        return (now - startedAt) / (double) total;
    }

    public boolean isDry(long now, Duration dryingDuration) {
        return completionRatio(now, dryingDuration) >= 1.0;
    }
}
