package io.github.zefarie.herbalis.infrastructure.listener;

import io.github.zefarie.herbalis.application.service.IrrigationService;
import io.github.zefarie.herbalis.application.service.OccupancyService;
import io.github.zefarie.herbalis.application.usecase.PlaceJarUseCase;
import io.github.zefarie.herbalis.application.usecase.PlaceLampUseCase;
import io.github.zefarie.herbalis.application.usecase.PlacePipeUseCase;
import io.github.zefarie.herbalis.application.usecase.PlacePotUseCase;
import io.github.zefarie.herbalis.application.usecase.PlaceRackUseCase;
import io.github.zefarie.herbalis.application.usecase.PlaceSiloUseCase;
import io.github.zefarie.herbalis.application.usecase.PlaceTankUseCase;
import io.github.zefarie.herbalis.domain.curing.JarVisualState;
import io.github.zefarie.herbalis.domain.drug.DrugRegistry;
import io.github.zefarie.herbalis.domain.drying.RackVisualState;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.irrigation.SiloVisualState;
import io.github.zefarie.herbalis.domain.irrigation.TankSize;
import io.github.zefarie.herbalis.domain.irrigation.TankVisualState;
import io.github.zefarie.herbalis.domain.quality.Quality;
import io.github.zefarie.herbalis.infrastructure.config.HerbalisConfig;
import io.github.zefarie.herbalis.infrastructure.config.Messages;
import io.github.zefarie.herbalis.infrastructure.fx.Fx;
import io.github.zefarie.herbalis.infrastructure.item.HerbalisItemType;
import io.github.zefarie.herbalis.infrastructure.item.ItemFactory;
import io.github.zefarie.herbalis.infrastructure.item.ItemKeys;
import io.github.zefarie.herbalis.infrastructure.render.DisplayRenderer;
import io.github.zefarie.herbalis.infrastructure.render.PipeLayout;
import io.github.zefarie.herbalis.infrastructure.render.PosCodec;
import io.github.zefarie.herbalis.infrastructure.world.LampBlocks;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Optional;

/**
 * Usages d'items dans le monde : pose de pot et de rack, remplissage de
 * l'arrosoir, conditionnement des pochons.
 */
public final class ItemUseListener implements Listener {

    private final Messages messages;
    private final Fx fx;
    private final ItemFactory items;
    private final DisplayRenderer renderer;
    private final DrugRegistry drugs;
    private final HerbalisConfig config;
    private final OccupancyService occupancy;
    private final IrrigationService irrigation;
    private final PipeLayout layout;
    private final PlacePotUseCase placePot;
    private final PlaceRackUseCase placeRack;
    private final PlaceJarUseCase placeJar;
    private final PlacePipeUseCase placePipe;
    private final PlaceTankUseCase placeTank;
    private final PlaceSiloUseCase placeSilo;
    private final PlaceLampUseCase placeLamp;

    public ItemUseListener(Messages messages, Fx fx, ItemFactory items,
                           DisplayRenderer renderer, DrugRegistry drugs,
                           HerbalisConfig config, OccupancyService occupancy,
                           IrrigationService irrigation, PipeLayout layout,
                           PlacePotUseCase placePot, PlaceRackUseCase placeRack,
                           PlaceJarUseCase placeJar, PlacePipeUseCase placePipe,
                           PlaceTankUseCase placeTank, PlaceSiloUseCase placeSilo,
                           PlaceLampUseCase placeLamp) {
        this.messages = messages;
        this.fx = fx;
        this.items = items;
        this.renderer = renderer;
        this.drugs = drugs;
        this.config = config;
        this.occupancy = occupancy;
        this.irrigation = irrigation;
        this.layout = layout;
        this.placePot = placePot;
        this.placeRack = placeRack;
        this.placeJar = placeJar;
        this.placePipe = placePipe;
        this.placeTank = placeTank;
        this.placeSilo = placeSilo;
        this.placeLamp = placeLamp;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ItemStack held = event.getItem();
        Optional<HerbalisItemType> type = ItemKeys.typeOf(held);
        if (type.isEmpty()) {
            return;
        }
        Player player = event.getPlayer();

        switch (type.get()) {
            case POT, DRYING_RACK, CURING_JAR, TANK_CUVE, TANK_CITERNE,
                 TANK_RESERVOIR, SILO, UV_LAMP -> {
                if (event.getAction() != Action.RIGHT_CLICK_BLOCK
                        || event.getClickedBlock() == null) {
                    return;
                }
                event.setCancelled(true);
                placeStructure(player, held, type.get(),
                        event.getClickedBlock(), event.getBlockFace());
            }
            case PIPE -> {
                if (event.getAction() != Action.RIGHT_CLICK_BLOCK
                        || event.getClickedBlock() == null) {
                    return;
                }
                event.setCancelled(true);
                placePipe(player, held,
                        event.getClickedBlock(), event.getBlockFace());
            }
            case WATERING_CAN, SPRAYER -> {
                if (event.getAction() != Action.RIGHT_CLICK_BLOCK
                        || event.getClickedBlock() == null) {
                    return;
                }
                if (isWater(event.getClickedBlock())
                        || isWater(event.getClickedBlock().getRelative(event.getBlockFace()))) {
                    event.setCancelled(true);
                    refill(player, held);
                }
            }
            case POUCH_EMPTY -> {
                if (event.getAction() != Action.RIGHT_CLICK_AIR
                        && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
                    return;
                }
                event.setCancelled(true);
                fillPouch(player, held);
            }
            default -> {
                // Les autres items s'utilisent sur les entites Interaction.
            }
        }
    }

    private void placeStructure(Player player, ItemStack held, HerbalisItemType type,
                                Block clicked, BlockFace face) {
        if (!player.hasPermission("herbalis.plant")) {
            player.sendMessage(messages.msg("erreurs.permission"));
            return;
        }
        if (face != BlockFace.UP || !clicked.isSolid()) {
            player.sendActionBar(messages.msg("culture.pose-surface-invalide"));
            return;
        }
        Block target = clicked.getRelative(BlockFace.UP);
        if (!target.getType().isAir()) {
            player.sendActionBar(messages.msg("culture.pose-place-occupee"));
            return;
        }
        BlockPos pos = PosCodec.of(target);
        if (occupancy.occupied(pos)) {
            player.sendActionBar(messages.msg("culture.pose-place-occupee"));
            return;
        }

        boolean placed = switch (type) {
            case POT -> placePot.execute(pos);
            case DRYING_RACK -> placeRack.execute(pos).isPresent();
            case CURING_JAR -> placeJar.execute(pos).isPresent();
            case TANK_CUVE, TANK_CITERNE, TANK_RESERVOIR ->
                    placeTank.execute(pos, ItemFactory.tankSizeOf(type)
                            .orElseThrow()).isPresent();
            case SILO -> placeSilo.execute(pos).isPresent();
            case UV_LAMP -> placeLamp.execute(pos);
            default -> false;
        };
        if (!placed) {
            player.sendActionBar(messages.msg("culture.pose-place-occupee"));
            return;
        }

        held.subtract();
        PosCodec.corner(pos).ifPresent(loc -> {
            switch (type) {
                case POT -> {
                    renderer.showPot(pos, Optional.empty(), "", "pot", 1.0f);
                    // Un pot est un point de branchement : le reseau change.
                    layout.refreshAround(pos);
                    irrigation.invalidate();
                    fx.potPlaced(loc);
                    player.sendActionBar(messages.msg("culture.pot-pose"));
                }
                case DRYING_RACK -> {
                    renderer.showRack(pos, RackVisualState.EMPTY);
                    fx.rackPlaced(loc);
                    player.sendActionBar(messages.msg("sechage.rack-pose"));
                }
                case CURING_JAR -> {
                    renderer.showJar(pos, JarVisualState.EMPTY);
                    fx.jarPlaced(loc);
                    player.sendActionBar(messages.msg("curing.jarre-posee"));
                }
                case TANK_CUVE, TANK_CITERNE, TANK_RESERVOIR -> {
                    TankSize size = ItemFactory.tankSizeOf(type).orElseThrow();
                    renderer.showTank(pos, size, TankVisualState.EMPTY);
                    layout.refreshAround(pos);
                    irrigation.invalidate();
                    fx.tankPlaced(loc);
                    player.sendActionBar(messages.msg("irrigation.caisson-pose"));
                }
                case SILO -> {
                    renderer.showSilo(pos, SiloVisualState.EMPTY);
                    layout.refreshAround(pos);
                    irrigation.invalidate();
                    fx.siloPlaced(loc);
                    player.sendActionBar(messages.msg("irrigation.silo-pose"));
                }
                case UV_LAMP -> {
                    renderer.showLamp(pos);
                    LampBlocks.place(pos, config.lampLightLevel());
                    fx.lampPlaced(loc);
                    player.sendActionBar(messages.msg("lampe.posee"));
                }
                default -> {
                }
            }
        });
        player.swingMainHand();
    }

    /** Les tuyaux se posent sur n'importe quelle face et peuvent flotter. */
    private void placePipe(Player player, ItemStack held, Block clicked,
                           BlockFace face) {
        if (!player.hasPermission("herbalis.plant")) {
            player.sendMessage(messages.msg("erreurs.permission"));
            return;
        }
        Block target = clicked.getRelative(face);
        if (!target.getType().isAir()) {
            player.sendActionBar(messages.msg("culture.pose-place-occupee"));
            return;
        }
        BlockPos pos = PosCodec.of(target);
        if (occupancy.occupied(pos) || !placePipe.execute(pos)) {
            player.sendActionBar(messages.msg("culture.pose-place-occupee"));
            return;
        }
        held.subtract();
        renderer.showPipe(pos, layout.maskOf(pos));
        layout.refreshAround(pos);
        irrigation.invalidate();
        PosCodec.corner(pos).ifPresent(fx::pipePlaced);
        player.sendActionBar(messages.msg("irrigation.tuyau-pose"));
        player.swingMainHand();
    }

    /** Arrosoir ou pulverisateur : un plein d'eau remet les charges. */
    private void refill(Player player, ItemStack tool) {
        Integer damage = tool.getData(DataComponentTypes.DAMAGE);
        if (damage == null || damage == 0) {
            player.sendActionBar(messages.msg("arrosage.deja-plein"));
            return;
        }
        tool.setData(DataComponentTypes.DAMAGE, 0);
        fx.canRefilled(player.getLocation());
        player.sendActionBar(messages.msg("arrosage.rempli"));
        player.swingMainHand();
    }

    /**
     * Conditionnement : le pochon vide se remplit avec la meilleure weed
     * sechee de l'inventaire. La qualite est heritee.
     */
    private void fillPouch(Player player, ItemStack pouch) {
        if (!player.hasPermission("herbalis.harvest")) {
            player.sendMessage(messages.msg("erreurs.permission"));
            return;
        }
        PlayerInventory inventory = player.getInventory();
        ItemStack best = null;
        Quality bestQuality = null;
        for (ItemStack stack : inventory.getStorageContents()) {
            if (ItemKeys.typeOf(stack).filter(t -> t == HerbalisItemType.DRIED).isEmpty()) {
                continue;
            }
            Quality quality = ItemKeys.qualityOf(stack).orElse(Quality.of(1));
            if (bestQuality == null || quality.compareTo(bestQuality) > 0) {
                best = stack;
                bestQuality = quality;
            }
        }
        if (best == null) {
            player.sendActionBar(messages.msg("conditionnement.rien-a-emballer"));
            return;
        }
        var drug = ItemKeys.drugOf(best).flatMap(drugs::byId).orElse(null);
        if (drug == null) {
            return;
        }
        best.subtract();
        pouch.subtract();
        ItemStack filled = items.pouch(drug, bestQuality);
        player.getInventory().addItem(filled).values().forEach(rest ->
                player.getWorld().dropItemNaturally(player.getLocation(), rest));
        fx.pouchFilled(player);
        player.sendActionBar(messages.msg("conditionnement.pochon-rempli",
                Messages.ph("etoiles", messages.deserialize(items.starsMarkup(bestQuality)))));
    }

    private static boolean isWater(Block block) {
        if (block.getType() == Material.WATER) {
            return true;
        }
        return block.getBlockData() instanceof Waterlogged waterlogged
                && waterlogged.isWaterlogged();
    }
}
