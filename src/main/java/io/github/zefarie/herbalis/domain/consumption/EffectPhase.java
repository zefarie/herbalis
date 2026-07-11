package io.github.zefarie.herbalis.domain.consumption;

/**
 * Phase d'une session d'effets apres consommation.
 */
public enum EffectPhase {
    /** Montee progressive, les effets s'installent par paliers. */
    RISE,
    /** Plateau, tous les effets positifs sont actifs. */
    HIGH,
    /** Descente, effets negatifs legers. */
    COMEDOWN,
    /** Session terminee. */
    DONE
}
