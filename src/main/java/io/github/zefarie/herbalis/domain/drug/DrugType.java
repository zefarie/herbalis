package io.github.zefarie.herbalis.domain.drug;

import io.github.zefarie.herbalis.domain.quality.QualityWeights;

/**
 * Definition complete d'un type de drogue. La weed est une instance de ce
 * record chargee depuis la config; ajouter une drogue plus tard revient a
 * charger une definition supplementaire, sans toucher au code.
 *
 * @param id          identifiant technique (ex : "weed"), utilise pour les
 *                    modeles du resource pack et la persistence
 * @param displayName nom affiche brut, stylise par la couche infrastructure
 */
public record DrugType(
        String id,
        String displayName,
        GrowthProfile growth,
        HydrationProfile hydration,
        FertilizerProfile fertilizer,
        HarvestWindow harvestWindow,
        DryingProfile drying,
        ToppingProfile topping,
        CuringProfile curing,
        PestProfile pests,
        EffectProfile effects,
        ConsumptionRules consumption,
        QualityWeights qualityWeights
) {

    public DrugType {
        if (id == null || id.isBlank() || !id.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException(
                    "L'identifiant d'une drogue doit etre en minuscules [a-z0-9_] : " + id);
        }
    }
}
