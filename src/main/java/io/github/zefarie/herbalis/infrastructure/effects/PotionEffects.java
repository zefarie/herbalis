package io.github.zefarie.herbalis.infrastructure.effects;

import io.github.zefarie.herbalis.domain.drug.EffectSpec;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Optional;

/**
 * Resolution des cles d'effets de la config vers l'API Bukkit et
 * application groupee.
 */
public final class PotionEffects {

    private PotionEffects() {
    }

    public static Optional<PotionEffectType> resolve(String key) {
        NamespacedKey namespaced = NamespacedKey.fromString(key);
        if (namespaced == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.MOB_EFFECT).get(namespaced));
    }

    /** Applique les effets continus (rafraichis chaque seconde), discrets. */
    public static void applyPersistent(Player player, List<EffectSpec> specs,
                                       int stars, int durationTicks) {
        for (EffectSpec spec : specs) {
            if (spec.oneShot()) {
                continue;
            }
            resolve(spec.effectKey()).ifPresent(type ->
                    player.addPotionEffect(new PotionEffect(type, durationTicks,
                            spec.amplifierFor(stars), true, false, true)));
        }
    }

    /** Applique les effets ponctuels d'entree de phase (courte nausee...). */
    public static void applyOneShots(Player player, List<EffectSpec> specs,
                                     int stars, int durationTicks) {
        for (EffectSpec spec : specs) {
            if (!spec.oneShot()) {
                continue;
            }
            resolve(spec.effectKey()).ifPresent(type ->
                    player.addPotionEffect(new PotionEffect(type, durationTicks,
                            spec.amplifierFor(stars), true, false, true)));
        }
    }

    public static void remove(Player player, List<EffectSpec> specs) {
        for (EffectSpec spec : specs) {
            resolve(spec.effectKey()).ifPresent(player::removePotionEffect);
        }
    }

    /** Effet unique par cle vanilla, pour les moments scriptes. */
    public static void one(Player player, String key, int durationTicks, int amplifier) {
        resolve(key).ifPresent(type ->
                player.addPotionEffect(new PotionEffect(type, durationTicks,
                        amplifier, true, false, true)));
    }

    public static void clear(Player player, String key) {
        resolve(key).ifPresent(player::removePotionEffect);
    }
}
