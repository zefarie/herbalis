package io.github.zefarie.herbalis.domain.irrigation;

/**
 * Etat visuel d'un silo d'engrais : vide, entame ou plein.
 */
public enum SiloVisualState {
    EMPTY, PARTIAL, FULL;

    public static SiloVisualState of(int doses, int capacity) {
        if (doses <= 0) {
            return EMPTY;
        }
        return doses >= capacity ? FULL : PARTIAL;
    }
}
