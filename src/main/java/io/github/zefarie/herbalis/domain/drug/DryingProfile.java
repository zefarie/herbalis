package io.github.zefarie.herbalis.domain.drug;

import java.time.Duration;

/**
 * Parametres de sechage d'une drogue.
 *
 * @param duration duree de sechage complete d'une tete
 * @param capacity nombre de tetes qu'un rack peut accueillir
 */
public record DryingProfile(
        Duration duration,
        int capacity
) {

    public DryingProfile {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity doit etre au moins 1");
        }
    }
}
