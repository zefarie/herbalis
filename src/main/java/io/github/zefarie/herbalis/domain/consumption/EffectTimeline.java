package io.github.zefarie.herbalis.domain.consumption;

import io.github.zefarie.herbalis.domain.drug.ConsumptionRules;
import io.github.zefarie.herbalis.domain.drug.EffectProfile;
import io.github.zefarie.herbalis.domain.quality.Quality;

/**
 * Chronologie d'une session d'effets : montee, plateau, descente.
 * Calculee une fois a la consommation, puis interrogee par le ticker
 * d'effets a partir du temps ecoule.
 *
 * @param riseMillis     duree de la montee
 * @param highMillis     duree du plateau (reduite par la tolerance)
 * @param comedownMillis duree de la descente (proportionnelle au plateau)
 * @param intensity      multiplicateur d'intensite des effets, 0 a 1
 * @param quality        qualite du produit consomme
 */
public record EffectTimeline(
        long riseMillis,
        long highMillis,
        long comedownMillis,
        double intensity,
        Quality quality
) {

    /**
     * Construit la chronologie pour une qualite et une tolerance donnees.
     * La tolerance reduit la duree du plateau et l'intensite, jusqu'a
     * {@code toleranceMaxReduction} a tolerance 100.
     */
    public static EffectTimeline create(EffectProfile effects, ConsumptionRules rules,
                                        Quality quality, double tolerance) {
        double reduction = Math.clamp(tolerance / 100.0, 0.0, 1.0)
                * rules.toleranceMaxReduction();
        double factor = 1.0 - reduction;

        long high = Math.round(effects.highDurationFor(quality.stars()).toMillis() * factor);
        long comedown = Math.round(high * effects.comedownRatio());

        return new EffectTimeline(
                effects.riseDuration().toMillis(),
                high,
                comedown,
                factor,
                quality);
    }

    public long totalMillis() {
        return riseMillis + highMillis + comedownMillis;
    }

    /** Phase active au temps ecoule donne. */
    public EffectPhase phaseAt(long elapsedMillis) {
        if (elapsedMillis < riseMillis) {
            return EffectPhase.RISE;
        }
        if (elapsedMillis < riseMillis + highMillis) {
            return EffectPhase.HIGH;
        }
        if (elapsedMillis < totalMillis()) {
            return EffectPhase.COMEDOWN;
        }
        return EffectPhase.DONE;
    }

    /** Progression dans la montee, 0 a 1 (1 = montee terminee). */
    public double riseProgress(long elapsedMillis) {
        if (riseMillis <= 0) {
            return 1.0;
        }
        return Math.clamp(elapsedMillis / (double) riseMillis, 0.0, 1.0);
    }
}
