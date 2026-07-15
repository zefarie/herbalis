package io.github.zefarie.herbalis.infrastructure.listener;

import io.github.zefarie.herbalis.application.port.JarRepository;
import io.github.zefarie.herbalis.application.port.PlantRepository;
import io.github.zefarie.herbalis.application.port.PotRepository;
import io.github.zefarie.herbalis.application.port.RackRepository;
import io.github.zefarie.herbalis.application.usecase.BreakJarUseCase;
import io.github.zefarie.herbalis.application.usecase.BreakPlantUseCase;
import io.github.zefarie.herbalis.application.usecase.BreakPotUseCase;
import io.github.zefarie.herbalis.application.usecase.BreakRackUseCase;
import io.github.zefarie.herbalis.domain.drug.DrugRegistry;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.plant.Plant;
import io.github.zefarie.herbalis.domain.quality.Quality;
import io.github.zefarie.herbalis.infrastructure.config.HerbalisConfig;
import io.github.zefarie.herbalis.infrastructure.config.Messages;
import io.github.zefarie.herbalis.infrastructure.fx.Fx;
import io.github.zefarie.herbalis.infrastructure.item.ItemFactory;
import io.github.zefarie.herbalis.infrastructure.render.DisplayRenderer;
import io.github.zefarie.herbalis.infrastructure.render.PosCodec;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.List;
import java.util.Optional;

/**
 * Protege les emplacements des pots, plantes et racks contre les blocs,
 * l'eau, les pistons et les explosions, et gere la casse du bloc support.
 */
public final class ProtectionListener implements Listener {

    private final HerbalisConfig config;
    private final Messages messages;
    private final Fx fx;
    private final ItemFactory items;
    private final DisplayRenderer renderer;
    private final DrugRegistry drugs;
    private final PotRepository pots;
    private final PlantRepository plants;
    private final RackRepository racks;
    private final JarRepository jars;
    private final BreakPlantUseCase breakPlant;
    private final BreakPotUseCase breakPot;
    private final BreakRackUseCase breakRack;
    private final BreakJarUseCase breakJar;

    public ProtectionListener(HerbalisConfig config, Messages messages, Fx fx,
                              ItemFactory items, DisplayRenderer renderer,
                              DrugRegistry drugs, PotRepository pots,
                              PlantRepository plants, RackRepository racks,
                              JarRepository jars,
                              BreakPlantUseCase breakPlant, BreakPotUseCase breakPot,
                              BreakRackUseCase breakRack, BreakJarUseCase breakJar) {
        this.config = config;
        this.messages = messages;
        this.fx = fx;
        this.items = items;
        this.renderer = renderer;
        this.drugs = drugs;
        this.pots = pots;
        this.plants = plants;
        this.racks = racks;
        this.jars = jars;
        this.breakPlant = breakPlant;
        this.breakPot = breakPot;
        this.breakRack = breakRack;
        this.breakJar = breakJar;
    }

    private boolean occupied(BlockPos pos) {
        return pots.exists(pos) || racks.at(pos).isPresent()
                || jars.at(pos).isPresent();
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (occupied(PosCodec.of(event.getBlock()))) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar(
                    messages.msg("culture.pose-place-occupee"));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onLiquidFlow(BlockFromToEvent event) {
        if (occupied(PosCodec.of(event.getToBlock()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (pistonThreatens(event.getBlocks(), event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (pistonThreatens(event.getBlocks(), null)) {
            event.setCancelled(true);
        }
    }

    private boolean pistonThreatens(List<Block> moved, BlockPistonExtendEvent extend) {
        for (Block block : moved) {
            BlockPos pos = PosCodec.of(block);
            // Un bloc pousse ne doit ni porter une structure, ni finir dedans.
            if (occupied(pos.above())) {
                return true;
            }
            if (extend != null) {
                Block destination = block.getRelative(extend.getDirection());
                if (occupied(PosCodec.of(destination))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Casse du bloc support : la structure posee dessus saute proprement. */
    @EventHandler(ignoreCancelled = true)
    public void onSupportBreak(BlockBreakEvent event) {
        BlockPos above = PosCodec.of(event.getBlock()).above();
        if (pots.exists(above)) {
            popPot(above, config.dropSeedOnBreak());
        } else if (racks.at(above).isPresent()) {
            popRack(above);
        } else if (jars.at(above).isPresent()) {
            popJar(above);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(event.getLocation(), event.blockList());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.getBlock().getLocation(), event.blockList());
    }

    private void handleExplosion(Location center, List<Block> blockList) {
        // Blocs supports souffles : les structures posees dessus sautent.
        for (Block block : blockList) {
            BlockPos above = PosCodec.of(block).above();
            if (pots.exists(above)) {
                popPot(above, config.dropSeedOnBreak());
            } else if (racks.at(above).isPresent()) {
                popRack(above);
            } else if (jars.at(above).isPresent()) {
                popJar(above);
            }
        }
        if (!config.explosionKillsPlants()) {
            return;
        }
        double radiusSq = config.explosionRadius() * config.explosionRadius();
        for (Plant plant : List.copyOf(plants.all())) {
            Optional<Location> loc = PosCodec.center(plant.pos());
            if (loc.isEmpty() || !loc.get().getWorld().equals(center.getWorld())) {
                continue;
            }
            if (loc.get().distanceSquared(center) > radiusSq) {
                continue;
            }
            breakPlant.execute(plant.pos());
            renderer.removePlantVisual(plant.pos());
            PosCodec.corner(plant.pos()).ifPresent(corner -> {
                fx.died(corner);
                if (config.dropSeedOnBreak()) {
                    drugs.byId(plant.drugId()).ifPresent(drug ->
                            corner.getWorld().dropItemNaturally(
                                    corner.clone().add(0.5, 0.6, 0.5),
                                    items.seed(drug,
                                            Quality.of(plant.seedQuality()))));
                }
            });
        }
    }

    private void popPot(BlockPos pos, boolean dropSeed) {
        BreakPotUseCase.Result result = breakPot.execute(pos);
        if (!result.potExisted()) {
            return;
        }
        renderer.removePotVisual(pos);
        PosCodec.corner(pos).ifPresent(loc -> {
            fx.broken(loc);
            Location dropAt = loc.clone().add(0.5, 0.4, 0.5);
            loc.getWorld().dropItemNaturally(dropAt, items.pot());
            result.plant()
                    .filter(plant -> dropSeed && !plant.isDead())
                    .ifPresent(plant -> drugs.byId(plant.drugId())
                            .ifPresent(drug -> loc.getWorld().dropItemNaturally(
                                    dropAt, items.seed(drug,
                                            Quality.of(plant.seedQuality())))));
        });
    }

    private void popRack(BlockPos pos) {
        var rack = breakRack.execute(pos).orElse(null);
        if (rack == null) {
            return;
        }
        renderer.removeRackVisual(pos);
        PosCodec.corner(pos).ifPresent(loc -> {
            fx.rackBroken(loc);
            Location dropAt = loc.clone().add(0.5, 0.4, 0.5);
            loc.getWorld().dropItemNaturally(dropAt, items.dryingRack());
            drugs.byId(rack.drugId()).ifPresent(drug ->
                    rack.slots().forEach(slot -> loc.getWorld()
                            .dropItemNaturally(dropAt, items.freshBud(drug, slot.quality()))));
        });
    }

    private void popJar(BlockPos pos) {
        var jar = breakJar.execute(pos).orElse(null);
        if (jar == null) {
            return;
        }
        renderer.removeJarVisual(pos);
        PosCodec.corner(pos).ifPresent(loc -> {
            fx.jarBroken(loc);
            Location dropAt = loc.clone().add(0.5, 0.4, 0.5);
            loc.getWorld().dropItemNaturally(dropAt, items.curingJar());
            drugs.byId(jar.drugId()).ifPresent(drug ->
                    jar.slots().forEach(slot -> loc.getWorld()
                            .dropItemNaturally(dropAt, items.dried(drug, slot.quality()))));
        });
    }
}
