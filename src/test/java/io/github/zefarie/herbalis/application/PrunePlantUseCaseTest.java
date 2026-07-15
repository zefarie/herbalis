package io.github.zefarie.herbalis.application;

import io.github.zefarie.herbalis.application.port.PlantRepository;
import io.github.zefarie.herbalis.application.usecase.HarvestPlantUseCase;
import io.github.zefarie.herbalis.application.usecase.PrunePlantUseCase;
import io.github.zefarie.herbalis.domain.TestFixtures;
import io.github.zefarie.herbalis.domain.drug.DrugRegistry;
import io.github.zefarie.herbalis.domain.drug.DrugType;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.plant.Plant;
import io.github.zefarie.herbalis.domain.plant.PlantState;
import io.github.zefarie.herbalis.domain.quality.Quality;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrunePlantUseCaseTest {

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

    private final DrugType weed = TestFixtures.weed();
    private final DrugRegistry drugs = new DrugRegistry();
    private final InMemoryPlants plants = new InMemoryPlants();
    private final PrunePlantUseCase prune = new PrunePlantUseCase(plants, drugs);

    PrunePlantUseCaseTest() {
        drugs.register(weed);
    }

    private Plant plantAtStage(int stage, double progress) {
        long growth = (long) (progress
                * weed.growth().durationOf(stage).toMillis());
        return new Plant(UUID.randomUUID(), "weed", TestFixtures.pos(),
                stage, growth, 0L, 100.0, 0.0, 0L, 0L, 0, 0, 3, 0,
                PlantState.HEALTHY, 0L);
    }

    @Test
    void tailleDansLaFenetreBonifieEtRendDeLaProgression() {
        Plant plant = plantAtStage(2, 0.45);
        plants.put(plant);
        var result = prune.execute(plant.pos());
        var topped = assertInstanceOf(PrunePlantUseCase.Result.Topped.class, result);
        assertEquals(1, topped.plant().topping());
        assertEquals(plant.stageGrowthMillis()
                        - weed.topping().setback().toMillis(),
                topped.plant().stageGrowthMillis());
    }

    @Test
    void tailleHorsFenetreAbimeLaPlante() {
        Plant plant = plantAtStage(2, 0.10);
        plants.put(plant);
        var result = prune.execute(plant.pos());
        var missed = assertInstanceOf(PrunePlantUseCase.Result.Missed.class, result);
        assertEquals(-1, missed.plant().topping());
    }

    @Test
    void uneSeuleTailleParPlante() {
        Plant plant = plantAtStage(3, 0.45);
        plants.put(plant);
        prune.execute(plant.pos());
        assertInstanceOf(PrunePlantUseCase.Result.AlreadyTopped.class,
                prune.execute(plant.pos()));
    }

    @Test
    void leStadeFinalNeSeTaillePas() {
        Plant plant = plantAtStage(4, 0.45);
        plants.put(plant);
        assertInstanceOf(PrunePlantUseCase.Result.Missed.class,
                prune.execute(plant.pos()));
    }

    @Test
    void laRecolteRendDesGrainesHeriteesEtLeBonusDeTaille() {
        Plant plant = new Plant(UUID.randomUUID(), "weed", TestFixtures.pos(),
                4, 0L, 0L, 100.0, 10_000.0, 100L, 0L, 4, 4, 5, 1,
                PlantState.HEALTHY, 0L);
        plants.put(plant);
        var harvest = new HarvestPlantUseCase(plants, drugs, new Random(42));
        var result = harvest.execute(plant.pos());
        var success = assertInstanceOf(
                HarvestPlantUseCase.Result.Success.class, result);

        // 2 ou 3 graines, chacune a une etoile de la mere au plus.
        assertTrue(success.seeds().size() >= 2 && success.seeds().size() <= 3);
        for (Quality seed : success.seeds()) {
            assertTrue(Math.abs(seed.stars() - success.quality().stars()) <= 1);
        }
        // Taille reussie : rendement au-dela du bareme de base.
        assertTrue(success.yield()
                > weed.growth().yieldFor(success.quality().stars()));
    }
}
