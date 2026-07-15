package io.github.zefarie.herbalis.domain.plant;

import io.github.zefarie.herbalis.domain.geo.BlockPos;

import java.util.UUID;

/**
 * Etat immuable d'une plante en pot. Chaque tick du moteur de croissance
 * produit une nouvelle instance; la persistence et le rendu consomment
 * ces instances sans jamais les modifier.
 *
 * <p>Les durees sont des compteurs de millisecondes accumulees uniquement
 * quand le chunk est charge (la croissance n'avance pas serveur eteint).</p>
 *
 * @param id                identifiant stable de la plante
 * @param drugId            type de drogue cultive
 * @param pos               position du pot qui porte la plante
 * @param stage             stage de croissance courant, 1-indexe
 * @param stageGrowthMillis progression accumulee dans le stage courant
 * @param ripenMillis       temps accumule au stade final (fenetre de recolte)
 * @param hydration         jauge d'hydratation, 0 a 100
 * @param hydrationSum      somme des echantillons d'hydratation (pour la moyenne)
 * @param hydrationSamples  nombre d'echantillons d'hydratation
 * @param dryMillis         temps accumule a sec (hydration a 0)
 * @param fertilizedStage   dernier stage ou un engrais a ete applique (0 = jamais)
 * @param fertilizerUses    nombre total d'engrais appliques
 * @param seedQuality       qualite de la graine plantee, 1 a 5 (genetique)
 * @param topping           taille : 0 jamais, 1 reussie, -1 ratee
 * @param state             etat de sante visible
 * @param plantedAt         date de plantation (epoch millis)
 */
public record Plant(
        UUID id,
        String drugId,
        BlockPos pos,
        int stage,
        long stageGrowthMillis,
        long ripenMillis,
        double hydration,
        double hydrationSum,
        long hydrationSamples,
        long dryMillis,
        int fertilizedStage,
        int fertilizerUses,
        int seedQuality,
        int topping,
        PlantState state,
        long plantedAt
) {

    /** Qualite de graine neutre (graine commune, sans lignee). */
    public static final int DEFAULT_SEED_QUALITY = 2;

    /** Nouvelle plante fraichement mise en pot, hydratee a 100. */
    public static Plant plant(String drugId, BlockPos pos, long now) {
        return plant(drugId, pos, now, DEFAULT_SEED_QUALITY);
    }

    /** Nouvelle plante issue d'une graine de qualite donnee. */
    public static Plant plant(String drugId, BlockPos pos, long now, int seedQuality) {
        return new Plant(UUID.randomUUID(), drugId, pos,
                1, 0L, 0L,
                100.0, 0.0, 0L, 0L,
                0, 0,
                Math.clamp(seedQuality, 1, 5), 0,
                PlantState.HEALTHY, now);
    }

    /** Hydratation moyenne sur la vie de la plante, 0 a 100. */
    public double averageHydration() {
        if (hydrationSamples == 0) {
            return hydration;
        }
        return hydrationSum / hydrationSamples;
    }

    public boolean isDead() {
        return state == PlantState.DEAD;
    }

    public boolean isFertilizedThisStage() {
        return fertilizedStage == stage;
    }

    /** Vrai si la plante a deja ete taillee (reussie ou ratee). */
    public boolean isToppingAttempted() {
        return topping != 0;
    }

    public Plant withHydration(double newHydration) {
        return new Plant(id, drugId, pos, stage, stageGrowthMillis, ripenMillis,
                Math.clamp(newHydration, 0.0, 100.0), hydrationSum, hydrationSamples,
                dryMillis, fertilizedStage, fertilizerUses, seedQuality, topping,
                state, plantedAt);
    }

    public Plant withFertilizer() {
        return new Plant(id, drugId, pos, stage, stageGrowthMillis, ripenMillis,
                hydration, hydrationSum, hydrationSamples, dryMillis,
                stage, fertilizerUses + 1, seedQuality, topping, state, plantedAt);
    }

    /** Taille reussie : la coupe rend de la progression au stage. */
    public Plant topped(long newStageGrowthMillis) {
        return new Plant(id, drugId, pos, stage,
                Math.max(0L, newStageGrowthMillis), ripenMillis,
                hydration, hydrationSum, hydrationSamples, dryMillis,
                fertilizedStage, fertilizerUses, seedQuality, 1, state, plantedAt);
    }

    /** Taille ratee : la plante est abimee, la qualite en patira. */
    public Plant toppingMissed() {
        return new Plant(id, drugId, pos, stage, stageGrowthMillis, ripenMillis,
                hydration, hydrationSum, hydrationSamples, dryMillis,
                fertilizedStage, fertilizerUses, seedQuality, -1, state, plantedAt);
    }

    Plant ticked(int newStage, long newStageGrowth, long newRipen,
                 double newHydration, double newHydrationSum, long newSamples,
                 long newDryMillis, PlantState newState) {
        return new Plant(id, drugId, pos, newStage, newStageGrowth, newRipen,
                newHydration, newHydrationSum, newSamples, newDryMillis,
                fertilizedStage, fertilizerUses, seedQuality, topping,
                newState, plantedAt);
    }
}
