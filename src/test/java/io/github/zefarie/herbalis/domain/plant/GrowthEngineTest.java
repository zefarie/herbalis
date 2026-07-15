package io.github.zefarie.herbalis.domain.plant;

import io.github.zefarie.herbalis.domain.TestFixtures;
import io.github.zefarie.herbalis.domain.drug.DrugType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrowthEngineTest {

    private final DrugType weed = TestFixtures.weed();

    private static Plant fresh() {
        return Plant.plant("weed", TestFixtures.pos(), 0L);
    }

    private static Plant tickFor(Plant plant, DrugType drug, long totalMillis,
                                 long stepMillis, int light) {
        Plant current = plant;
        for (long elapsed = 0; elapsed < totalMillis; elapsed += stepMillis) {
            current = GrowthEngine.tick(current, drug,
                    new GrowthConditions(light, stepMillis)).plant();
        }
        return current;
    }

    @Test
    void progresseAuStageSuivantApresLaDuree() {
        Plant plant = fresh().withHydration(100);
        GrowthEngine.GrowthTick tick = GrowthEngine.tick(plant, weed,
                new GrowthConditions(15, weed.growth().durationOf(1).toMillis()));
        assertEquals(2, tick.plant().stage());
        assertTrue(tick.events().stream().anyMatch(
                e -> e instanceof PlantEvent.StageAdvanced advanced
                        && advanced.newStage() == 2 && !advanced.isFinal()));
    }

    @Test
    void atteintLeStadeFinalEtSignaleLaFloraison() {
        Plant plant = fresh();
        // 3 passages de stage de 8 minutes, en tickant par minute avec arrosage.
        Plant current = plant;
        List<PlantEvent> allEvents = new java.util.ArrayList<>();
        for (int i = 0; i < 3 * 8; i++) {
            current = current.withHydration(100);
            GrowthEngine.GrowthTick tick = GrowthEngine.tick(current, weed,
                    new GrowthConditions(15, 60_000));
            current = tick.plant();
            allEvents.addAll(tick.events());
        }
        assertEquals(4, current.stage());
        assertTrue(allEvents.stream().anyMatch(
                e -> e instanceof PlantEvent.StageAdvanced advanced && advanced.isFinal()));
    }

    @Test
    void croissanceFigeeSansLumiere() {
        Plant plant = fresh().withHydration(100);
        Plant after = GrowthEngine.tick(plant, weed,
                new GrowthConditions(8, 60_000)).plant();
        assertEquals(0, after.stageGrowthMillis());
        assertEquals(1, after.stage());
        assertTrue(GrowthEngine.isLightStarved(after, weed, 8));
        assertFalse(GrowthEngine.isLightStarved(after, weed, 12));
    }

    @Test
    void croissanceFigeeSousLeSeuilDeSoif() {
        Plant plant = fresh().withHydration(10);
        Plant after = GrowthEngine.tick(plant, weed,
                new GrowthConditions(15, 60_000)).plant();
        assertEquals(0, after.stageGrowthMillis());
    }

    @Test
    void engraisAccelereLeStage() {
        Plant sans = fresh().withHydration(100);
        Plant avec = fresh().withHydration(100).withFertilizer();
        long delta = 60_000;
        Plant apresSans = GrowthEngine.tick(sans, weed,
                new GrowthConditions(15, delta)).plant();
        Plant apresAvec = GrowthEngine.tick(avec, weed,
                new GrowthConditions(15, delta)).plant();
        assertEquals(delta, apresSans.stageGrowthMillis());
        assertEquals(Math.round(delta * 1.5), apresAvec.stageGrowthMillis());
    }

    @Test
    void jaunitPuisMeurtASec() {
        Plant plant = fresh().withHydration(0.5);
        // 5 minutes a sec : jauni (delai 4 min), pas mort (delai 12 min).
        Plant withered = tickFor(plant, weed, 5 * 60_000, 30_000, 15);
        assertEquals(PlantState.WITHERED, withered.state());
        assertFalse(withered.isDead());
        // 8 minutes de plus : morte.
        Plant dead = tickFor(withered, weed, 8 * 60_000, 30_000, 15);
        assertEquals(PlantState.DEAD, dead.state());
    }

    @Test
    void recupereApresArrosage() {
        Plant plant = fresh().withHydration(0.5);
        Plant withered = tickFor(plant, weed, 5 * 60_000, 30_000, 15);
        assertEquals(PlantState.WITHERED, withered.state());

        GrowthEngine.GrowthTick tick = GrowthEngine.tick(
                withered.withHydration(80), weed, new GrowthConditions(15, 1_000));
        assertEquals(PlantState.HEALTHY, tick.plant().state());
        assertTrue(tick.events().stream()
                .anyMatch(e -> e instanceof PlantEvent.Recovered));
    }

    @Test
    void fenetreDeRecolteSeRefermeApresLaDureeOptimale() {
        Plant atFinal = new Plant(java.util.UUID.randomUUID(), "weed",
                TestFixtures.pos(), 4, 0L, 0L, 100.0, 0.0, 0L, 0L, 0, 0, 2, 0,
                PlantState.HEALTHY, 0L);
        // 9 minutes : encore optimal.
        Plant inWindow = tickFor(atFinal, weed, 9 * 60_000, 60_000, 15);
        assertTrue(weed.harvestWindow().isOptimal(inWindow.ripenMillis()));
        // 2 minutes de plus : fenetre refermee.
        GrowthEngine.GrowthTick closing = GrowthEngine.tick(
                inWindow.withHydration(100), weed,
                new GrowthConditions(15, 2 * 60_000));
        assertFalse(weed.harvestWindow().isOptimal(closing.plant().ripenMillis()));
        assertTrue(closing.events().stream()
                .anyMatch(e -> e instanceof PlantEvent.HarvestWindowClosed));
    }
}
