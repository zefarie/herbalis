package io.github.zefarie.herbalis.application;

import io.github.zefarie.herbalis.application.port.PipeRepository;
import io.github.zefarie.herbalis.application.port.PlantEnvironment;
import io.github.zefarie.herbalis.application.port.PlantRepository;
import io.github.zefarie.herbalis.application.port.PotRepository;
import io.github.zefarie.herbalis.application.port.SiloRepository;
import io.github.zefarie.herbalis.application.port.TankRepository;
import io.github.zefarie.herbalis.application.service.IrrigationService;
import io.github.zefarie.herbalis.application.usecase.GrowPlantsUseCase;
import io.github.zefarie.herbalis.domain.TestFixtures;
import io.github.zefarie.herbalis.domain.drug.DrugRegistry;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.irrigation.FertilizerSilo;
import io.github.zefarie.herbalis.domain.irrigation.TankSize;
import io.github.zefarie.herbalis.domain.irrigation.WaterTank;
import io.github.zefarie.herbalis.domain.plant.Plant;
import io.github.zefarie.herbalis.domain.plant.PlantEvent;
import io.github.zefarie.herbalis.domain.plant.PlantState;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
        int blockLight = 15;
        int skyLight = 15;

        @Override
        public boolean isLoaded(BlockPos pos) {
            return loaded;
        }

        @Override
        public int lightLevel(BlockPos pos) {
            return light;
        }

        @Override
        public int blockLightLevel(BlockPos pos) {
            return blockLight;
        }

        @Override
        public int skyLightLevel(BlockPos pos) {
            return skyLight;
        }
    }

    private static final class InMemoryPots implements PotRepository {
        final Set<BlockPos> pots = new HashSet<>();
        final Set<BlockPos> drippers = new HashSet<>();

        @Override
        public boolean exists(BlockPos pos) {
            return pots.contains(pos);
        }

        @Override
        public void add(BlockPos pos) {
            pots.add(pos);
        }

        @Override
        public void remove(BlockPos pos) {
            pots.remove(pos);
            drippers.remove(pos);
        }

        @Override
        public Collection<BlockPos> all() {
            return List.copyOf(pots);
        }

        @Override
        public boolean hasDripper(BlockPos pos) {
            return drippers.contains(pos);
        }

        @Override
        public void setDripper(BlockPos pos, boolean installed) {
            if (installed) {
                drippers.add(pos);
            } else {
                drippers.remove(pos);
            }
        }
    }

    private static final class InMemoryTanks implements TankRepository {
        final Map<BlockPos, WaterTank> tanks = new HashMap<>();

        @Override
        public Optional<WaterTank> at(BlockPos pos) {
            return Optional.ofNullable(tanks.get(pos));
        }

        @Override
        public void put(WaterTank tank) {
            tanks.put(tank.pos(), tank);
        }

        @Override
        public void remove(BlockPos pos) {
            tanks.remove(pos);
        }

        @Override
        public Collection<WaterTank> all() {
            return List.copyOf(tanks.values());
        }
    }

    private static final class InMemorySilos implements SiloRepository {
        final Map<BlockPos, FertilizerSilo> silos = new HashMap<>();

        @Override
        public Optional<FertilizerSilo> at(BlockPos pos) {
            return Optional.ofNullable(silos.get(pos));
        }

        @Override
        public void put(FertilizerSilo silo) {
            silos.put(silo.pos(), silo);
        }

        @Override
        public void remove(BlockPos pos) {
            silos.remove(pos);
        }

        @Override
        public Collection<FertilizerSilo> all() {
            return List.copyOf(silos.values());
        }
    }

    private static final class InMemoryPipes implements PipeRepository {
        final Set<BlockPos> pipes = new HashSet<>();

        @Override
        public boolean has(BlockPos pos) {
            return pipes.contains(pos);
        }

        @Override
        public void add(BlockPos pos) {
            pipes.add(pos);
        }

        @Override
        public void remove(BlockPos pos) {
            pipes.remove(pos);
        }

        @Override
        public Set<BlockPos> all() {
            return Set.copyOf(pipes);
        }
    }

    private final DrugRegistry drugs = new DrugRegistry();
    private final InMemoryPlants plants = new InMemoryPlants();
    private final InMemoryPots pots = new InMemoryPots();
    private final InMemoryTanks tanks = new InMemoryTanks();
    private final InMemorySilos silos = new InMemorySilos();
    private final InMemoryPipes pipes = new InMemoryPipes();
    private final FakeEnvironment environment = new FakeEnvironment();
    private final IrrigationService irrigation = new IrrigationService(
            tanks, silos, pipes, pots);
    private final GrowPlantsUseCase grow = new GrowPlantsUseCase(
            plants, pots, drugs, environment, irrigation,
            new java.util.Random(42), 0.5);

    GrowPlantsUseCaseTest() {
        drugs.register(TestFixtures.weed());
    }

    private static BlockPos at(int x, int y, int z) {
        return new BlockPos(TestFixtures.WORLD, x, y, z);
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
    void laPlanteSousCielPousseAuRythmeDuSoleilPendantLeRattrapage() {
        // Exterieur : aucun bloc lumineux, plein ciel, chunk recharge de
        // nuit. Le rattrapage alterne jour/nuit : la moitie du temps
        // pousse, l'autre non.
        plants.put(Plant.plant("weed", TestFixtures.pos(), 0L));
        environment.blockLight = 0;
        environment.skyLight = 15;
        environment.light = 4;
        grow.tick(32 * 60_000L);

        // 32 min dont ~la moitie de jour : un seul stage franchi, la ou
        // une serre eclairee en aurait franchi trois.
        assertEquals(2, plants.at(TestFixtures.pos()).orElseThrow().stage());
    }

    @Test
    void leGoutteAGoutteEconomiseLeauPendantLeRattrapage() {
        plants.put(Plant.plant("weed", TestFixtures.pos(), 0L));
        pots.setDripper(TestFixtures.pos(), true);
        grow.tick(10 * 60_000L);

        // Perte 2.5/min divisee par deux : 12.5 points en 10 minutes.
        Plant after = plants.at(TestFixtures.pos()).orElseThrow();
        assertEquals(100.0 - 12.5, after.hydration(), 0.01);
    }

    @Test
    void leCaissonRelieMaintientLaPlanteEnEauEtSeVide() {
        // Pot en (0), tuyaux en (1) et (2), caisson en (3).
        BlockPos potPos = at(0, 64, 0);
        pots.add(potPos);
        plants.put(Plant.plant("weed", potPos, 0L));
        pipes.add(at(1, 64, 0));
        pipes.add(at(2, 64, 0));
        BlockPos tankPos = at(3, 64, 0);
        tanks.put(new WaterTank(java.util.UUID.randomUUID(), tankPos,
                TankSize.CUVE, 1000.0));

        grow.tick(40 * 60_000L);

        // 40 min a 2.5/min : la plante reste a 100, le caisson paie.
        assertEquals(100.0, plants.at(potPos).orElseThrow().hydration(), 0.01);
        assertEquals(1000.0 - 100.0, tanks.at(tankPos).orElseThrow().stock(), 0.01);
    }

    @Test
    void laPanneSecheLaisseLaPlanteDeclinerEnPleinRattrapage() {
        BlockPos potPos = at(0, 64, 0);
        pots.add(potPos);
        plants.put(Plant.plant("weed", potPos, 0L));
        pipes.add(at(1, 64, 0));
        BlockPos tankPos = at(2, 64, 0);
        // 25 points : 10 minutes d'eau, puis panne seche.
        tanks.put(new WaterTank(java.util.UUID.randomUUID(), tankPos,
                TankSize.CUVE, 25.0));

        grow.tick(20 * 60_000L);

        assertEquals(75.0, plants.at(potPos).orElseThrow().hydration(), 0.01);
        assertEquals(0.0, tanks.at(tankPos).orElseThrow().stock(), 0.01);
    }

    @Test
    void laFertigationConsommeUneDoseParStage() {
        BlockPos potPos = at(0, 64, 0);
        pots.add(potPos);
        plants.put(Plant.plant("weed", potPos, 0L).withHydration(100));
        pipes.add(at(1, 64, 0));
        BlockPos siloPos = at(2, 64, 0);
        silos.put(new FertilizerSilo(java.util.UUID.randomUUID(), siloPos, 2));
        // De l'eau pour ne pas fausser la croissance.
        BlockPos tankPos = at(1, 65, 0);
        tanks.put(new WaterTank(java.util.UUID.randomUUID(), tankPos,
                TankSize.CUVE, 10_000.0));

        grow.tick(12 * 60_000L);

        // Stages de 8 min boostes a x1.5 : le stage 2 est entame, deux
        // doses parties (une par stage).
        Plant after = plants.at(potPos).orElseThrow();
        assertEquals(2, after.stage());
        assertEquals(2, after.fertilizerUses());
        assertEquals(0, silos.at(siloPos).orElseThrow().doses());
    }

    @Test
    void deuxPotsSePartagentLeMemeCaisson() {
        BlockPos potA = at(0, 64, 0);
        BlockPos potB = at(0, 64, 2);
        pots.add(potA);
        pots.add(potB);
        plants.put(Plant.plant("weed", potA, 0L));
        plants.put(Plant.plant("weed", potB, 0L));
        pipes.add(at(0, 64, 1));
        pipes.add(at(1, 64, 1));
        BlockPos tankPos = at(2, 64, 1);
        // 30 points pour 50 de besoin total : le premier servi boit tout.
        tanks.put(new WaterTank(java.util.UUID.randomUUID(), tankPos,
                TankSize.CUVE, 30.0));

        grow.tick(10 * 60_000L);

        assertEquals(0.0, tanks.at(tankPos).orElseThrow().stock(), 0.01);
        List<Double> hydrations = new java.util.ArrayList<>(List.of(
                plants.at(potA).orElseThrow().hydration(),
                plants.at(potB).orElseThrow().hydration()));
        hydrations.sort(Double::compareTo);
        assertEquals(80.0, hydrations.get(0), 0.01);
        assertEquals(100.0, hydrations.get(1), 0.01);
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
