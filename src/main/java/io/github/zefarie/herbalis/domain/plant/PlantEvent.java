package io.github.zefarie.herbalis.domain.plant;

/**
 * Evenements de cycle de vie emis par le moteur de croissance.
 * L'infrastructure les traduit en particules, sons et changements de modele.
 */
public sealed interface PlantEvent {

    /** La plante vient de passer au stage donne. */
    record StageAdvanced(int newStage, boolean isFinal) implements PlantEvent {
    }

    /** La plante est restee a sec et vient de jaunir. */
    record Withered() implements PlantEvent {
    }

    /** La plante jaunie vient de retrouver sa sante. */
    record Recovered() implements PlantEvent {
    }

    /** La plante est morte de soif. */
    record Died() implements PlantEvent {
    }

    /** La fenetre de recolte optimale vient de se refermer. */
    record HarvestWindowClosed() implements PlantEvent {
    }
}
