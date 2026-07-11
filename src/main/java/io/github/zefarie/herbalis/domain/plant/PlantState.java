package io.github.zefarie.herbalis.domain.plant;

/**
 * Etat de sante visible d'une plante.
 */
public enum PlantState {
    /** Plante en bonne sante, modele normal. */
    HEALTHY,
    /** Plante restee a sec trop longtemps, modele jauni. */
    WITHERED,
    /** Plante morte, ne pousse plus, seul le pot est recuperable. */
    DEAD
}
