package io.github.zefarie.herbalis.application;

import io.github.zefarie.herbalis.application.port.PlantEnvironment;
import io.github.zefarie.herbalis.application.port.PlantRepository;
import io.github.zefarie.herbalis.application.usecase.GrowPlantsUseCase;
import io.github.zefarie.herbalis.domain.TestFixtures;
import io.github.zefarie.herbalis.domain.drug.DrugRegistry;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.plant.Plant;
import io.github.zefarie.herbalis.domain.plant.PlantEvent;
import io.github.zefarie.herbalis.domain.plant.PlantState;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Croissance en temps reel : le retard d'une plante (chunk decharge,
 * serveur eteint) se rattrape au tick suivant, tranche par tranche.
 */
class GrowPlantsUseCaseTest {

    private static final class InMemoryPlants implements PlantRepository {
        private final Map<BlockPos, Plant> plants = new HashMap<>();

        @Override
        public Optional<Plant> at(BlockPos pos) {
            return Optional.ofNullable(plants.get(pos));
        }

        @Override
        public void put(Plant plant) {
            plants.put(plant.pos(), plant);
        }

        @Override
        public void remove(BlockPos pos) {
            plants.remove(pos);
        }

        @Override
        public Collection<Plant> all() {
            return List.copyOf(plants.values());
        }
    }

    private static final class FakeEnvironment implements PlantEnvironment {
        boolean loaded = true;
        int light = 15;

        @Override
        public boolean isLoaded(BlockPos pos) {
            return loaded;
        }

        @Override
        public int lightLevel(BlockPos pos) {
            return light;
        }
    }

    private final DrugRegistry drugs = new DrugRegistry();
    private final InMemoryPlants plants = new InMemoryPlants();
    private final FakeEnvironment environment = new FakeEnvironment();
    private final GrowPlantsUseCase grow =
            new GrowPlantsUseCase(plants, drugs, environment);

    GrowPlantsUseCaseTest() {
        drugs.register(TestFixtures.weed());
    }

    @Test
    void rattrapeLesStagesManquesEnUnSeulTick() {
        // Stages de 8 min : 20 min d'absence font passer deux stages.
        plants.put(Plant.plant("weed", TestFixtures.pos(), 0L));
        List<GrowPlantsUseCase.PlantTickReport> reports = grow.tick(20 * 60_000L);

        assertEquals(1, reports.size());
        assertEquals(3, reports.getFirst().plant().stage());
        assertEquals(2, reports.getFirst().events().stream()
                .filter(e -> e instanceof PlantEvent.StageAdvanced)
                .count());
    }

    @Test
    void laPlanteNegligeeMeurtAuBonMomentDuRattrapage() {
        // Jauge a sec au bout de 40 min, mort 12 min plus tard : une heure
        // d'absence tue la plante, meme traversee en un seul tick.
        plants.put(Plant.plant("weed", TestFixtures.pos(), 0L));
        List<GrowPlantsUseCase.PlantTickReport> reports = grow.tick(3_600_000L);

        Plant after = plants.at(TestFixtures.pos()).orElseThrow();
        assertEquals(PlantState.DEAD, after.state());
        assertTrue(reports.getFirst().events().stream()
                .anyMatch(e -> e instanceof PlantEvent.Died));
    }

    @Test
    void unChunkDechargeNeFigeRienDefinitivement() {
        plants.put(Plant.plant("weed", TestFixtures.pos(), 0L));
        environment.loaded = false;
        assertTrue(grow.tick(10 * 60_000L).isEmpty());
        assertEquals(0L, plants.at(TestFixtures.pos()).orElseThrow().lastTickAt());

        // Le chunk revient : tout le retard est rattrape d'un coup.
        environment.loaded = true;
        grow.tick(20 * 60_000L);
        assertEquals(3, plants.at(TestFixtures.pos()).orElseThrow().stage());
    }

    @Test
    void unTickDejaRattrapeNeRejoueRien() {
        plants.put(Plant.plant("weed", TestFixtures.pos(), 0L));
        grow.tick(10 * 60_000L);
        Plant after = plants.at(TestFixtures.pos()).orElseThrow();
        assertEquals(10 * 60_000L, after.lastTickAt());

        assertTrue(grow.tick(10 * 60_000L).isEmpty());
        assertEquals(after, plants.at(TestFixtures.pos()).orElseThrow());
    }
}
