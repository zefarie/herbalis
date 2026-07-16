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
        return plantWith(avgHydration, fertilizerUses, ripenMillis, 3, 0);
    }

    private static Plant plantWith(double avgHydration, int fertilizerUses,
                                   long ripenMillis, int seedQuality, int topping) {
        return new Plant(UUID.randomUUID(), "weed", TestFixtures.pos(),
                4, 0L, ripenMillis,
                avgHydration, avgHydration * 100, 100L, 0L,
                0, fertilizerUses, seedQuality, topping, 0L, 0,
                PlantState.HEALTHY, 0L, 0L);
    }

    @Test
    void cultureParfaiteDonneCinqEtoiles() {
        Plant plant = plantWith(100.0, 4, 0L, 5, 0);
        assertEquals(5, QualityCalculator.harvestQuality(plant, weed).stars());
    }

    @Test
    void cultureNegligeeDonneUneEtoile() {
        // Hydratation moyenne quasi nulle, aucun engrais, tres en retard,
        // graine de fond de tiroir.
        Plant plant = plantWith(2.0, 0, weed.harvestWindow().optimalDuration()
                        .plus(weed.harvestWindow().decayDuration()).toMillis() + 60_000,
                1, 0);
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
    void laGenetiquePeseSurLaQualite() {
        // Culture parfaite : seule la graine differe.
        Plant bonneLignee = plantWith(100.0, 4, 0L, 5, 0);
        Plant ligneeFaible = plantWith(100.0, 4, 0L, 1, 0);
        assertEquals(5, QualityCalculator.harvestQuality(bonneLignee, weed).stars());
        assertEquals(4, QualityCalculator.harvestQuality(ligneeFaible, weed).stars());
    }

    @Test
    void uneTailleRateeCouteUneEtoile() {
        Plant intacte = plantWith(100.0, 4, 0L, 5, 0);
        Plant abimee = plantWith(100.0, 4, 0L, 5, -1);
        assertEquals(5, QualityCalculator.harvestQuality(intacte, weed).stars());
        assertEquals(4, QualityCalculator.harvestQuality(abimee, weed).stars());
    }

    @Test
    void uneInfestationNonTraiteeCouteUneEtoile() {
        Plant saine = plantWith(100.0, 4, 0L, 5, 0);
        Plant abimee = new Plant(UUID.randomUUID(), "weed", TestFixtures.pos(),
                4, 0L, 0L, 100.0, 100.0 * 100, 100L, 0L,
                0, 4, 5, 0, 0L, 1, PlantState.HEALTHY, 0L, 0L);
        assertEquals(5, QualityCalculator.harvestQuality(saine, weed).stars());
        assertEquals(4, QualityCalculator.harvestQuality(abimee, weed).stars());
    }

    @Test
    void curingCompletBonifieEtMoisissureRuine() {
        assertEquals(4, QualityCalculator.afterCuring(
                Quality.of(3), true, false, 1).stars());
        assertEquals(5, QualityCalculator.afterCuring(
                Quality.of(5), true, false, 1).stars());
        assertEquals(3, QualityCalculator.afterCuring(
                Quality.of(3), false, false, 1).stars());
        assertEquals(1, QualityCalculator.afterCuring(
                Quality.of(5), true, true, 1).stars());
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
