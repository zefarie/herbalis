package io.github.zefarie.herbalis.domain.drug;

import java.util.Locale;

/**
 * Style de deformation du chat d'un joueur sous effet.
 */
public enum SlurStyle {
    /** Aucune deformation. */
    NONE,
    /** Elocution defoncee : voyelles etirees, pauses, tics amuses. */
    STONED,
    /** Elocution d'ivresse : begaiement, lettres doublees, hoquets. */
    DRUNK;

    /**
     * Resout un style depuis la config ({@code defonce}, {@code ivre},
     * {@code aucun}). Valeur inconnue : aucune deformation.
     */
    public static SlurStyle fromConfig(String raw) {
        if (raw == null) {
            return NONE;
        }
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "defonce", "défoncé", "stoned" -> STONED;
            case "ivre", "drunk" -> DRUNK;
            default -> NONE;
        };
    }
}
