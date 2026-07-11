package io.github.zefarie.herbalis.domain.drug;

import java.time.Duration;

/**
 * Regles de consommation : abus, tolerance et addiction.
 *
 * @param blackoutCount        nombre de consommations dans la fenetre declenchant le blackout
 * @param blackoutWindow       fenetre glissante de detection de l'abus
 * @param blackoutDuration     duree du blackout
 * @param toleranceGainPerUse  points de tolerance gagnes par consommation (jauge 0 a 100)
 * @param toleranceDecayPerHour points de tolerance perdus par heure, temps reel
 * @param toleranceMaxReduction reduction maximale des effets a tolerance 100 (0.6 = -60%)
 * @param addictionGainPerUse  points d'addiction gagnes par consommation (jauge 0 a 100)
 * @param addictionDecayPerHour points d'addiction perdus par heure d'abstinence
 * @param addictionThreshold   seuil au-dela duquel le joueur est en etat d'addiction
 * @param withdrawalDelay      delai sans consommer avant l'apparition du manque
 */
public record ConsumptionRules(
        int blackoutCount,
        Duration blackoutWindow,
        Duration blackoutDuration,
        double toleranceGainPerUse,
        double toleranceDecayPerHour,
        double toleranceMaxReduction,
        double addictionGainPerUse,
        double addictionDecayPerHour,
        double addictionThreshold,
        Duration withdrawalDelay
) {

    public ConsumptionRules {
        if (blackoutCount < 2) {
            throw new IllegalArgumentException("blackoutCount doit etre au moins 2");
        }
        if (toleranceMaxReduction < 0 || toleranceMaxReduction > 1) {
            throw new IllegalArgumentException("toleranceMaxReduction doit etre entre 0 et 1");
        }
    }
}
