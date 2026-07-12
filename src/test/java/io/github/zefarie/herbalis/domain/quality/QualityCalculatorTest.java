package io.github.zefarie.herbalis.domain.quality;

import io.github.zefarie.herbalis.domain.TestFixtures;
import io.github.zefarie.herbalis.domain.drug.DrugType;
import io.github.zefarie.herbalis.domain.plant.Plant;
import io.github.zefarie.herbalis.domain.plant.PlantState;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QualityCalculatorTest {

    private final DrugType weed = TestFixtures.weed();

    private static Plant plantWith(double avgHydration, int fertilizerUses,
                                   long ripenMillis) {
        return new Plant(UUID.randomUUID(), "weed", TestFixtures.pos(),
                4, 0L, ripenMillis,
                avgHydration, avgHydration * 100, 100L, 0L,
                0, fertilizerUses, PlantState.HEALTHY, 0L);
    }

    @Test
    void cultureParfaiteDonneCinqEtoiles() {
        Plant plant = plantWith(100.0, 4, 0L);
        assertEquals(5, QualityCalculator.harvestQuality(plant, weed).stars());
    }

    @Test
    void cultureNegligeeDonneUneEtoile() {
        // Hydratation moyenne quasi nulle, aucun engrais, tres en retard.
        Plant plant = plantWith(2.0, 0, weed.harvestWindow().optimalDuration()
                .plus(weed.harvestWindow().decayDuration()).toMillis() + 60_000);
        assertEquals(1, QualityCalculator.harvestQuality(plant, weed).stars());
    }

    @Test
    void recolteTardiveCouteDesEtoiles() {
        Plant onTime = plantWith(100.0, 4, 0L);
        Plant late = plantWith(100.0, 4,
                weed.harvestWindow().optimalDuration().toMillis()
                        + weed.harvestWindow().decayDuration().toMillis());
        assertTrue(QualityCalculator.harvestQuality(late, weed)
                .compareTo(QualityCalculator.harvestQuality(onTime, weed)) < 0);
    }

    @Test
    void timingDecroitLineairementApresLaFenetre() {
        long optimal = weed.harvestWindow().optimalDuration().toMillis();
        long decay = weed.harvestWindow().decayDuration().toMillis();
        assertEquals(1.0, weed.harvestWindow().timingScore(optimal));
        assertEquals(0.5, weed.harvestWindow().timingScore(optimal + decay / 2), 0.01);
        assertEquals(0.0, weed.harvestWindow().timingScore(optimal + decay));
    }

    @Test
    void sechageCompletConserveLaQualite() {
        assertEquals(5, QualityCalculator.afterDrying(Quality.of(5), 1.0).stars());
        assertEquals(5, QualityCalculator.afterDrying(Quality.of(5), 1.4).stars());
    }

    @Test
    void retraitAnticipePenaliseProportionnellement() {
        // A moitie seche : 2 etoiles perdues.
        assertEquals(3, QualityCalculator.afterDrying(Quality.of(5), 0.5).stars());
        // A peine entamee : retour au minimum.
        assertEquals(1, QualityCalculator.afterDrying(Quality.of(5), 0.05).stars());
        // Jamais en dessous de 1 etoile.
        assertEquals(1, QualityCalculator.afterDrying(Quality.of(1), 0.0).stars());
    }

    @Test
    void scoreEstBorneEntreUneEtCinqEtoiles() {
        assertEquals(1, Quality.fromScore(-0.3).stars());
        assertEquals(5, Quality.fromScore(1.7).stars());
        assertEquals(3, Quality.fromScore(0.5).stars());
    }
}
