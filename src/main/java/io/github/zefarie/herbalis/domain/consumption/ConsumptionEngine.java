package io.github.zefarie.herbalis.domain.consumption;

import io.github.zefarie.herbalis.domain.drug.ConsumptionRules;
import io.github.zefarie.herbalis.domain.drug.DrugType;
import io.github.zefarie.herbalis.domain.quality.Quality;

import java.util.ArrayList;
import java.util.List;

/**
 * Regles de tolerance, d'addiction et d'abus. Fonctions pures sur
 * {@link ConsumerProfile}.
 */
public final class ConsumptionEngine {

    private ConsumptionEngine() {
    }

    /**
     * Resultat d'une consommation.
     *
     * @param profile  nouveau profil du joueur
     * @param timeline chronologie des effets a derouler
     * @param blackout vrai si cette consommation declenche un blackout
     */
    public record ConsumeOutcome(
            ConsumerProfile profile,
            EffectTimeline timeline,
            boolean blackout
    ) {
    }

    /**
     * Applique la decroissance temps reel de la tolerance et de
     * l'addiction, sur la base du temps ecoule depuis la derniere mise a
     * jour (fonctionne aussi apres une longue deconnexion).
     */
    public static ConsumerProfile decayed(ConsumerProfile profile, long now,
                                          ConsumptionRules rules) {
        long elapsed = Math.max(0, now - profile.lastUpdatedAt());
        if (elapsed == 0) {
            return profile;
        }
        double hours = elapsed / 3_600_000.0;
        double tolerance = Math.max(0.0,
                profile.tolerance() - rules.toleranceDecayPerHour() * hours);
        double addiction = Math.max(0.0,
                profile.addiction() - rules.addictionDecayPerHour() * hours);
        return new ConsumerProfile(tolerance, addiction, now,
                profile.lastConsumedAt(), profile.recentConsumptions());
    }

    /**
     * Consomme un produit : met a jour les jauges, l'historique recent,
     * calcule la chronologie des effets et detecte l'abus.
     */
    public static ConsumeOutcome consume(ConsumerProfile profile, long now,
                                         Quality quality, DrugType drug) {
        ConsumptionRules rules = drug.consumption();
        ConsumerProfile current = decayed(profile, now, rules);

        EffectTimeline timeline = EffectTimeline.create(
                drug.effects(), rules, quality, current.tolerance());

        double tolerance = Math.min(100.0,
                current.tolerance() + rules.toleranceGainPerUse());
        double addiction = Math.min(100.0,
                current.addiction() + rules.addictionGainPerUse());

        long windowStart = now - rules.blackoutWindow().toMillis();
        List<Long> history = new ArrayList<>();
        for (long timestamp : current.recentConsumptions()) {
            if (timestamp >= windowStart) {
                history.add(timestamp);
            }
        }
        history.add(now);
        boolean blackout = history.size() >= rules.blackoutCount();

        ConsumerProfile next = new ConsumerProfile(
                tolerance, addiction, now, now, history);
        return new ConsumeOutcome(next, timeline, blackout);
    }

    /**
     * Etat de manque au temps donne. Le manque frappe les joueurs dont
     * l'addiction depasse le seuil et qui n'ont pas consomme depuis
     * {@code withdrawalDelay}. Il cesse quand l'addiction redescend sous
     * le seuil (decroissance temps reel) ou a la prochaine consommation.
     */
    public static WithdrawalState withdrawal(ConsumerProfile profile, long now,
                                             ConsumptionRules rules) {
        ConsumerProfile current = decayed(profile, now, rules);
        if (!current.hasEverConsumed()) {
            return WithdrawalState.NONE;
        }
        if (current.addiction() < rules.addictionThreshold()) {
            return WithdrawalState.NONE;
        }
        long sinceLast = now - current.lastConsumedAt();
        if (sinceLast < rules.withdrawalDelay().toMillis()) {
            return WithdrawalState.NONE;
        }
        return WithdrawalState.WITHDRAWAL;
    }
}
