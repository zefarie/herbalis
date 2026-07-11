package io.github.zefarie.herbalis.domain.drying;

/**
 * Etat visuel d'un rack de sechage, chaque valeur correspond a un modele
 * 3D distinct du resource pack.
 */
public enum RackVisualState {
    /** Rack vide, structure nue. */
    EMPTY,
    /** Tetes vertes suspendues, sechage en cours. */
    DRYING,
    /** Tetes brunies, tout est pret a etre recupere. */
    READY
}
