package io.github.zefarie.herbalis.infrastructure.render;

import io.github.zefarie.herbalis.application.port.JarRepository;
import io.github.zefarie.herbalis.application.port.LampRepository;
import io.github.zefarie.herbalis.application.port.PipeRepository;
import io.github.zefarie.herbalis.application.port.PlantRepository;
import io.github.zefarie.herbalis.application.port.PotRepository;
import io.github.zefarie.herbalis.application.port.RackRepository;
import io.github.zefarie.herbalis.application.port.SiloRepository;
import io.github.zefarie.herbalis.application.port.TankRepository;
import io.github.zefarie.herbalis.domain.curing.CuringJar;
import io.github.zefarie.herbalis.domain.curing.JarVisualState;
import io.github.zefarie.herbalis.domain.drug.DrugRegistry;
import io.github.zefarie.herbalis.domain.drying.DryingRack;
import io.github.zefarie.herbalis.domain.drying.RackVisualState;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.irrigation.FertilizerSilo;
import io.github.zefarie.herbalis.domain.irrigation.SiloVisualState;
import io.github.zefarie.herbalis.domain.irrigation.TankVisualState;
import io.github.zefarie.herbalis.domain.irrigation.WaterTank;
import io.github.zefarie.herbalis.domain.plant.Plant;
import io.github.zefarie.herbalis.infrastructure.config.HerbalisConfig;
import io.github.zefarie.herbalis.infrastructure.world.LampBlocks;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;

import java.util.Optional;
import java.util.UUID;

/**
 * Synchronise le contenu de la base avec les entites du monde :
 * purge des orphelines puis respawn, chunk par chunk.
 */
public final class WorldSync {

    private final DisplayRenderer renderer;
    private final PotRepository pots;
    private final PlantRepository plants;
    private final RackRepository racks;
    private final JarRepository jars;
    private final TankRepository tanks;
    private final SiloRepository silos;
    private final PipeRepository pipes;
    private final LampRepository lamps;
    private final DrugRegistry drugs;
    private final HerbalisConfig config;
    private final PipeLayout layout;

    public WorldSync(DisplayRenderer renderer, PotRepository pots,
                     PlantRepository plants, RackRepository racks,
                     JarRepository jars, TankRepository tanks,
                     SiloRepository silos, PipeRepository pipes,
                     LampRepository lamps, DrugRegistry drugs,
                     HerbalisConfig config, PipeLayout layout) {
        this.renderer = renderer;
        this.pots = pots;
        this.plants = plants;
        this.racks = racks;
        this.jars = jars;
        this.tanks = tanks;
        this.silos = silos;
        this.pipes = pipes;
        this.lamps = lamps;
        this.drugs = drugs;
        this.config = config;
        this.layout = layout;
    }

    /** Purge les entites Herbalis du chunk puis respawn depuis la base. */
    public void respawnChunk(Chunk chunk) {
        renderer.purgeChunk(chunk);
        UUID worldId = chunk.getWorld().getUID();
        long now = System.currentTimeMillis();

        for (BlockPos pos : pots.all()) {
            if (inChunk(pos, worldId, chunk.getX(), chunk.getZ())) {
                Optional<Plant> plant = plants.at(pos);
                var drug = plant.flatMap(p -> drugs.byId(p.drugId()));
                float scale = plant.isPresent() && drug.isPresent()
                        ? PlantVisuals.scaleOf(plant.get(), drug.get())
                        : 1.0f;
                renderer.showPot(pos, plant,
                        plant.map(p -> PlantVisuals.plantModel(
                                p, drug.orElse(null))).orElse(""),
                        PlantVisuals.potModel(plant, drug,
                                pots.hasDripper(pos)), scale);
            }
        }
        for (DryingRack rack : racks.all()) {
            if (inChunk(rack.pos(), worldId, chunk.getX(), chunk.getZ())) {
                RackVisualState state = drugs.byId(rack.drugId())
                        .map(drug -> rack.visualState(now, drug.drying().duration()))
                        .orElse(RackVisualState.EMPTY);
                renderer.showRack(rack.pos(), state);
            }
        }
        for (CuringJar jar : jars.all()) {
            if (inChunk(jar.pos(), worldId, chunk.getX(), chunk.getZ())) {
                JarVisualState state = drugs.byId(jar.drugId())
                        .map(drug -> jar.visualState(now, drug.curing()))
                        .orElse(JarVisualState.EMPTY);
                renderer.showJar(jar.pos(), state);
            }
        }
        for (WaterTank tank : tanks.all()) {
            if (inChunk(tank.pos(), worldId, chunk.getX(), chunk.getZ())) {
                double capacity = config.tankCapacity(tank.size());
                renderer.showTank(tank.pos(), tank.size(),
                        TankVisualState.of(tank.fillRatio(capacity)));
            }
        }
        for (FertilizerSilo silo : silos.all()) {
            if (inChunk(silo.pos(), worldId, chunk.getX(), chunk.getZ())) {
                renderer.showSilo(silo.pos(), SiloVisualState.of(
                        silo.doses(), config.siloCapacityDoses()));
            }
        }
        for (BlockPos pos : pipes.all()) {
            if (inChunk(pos, worldId, chunk.getX(), chunk.getZ())) {
                renderer.showPipe(pos, layout.maskOf(pos));
            }
        }
        for (BlockPos pos : lamps.all()) {
            if (inChunk(pos, worldId, chunk.getX(), chunk.getZ())) {
                boolean enabled = lamps.isEnabled(pos);
                renderer.showLamp(pos, enabled);
                // Le bloc lumineux est persistant, mais on le resynchronise
                // au cas ou (WorldEdit, /fill, reset partiel, etat bascule).
                if (enabled) {
                    LampBlocks.place(pos, config.lampLightLevel());
                } else {
                    LampBlocks.remove(pos);
                }
            }
        }
    }

    public void forgetChunk(Chunk chunk) {
        renderer.forgetChunk(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
    }

    /** Au demarrage et a l'arret : traite tous les chunks deja charges. */
    public void respawnLoadedChunks() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                respawnChunk(chunk);
            }
        }
    }

    public void purgeLoadedChunks() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                renderer.purgeChunk(chunk);
            }
        }
    }

    private static boolean inChunk(BlockPos pos, UUID worldId, int chunkX, int chunkZ) {
        return pos.worldId().equals(worldId)
                && pos.chunkX() == chunkX && pos.chunkZ() == chunkZ;
    }
}
