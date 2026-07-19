package io.github.zefarie.herbalis.domain.irrigation;

/**
 * Etat visuel d'un caisson : le niveau d'eau se lit dans la cuve.
 */
public enum TankVisualState {
    EMPTY, LOW, MID, FULL;

    public static TankVisualState of(double fillRatio) {
        if (fillRatio <= 0.001) {
            return EMPTY;
        }
        if (fillRatio < 0.35) {
            return LOW;
        }
        return fillRatio < 0.8 ? MID : FULL;
    }
}
