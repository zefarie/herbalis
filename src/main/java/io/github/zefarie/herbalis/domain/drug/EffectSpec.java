package io.github.zefarie.herbalis.domain.drug;

/**
 * Specification d'un effet de potion, portee par la config.
 * Le domaine ne connait que la cle et l'amplificateur, l'infrastructure
 * resout la cle vers l'API Bukkit.
 *
 * @param effectKey     cle de l'effet (ex : "minecraft:regeneration")
 * @param amplifier     amplificateur de base (0 = niveau I)
 * @param scalesWithQuality vrai si l'amplificateur augmente avec la qualite
 */
public record EffectSpec(
        String effectKey,
        int amplifier,
        boolean scalesWithQuality
) {

    /** Amplificateur effectif pour une qualite en etoiles (1 a 5). */
    public int amplifierFor(int stars) {
        if (!scalesWithQuality) {
            return amplifier;
        }
        return amplifier + (stars >= 4 ? 1 : 0);
    }
}
