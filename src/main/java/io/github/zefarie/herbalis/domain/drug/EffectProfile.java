package io.github.zefarie.herbalis.domain.drug;

import java.time.Duration;
import java.util.List;

/**
 * Profil des effets de consommation d'une drogue.
 *
 * @param riseDuration     duree de la montee progressive
 * @param highDurationMin  duree du high a 1 etoile
 * @param highDurationMax  duree du high a 5 etoiles
 * @param comedownRatio    duree de la descente en fraction du high
 * @param highEffects      effets appliques pendant le high
 * @param comedownEffects  effets appliques pendant la descente
 * @param chatSlur         deformation du chat pendant la session
 */
public record EffectProfile(
        Duration riseDuration,
        Duration highDurationMin,
        Duration highDurationMax,
        double comedownRatio,
        List<EffectSpec> highEffects,
        List<EffectSpec> comedownEffects,
        SlurProfile chatSlur
) {

    public EffectProfile {
        highEffects = List.copyOf(highEffects);
        comedownEffects = List.copyOf(comedownEffects);
    }

    /** Duree du high pour une qualite donnee, interpolation lineaire. */
    public Duration highDurationFor(int stars) {
        double t = (stars - 1) / 4.0;
        long min = highDurationMin.toMillis();
        long max = highDurationMax.toMillis();
        return Duration.ofMillis(Math.round(min + (max - min) * t));
    }
}
