package io.github.zefarie.herbalis.domain.drug;

/**
 * Deformation du chat pendant une session d'effets. L'intensite se
 * regle par phase, de 0 (aucune) a 1 (deformation maximale) ; la
 * tolerance du joueur la reduit ensuite comme le reste des effets.
 *
 * @param style             style de deformation applique
 * @param riseIntensity     intensite pendant la montee (atteinte en fin de montee)
 * @param highIntensity     intensite pendant le plateau
 * @param comedownIntensity intensite pendant la descente
 */
public record SlurProfile(
        SlurStyle style,
        double riseIntensity,
        double highIntensity,
        double comedownIntensity
) {

    public static final SlurProfile DEFAULT =
            new SlurProfile(SlurStyle.STONED, 0.3, 1.0, 0.35);

    public static final SlurProfile DISABLED =
            new SlurProfile(SlurStyle.NONE, 0.0, 0.0, 0.0);

    public boolean enabled() {
        return style != SlurStyle.NONE
                && (riseIntensity > 0 || highIntensity > 0 || comedownIntensity > 0);
    }
}
