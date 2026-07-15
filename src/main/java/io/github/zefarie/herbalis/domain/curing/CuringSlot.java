package io.github.zefarie.herbalis.domain.curing;

import io.github.zefarie.herbalis.domain.quality.Quality;

import java.time.Duration;

/**
 * Une tete sechee en cours d'affinage dans une jarre.
 *
 * @param quality   qualite de la tete a l'entree en jarre
 * @param startedAt debut de l'affinage (epoch millis)
 */
public record CuringSlot(Quality quality, long startedAt) {

    /** Fraction d'affinage accomplie, peut depasser 1. */
    public double completionRatio(long now, Duration curingDuration) {
        long duration = Math.max(1, curingDuration.toMillis());
        return (now - startedAt) / (double) duration;
    }

    public boolean isCured(long now, Duration curingDuration) {
        return completionRatio(now, curingDuration) >= 1.0;
    }

    /** Vrai si la tete est restee trop longtemps apres affinage. */
    public boolean isMoldy(long now, Duration curingDuration, Duration moldDelay) {
        return now - startedAt >= curingDuration.toMillis() + moldDelay.toMillis();
    }
}
