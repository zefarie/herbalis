package io.github.zefarie.herbalis.infrastructure.listener;

import io.github.zefarie.herbalis.application.port.JarRepository;
import io.github.zefarie.herbalis.application.port.PlantRepository;
import io.github.zefarie.herbalis.application.port.PotRepository;
import io.github.zefarie.herbalis.application.port.RackRepository;
import io.github.zefarie.herbalis.application.usecase.AddBudToRackUseCase;
import io.github.zefarie.herbalis.application.usecase.AddToJarUseCase;
import io.github.zefarie.herbalis.application.usecase.BreakJarUseCase;
import io.github.zefarie.herbalis.application.usecase.BreakPlantUseCase;
import io.github.zefarie.herbalis.application.usecase.BreakPotUseCase;
import io.github.zefarie.herbalis.application.usecase.BreakRackUseCase;
import io.github.zefarie.herbalis.application.usecase.CollectJarUseCase;
import io.github.zefarie.herbalis.application.usecase.CollectRackUseCase;
import io.github.zefarie.herbalis.application.usecase.FertilizePlantUseCase;
import io.github.zefarie.herbalis.application.usecase.HarvestPlantUseCase;
import io.github.zefarie.herbalis.application.usecase.PlantSeedUseCase;
import io.github.zefarie.herbalis.application.usecase.PrunePlantUseCase;
import io.github.zefarie.herbalis.application.usecase.TreatPlantUseCase;
import io.github.zefarie.herbalis.application.usecase.WaterPlantUseCase;
import io.github.zefarie.herbalis.domain.curing.JarVisualState;
import io.github.zefarie.herbalis.domain.drug.DrugRegistry;
import io.github.zefarie.herbalis.domain.drug.DrugType;
import io.github.zefarie.herbalis.domain.drying.RackVisualState;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.plant.GrowthEngine;
import io.github.zefarie.herbalis.domain.plant.Plant;
import io.github.zefarie.herbalis.domain.quality.Quality;
import io.github.zefarie.herbalis.infrastructure.config.HerbalisConfig;
import io.github.zefarie.herbalis.infrastructure.config.Messages;
import io.github.zefarie.herbalis.infrastructure.fx.Fx;
import io.github.zefarie.herbalis.infrastructure.hud.HudService;
import io.github.zefarie.herbalis.infrastructure.item.HerbalisItemType;
import io.github.zefarie.herbalis.infrastructure.item.ItemFactory;
import io.github.zefarie.herbalis.infrastructure.item.ItemKeys;
import io.github.zefarie.herbalis.infrastructure.render.DisplayRenderer;
import io.github.zefarie.herbalis.infrastructure.render.PlantVisuals;
import io.github.zefarie.herbalis.infrastructure.render.PosCodec;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Interactions avec les hitbox des pots, plantes et racks : planter,
 * arroser, fertiliser, recolter, secher, casser. Chaque action a son
 * retour visuel et sonore.
 */
public final class PlantInteractListener implements Listener {

    private final Messages messages;
    private final Fx fx;
    private final ItemFactory items;
    private final DisplayRenderer renderer;
    private final DrugRegistry drugs;
    private final HerbalisConfig config;
    private final HudService hud;
    private final PlantRepository plants;
    private final PotRepository pots;
    private final RackRepository racks;
    private final JarRepository jars;
    private final PlantSeedUseCase plantSeed;
    private final WaterPlantUseCase waterPlant;
    private final FertilizePlantUseCase fertilizePlant;
    private final PrunePlantUseCase prunePlant;
    private final TreatPlantUseCase treatPlant;
    private final HarvestPlantUseCase harvestPlant;
    private final BreakPlantUseCase breakPlant;
    private final BreakPotUseCase breakPot;
    private final AddBudToRackUseCase addBud;
    private final CollectRackUseCase collectRack;
    private final BreakRackUseCase breakRack;
    private final AddToJarUseCase addToJar;
    private final CollectJarUseCase collectJar;
    private final BreakJarUseCase breakJar;

    public PlantInteractListener(Messages messages, Fx fx, ItemFactory items,
                                 DisplayRenderer renderer, DrugRegistry drugs,
                                 HerbalisConfig config, HudService hud,
                                 PlantRepository plants, PotRepository pots,
                                 RackRepository racks, JarRepository jars,
                                 PlantSeedUseCase plantSeed, WaterPlantUseCase waterPlant,
                                 FertilizePlantUseCase fertilizePlant,
                                 PrunePlantUseCase prunePlant,
                                 TreatPlantUseCase treatPlant,
                                 HarvestPlantUseCase harvestPlant,
                                 BreakPlantUseCase breakPlant, BreakPotUseCase breakPot,
                                 AddBudToRackUseCase addBud, CollectRackUseCase collectRack,
                                 BreakRackUseCase breakRack, AddToJarUseCase addToJar,
                                 CollectJarUseCase collectJar, BreakJarUseCase breakJar) {
        this.messages = messages;
        this.fx = fx;
        this.items = items;
        this.renderer = renderer;
        this.drugs = drugs;
        this.config = config;
        this.hud = hud;
        this.plants = plants;
        this.pots = pots;
        this.racks = racks;
        this.jars = jars;
        this.plantSeed = plantSeed;
        this.waterPlant = waterPlant;
        this.fertilizePlant = fertilizePlant;
        this.prunePlant = prunePlant;
        this.treatPlant = treatPlant;
        this.harvestPlant = harvestPlant;
        this.breakPlant = breakPlant;
        this.breakPot = breakPot;
        this.addBud = addBud;
        this.collectRack = collectRack;
        this.breakRack = breakRack;
        this.addToJar = addToJar;
        this.collectJar = collectJar;
        this.breakJar = breakJar;
    }

    // ----------------------------------------------------------------
    // Clic droit
    // ----------------------------------------------------------------

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
                || !(event.getRightClicked() instanceof Interaction interaction)) {
            return;
        }
        Optional<String> marker = renderer.markerOf(interaction);
        Optional<BlockPos> pos = renderer.posOf(interaction);
        if (marker.isEmpty() || pos.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();

        switch (marker.get()) {
            case "pot" -> potRightClick(player, pos.get(), now);
            case "rack" -> rackRightClick(player, pos.get(), now);
            case "jar" -> jarRightClick(player, pos.get(), now);
            default -> {
            }
        }
    }

    private void potRightClick(Player player, BlockPos pos, long now) {
        ItemStack held = player.getInventory().getItemInMainHand();
        Optional<HerbalisItemType> heldType = ItemKeys.typeOf(held);
        Optional<Plant> plant = plants.at(pos);
        Location loc = PosCodec.corner(pos).orElse(null);
        if (loc == null) {
            return;
        }

        // Plante morte : n'importe quel clic droit l'arrache.
        if (plant.isPresent() && plant.get().isDead()) {
            breakPlant.execute(pos);
            renderer.removePlantVisual(pos);
            renderer.updatePotModel(pos, emptyPotModel(pos));
            fx.died(loc);
            player.sendActionBar(messages.msg("culture.plante-morte-arrachee"));
            return;
        }

        if (heldType.filter(t -> t == HerbalisItemType.SEED).isPresent()) {
            plantSeedAction(player, pos, held, loc, now);
            return;
        }
        if (heldType.filter(t -> t == HerbalisItemType.WATERING_CAN).isPresent()) {
            waterAction(player, pos, held, loc);
            return;
        }
        if (heldType.filter(t -> t == HerbalisItemType.FERTILIZER).isPresent()) {
            fertilizeAction(player, pos, held, loc);
            return;
        }
        if (heldType.filter(t -> t == HerbalisItemType.SPRAYER).isPresent()) {
            sprayAction(player, pos, held, loc);
            return;
        }
        if (heldType.filter(t -> t == HerbalisItemType.DRIPPER).isPresent()) {
            installDripperAction(player, pos, held, loc, plant);
            return;
        }
        // Cisailles vanilla : on taille, sauf au stade final ou elles
        // servent naturellement a couper la recolte.
        if (held.getType() == Material.SHEARS && !isHarvestable(plant)) {
            pruneAction(player, pos, held, loc);
            return;
        }
        if (isHarvestable(plant)) {
            harvestAction(player, pos, loc);
            return;
        }
        // Rien d'actionnable : l'hologramme au-dessus de la cible montre
        // deja l'etat complet.
    }

    private void plantSeedAction(Player player, BlockPos pos, ItemStack seed,
                                 Location loc, long now) {
        if (!player.hasPermission("herbalis.plant")) {
            player.sendMessage(messages.msg("erreurs.permission"));
            return;
        }
        String drugId = ItemKeys.drugOf(seed).orElse("");
        int seedQuality = ItemKeys.qualityOf(seed).map(Quality::stars)
                .orElse(Plant.DEFAULT_SEED_QUALITY);
        switch (plantSeed.execute(pos, drugId, seedQuality, now)) {
            case PlantSeedUseCase.Result.Success success -> {
                seed.subtract();
                DrugType drug = drugs.byId(drugId).orElseThrow();
                renderer.spawnPlantWithPop(pos, success.plant(),
                        PlantVisuals.plantModel(success.plant(), drug),
                        PlantVisuals.scaleOf(success.plant(), drug));
                fx.planted(loc);
                player.sendActionBar(messages.msg("culture.graine-plantee",
                        Messages.ph("drogue", drug.displayName())));
            }
            case PlantSeedUseCase.Result.AlreadyPlanted ignored ->
                    player.sendActionBar(messages.msg("culture.deja-plante"));
            default -> {
            }
        }
    }

    private void waterAction(Player player, BlockPos pos, ItemStack can, Location loc) {
        Integer damage = can.getData(DataComponentTypes.DAMAGE);
        Integer maxDamage = can.getData(DataComponentTypes.MAX_DAMAGE);
        if (damage != null && maxDamage != null && damage >= maxDamage) {
            player.sendActionBar(messages.msg("arrosage.arrosoir-vide"));
            return;
        }
        switch (waterPlant.execute(pos)) {
            case WaterPlantUseCase.Result.Success success -> {
                if (damage != null) {
                    can.setData(DataComponentTypes.DAMAGE, damage + 1);
                }
                renderer.pulsePlant(pos);
                // Le terreau fonce immediatement, le soin se voit.
                renderer.updatePotModel(pos, PlantVisuals.potModel(
                        Optional.of(success.plant()),
                        drugs.byId(success.plant().drugId()),
                        pots.hasDripper(pos)));
                fx.watered(loc);
                player.sendActionBar(messages.msg("arrosage.arrosee"));
            }
            case WaterPlantUseCase.Result.AlreadyMoist ignored ->
                    player.sendActionBar(messages.msg("arrosage.deja-humide"));
            case WaterPlantUseCase.Result.NoPlant ignored ->
                    player.sendActionBar(messages.msg("culture.pot-vide-info"));
            case WaterPlantUseCase.Result.PlantDead ignored ->
                    player.sendActionBar(messages.msg("culture.plante-morte-info"));
        }
    }

    private void fertilizeAction(Player player, BlockPos pos, ItemStack fertilizer,
                                 Location loc) {
        switch (fertilizePlant.execute(pos)) {
            case FertilizePlantUseCase.Result.Success success -> {
                fertilizer.subtract();
                renderer.pulsePlant(pos);
                renderer.updatePotModel(pos, PlantVisuals.potModel(
                        Optional.of(success.plant()),
                        drugs.byId(success.plant().drugId()),
                        pots.hasDripper(pos)));
                fx.fertilized(loc);
                player.sendActionBar(messages.msg("engrais.applique"));
            }
            case FertilizePlantUseCase.Result.AlreadyFertilized ignored ->
                    player.sendActionBar(messages.msg("engrais.deja-applique"));
            case FertilizePlantUseCase.Result.NoPlant ignored ->
                    player.sendActionBar(messages.msg("culture.pot-vide-info"));
            case FertilizePlantUseCase.Result.PlantDead ignored ->
                    player.sendActionBar(messages.msg("culture.plante-morte-info"));
        }
    }

    private void harvestAction(Player player, BlockPos pos, Location loc) {
        if (!player.hasPermission("herbalis.harvest")) {
            player.sendMessage(messages.msg("erreurs.permission"));
            return;
        }
        if (!(harvestPlant.execute(pos) instanceof HarvestPlantUseCase.Result.Success success)) {
            return;
        }
        DrugType drug = drugs.byId(success.drugId()).orElseThrow();
        renderer.removePlantVisual(pos);
        renderer.updatePotModel(pos, emptyPotModel(pos));
        fx.harvested(loc);
        Location dropAt = loc.clone().add(0.5, 0.8, 0.5);
        for (int i = 0; i < success.yield(); i++) {
            loc.getWorld().dropItemNaturally(dropAt,
                    items.freshBud(drug, success.quality()));
        }
        // Genetique : la plante rend des graines heritees de sa qualite.
        for (Quality seedQuality : success.seeds()) {
            loc.getWorld().dropItemNaturally(dropAt, items.seed(drug, seedQuality));
        }
        player.sendActionBar(messages.msg(
                success.optimal() ? "recolte.optimale" : "recolte.tardive",
                Messages.ph("nombre", String.valueOf(success.yield())),
                Messages.ph("graines", String.valueOf(success.seeds().size())),
                Messages.ph("etoiles",
                        messages.deserialize(items.starsMarkup(success.quality())))));
    }

    /** Installe le goutte-a-goutte : la perte d'eau du pot ralentit. */
    private void installDripperAction(Player player, BlockPos pos, ItemStack dripper,
                                      Location loc, Optional<Plant> plant) {
        if (!player.hasPermission("herbalis.plant")) {
            player.sendMessage(messages.msg("erreurs.permission"));
            return;
        }
        if (pots.hasDripper(pos)) {
            player.sendActionBar(messages.msg("culture.goutte-deja"));
            return;
        }
        pots.setDripper(pos, true);
        dripper.subtract();
        renderer.updatePotModel(pos, PlantVisuals.potModel(
                plant, plant.flatMap(p -> drugs.byId(p.drugId())), true));
        fx.dripperInstalled(loc);
        player.sendActionBar(messages.msg("culture.goutte-installee"));
    }

    private void sprayAction(Player player, BlockPos pos, ItemStack sprayer,
                             Location loc) {
        Integer damage = sprayer.getData(DataComponentTypes.DAMAGE);
        Integer maxDamage = sprayer.getData(DataComponentTypes.MAX_DAMAGE);
        if (damage != null && maxDamage != null && damage >= maxDamage) {
            player.sendActionBar(messages.msg("nuisibles.pulverisateur-vide"));
            return;
        }
        switch (treatPlant.execute(pos)) {
            case TreatPlantUseCase.Result.Treated ignored -> {
                if (damage != null) {
                    sprayer.setData(DataComponentTypes.DAMAGE, damage + 1);
                }
                renderer.pulsePlant(pos);
                fx.sprayed(loc);
                player.sendActionBar(messages.msg("nuisibles.traitee"));
            }
            case TreatPlantUseCase.Result.NotInfested ignored ->
                    player.sendActionBar(messages.msg("nuisibles.plante-saine"));
            case TreatPlantUseCase.Result.NoPlant ignored ->
                    player.sendActionBar(messages.msg("culture.pot-vide-info"));
            case TreatPlantUseCase.Result.PlantDead ignored ->
                    player.sendActionBar(messages.msg("culture.plante-morte-info"));
        }
    }

    private boolean isHarvestable(Optional<Plant> plant) {
        return plant.isPresent() && drugs.byId(plant.get().drugId())
                .map(drug -> GrowthEngine.isHarvestable(plant.get(), drug))
                .orElse(false);
    }

    /** Modele d'un pot vide, goutte-a-goutte compris. */
    private String emptyPotModel(BlockPos pos) {
        return PlantVisuals.potModel(Optional.empty(), Optional.empty(),
                pots.hasDripper(pos));
    }

    private void pruneAction(Player player, BlockPos pos, ItemStack shears,
                             Location loc) {
        if (!player.hasPermission("herbalis.plant")) {
            player.sendMessage(messages.msg("erreurs.permission"));
            return;
        }
        PrunePlantUseCase.Result result = prunePlant.execute(pos);
        switch (result) {
            case PrunePlantUseCase.Result.Topped success -> {
                wearShears(player, shears);
                DrugType drug = drugs.byId(success.plant().drugId()).orElse(null);
                if (drug != null) {
                    // La coupe se voit : la plante revient un peu en arriere.
                    renderer.updatePlant(pos,
                            PlantVisuals.plantModel(success.plant(), drug), true,
                            PlantVisuals.scaleOf(success.plant(), drug));
                }
                fx.pruned(loc);
                player.sendActionBar(messages.msg("taille.reussie"));
            }
            case PrunePlantUseCase.Result.Missed ignored -> {
                wearShears(player, shears);
                fx.pruneMissed(loc);
                player.sendActionBar(messages.msg("taille.ratee"));
            }
            case PrunePlantUseCase.Result.AlreadyTopped ignored ->
                    player.sendActionBar(messages.msg("taille.deja-taillee"));
            case PrunePlantUseCase.Result.PlantDead ignored ->
                    player.sendActionBar(messages.msg("culture.plante-morte-info"));
            case PrunePlantUseCase.Result.NoPlant ignored ->
                    player.sendActionBar(messages.msg("culture.pot-vide-info"));
        }
    }

    /** Usure vanilla des cisailles : unbreaking, casse et son inclus. */
    private void wearShears(Player player, ItemStack shears) {
        int wear = config.shearsWearPerPruning();
        if (wear > 0) {
            shears.damage(wear, player);
        }
    }

    private void rackRightClick(Player player, BlockPos pos, long now) {
        ItemStack held = player.getInventory().getItemInMainHand();
        Optional<HerbalisItemType> heldType = ItemKeys.typeOf(held);
        Location loc = PosCodec.corner(pos).orElse(null);
        if (loc == null) {
            return;
        }

        if (heldType.filter(t -> t == HerbalisItemType.BUD_FRESH).isPresent()) {
            if (!player.hasPermission("herbalis.harvest")) {
                player.sendMessage(messages.msg("erreurs.permission"));
                return;
            }
            String drugId = ItemKeys.drugOf(held).orElse("");
            Quality quality = ItemKeys.qualityOf(held).orElse(Quality.of(1));
            switch (addBud.execute(pos, drugId, quality, now)) {
                case AddBudToRackUseCase.Result.Success success -> {
                    held.subtract();
                    renderer.updateRack(pos, RackVisualState.DRYING);
                    fx.rackAdd(loc);
                    DrugType drug = drugs.byId(drugId).orElseThrow();
                    player.sendActionBar(messages.msg("sechage.tete-deposee",
                            Messages.ph("nombre", String.valueOf(success.rack().slots().size())),
                            Messages.ph("capacite",
                                    String.valueOf(drug.drying().capacity()))));
                }
                case AddBudToRackUseCase.Result.RackFull ignored ->
                        player.sendActionBar(messages.msg("sechage.rack-plein"));
                case AddBudToRackUseCase.Result.MixedDrugs ignored ->
                        player.sendActionBar(messages.msg("sechage.melange-interdit"));
                case AddBudToRackUseCase.Result.NoRack ignored -> {
                }
            }
            return;
        }

        var rack = racks.at(pos).orElse(null);
        if (rack == null || rack.isEmpty()) {
            player.sendActionBar(messages.msg("hud.rack-vide"));
            return;
        }
        DrugType drug = drugs.byId(rack.drugId()).orElse(null);
        boolean ready = drug != null && rack.isReady(now, drug.drying().duration());
        if (!ready && !player.isSneaking()) {
            // Pas encore sec : on montre la progression, sneak pour forcer.
            hud.buildLine(pos, now).ifPresent(player::sendActionBar);
            return;
        }
        if (!player.hasPermission("herbalis.harvest")) {
            player.sendMessage(messages.msg("erreurs.permission"));
            return;
        }
        if (!(collectRack.execute(pos, now) instanceof CollectRackUseCase.Result.Success success)) {
            return;
        }
        DrugType dried = drugs.byId(success.drugId()).orElseThrow();
        Location dropAt = loc.clone().add(0.5, 0.7, 0.5);
        for (Quality quality : success.qualities()) {
            loc.getWorld().dropItemNaturally(dropAt, items.dried(dried, quality));
        }
        renderer.updateRack(pos, RackVisualState.EMPTY);
        fx.rackCollect(loc, success.anyEarly());
        player.sendActionBar(messages.msg(success.anyEarly()
                ? "sechage.recupere-trop-tot" : "sechage.recupere"));
    }

    private void jarRightClick(Player player, BlockPos pos, long now) {
        ItemStack held = player.getInventory().getItemInMainHand();
        Optional<HerbalisItemType> heldType = ItemKeys.typeOf(held);
        Location loc = PosCodec.corner(pos).orElse(null);
        if (loc == null) {
            return;
        }

        if (heldType.filter(t -> t == HerbalisItemType.DRIED).isPresent()) {
            if (!player.hasPermission("herbalis.harvest")) {
                player.sendMessage(messages.msg("erreurs.permission"));
                return;
            }
            String drugId = ItemKeys.drugOf(held).orElse("");
            Quality quality = ItemKeys.qualityOf(held).orElse(Quality.of(1));
            switch (addToJar.execute(pos, drugId, quality, now)) {
                case AddToJarUseCase.Result.Success success -> {
                    held.subtract();
                    renderer.updateJar(pos, JarVisualState.CURING);
                    fx.jarAdd(loc);
                    DrugType drug = drugs.byId(drugId).orElseThrow();
                    player.sendActionBar(messages.msg("curing.tete-deposee",
                            Messages.ph("nombre",
                                    String.valueOf(success.jar().slots().size())),
                            Messages.ph("capacite",
                                    String.valueOf(drug.curing().capacity()))));
                }
                case AddToJarUseCase.Result.JarFull ignored ->
                        player.sendActionBar(messages.msg("curing.jarre-pleine"));
                case AddToJarUseCase.Result.MixedDrugs ignored ->
                        player.sendActionBar(messages.msg("sechage.melange-interdit"));
                case AddToJarUseCase.Result.Moldy ignored ->
                        player.sendActionBar(messages.msg("curing.jarre-moisie-info"));
                case AddToJarUseCase.Result.NoJar ignored -> {
                }
            }
            return;
        }

        var jar = jars.at(pos).orElse(null);
        if (jar == null || jar.isEmpty()) {
            player.sendActionBar(messages.msg("hud.jarre-vide"));
            return;
        }
        DrugType drug = drugs.byId(jar.drugId()).orElse(null);
        boolean ready = drug != null && jar.isReady(now, drug.curing());
        boolean moldy = drug != null && jar.isMoldy(now, drug.curing());
        if (!ready && !moldy && !player.isSneaking()) {
            // Affinage en cours : progression, sneak pour forcer.
            hud.buildLine(pos, now).ifPresent(player::sendActionBar);
            return;
        }
        if (!player.hasPermission("herbalis.harvest")) {
            player.sendMessage(messages.msg("erreurs.permission"));
            return;
        }
        if (!(collectJar.execute(pos, now) instanceof CollectJarUseCase.Result.Success success)) {
            return;
        }
        DrugType cured = drugs.byId(success.drugId()).orElseThrow();
        Location dropAt = loc.clone().add(0.5, 0.6, 0.5);
        for (Quality quality : success.qualities()) {
            loc.getWorld().dropItemNaturally(dropAt, items.dried(cured, quality));
        }
        renderer.updateJar(pos, JarVisualState.EMPTY);
        fx.jarCollect(loc, success.moldy());
        String key = success.moldy() ? "curing.recupere-moisi"
                : success.anyEarly() ? "curing.recupere-trop-tot" : "curing.recupere";
        player.sendActionBar(messages.msg(key));
    }

    // ----------------------------------------------------------------
    // Clic gauche
    // ----------------------------------------------------------------

    @EventHandler
    public void onAttack(PrePlayerAttackEntityEvent event) {
        Entity attacked = event.getAttacked();
        if (!(attacked instanceof Interaction interaction)) {
            return;
        }
        Optional<String> marker = renderer.markerOf(interaction);
        Optional<BlockPos> pos = renderer.posOf(interaction);
        if (marker.isEmpty() || pos.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!player.hasPermission("herbalis.plant")) {
            player.sendMessage(messages.msg("erreurs.permission"));
            return;
        }

        switch (marker.get()) {
            case "pot" -> potLeftClick(player, pos.get());
            case "rack" -> rackLeftClick(player, pos.get());
            case "jar" -> jarLeftClick(player, pos.get());
            default -> {
            }
        }
    }

    private void potLeftClick(Player player, BlockPos pos) {
        Location loc = PosCodec.corner(pos).orElse(null);
        if (loc == null) {
            return;
        }
        Optional<Plant> plant = plants.at(pos);
        if (plant.isPresent()) {
            breakPlant.execute(pos);
            renderer.removePlantVisual(pos);
            renderer.updatePotModel(pos, emptyPotModel(pos));
            fx.harvested(loc);
            if (config.dropSeedOnBreak() && !plant.get().isDead()) {
                // La graine rendue garde la genetique de la plante.
                drugs.byId(plant.get().drugId()).ifPresent(drug ->
                        loc.getWorld().dropItemNaturally(
                                loc.clone().add(0.5, 0.6, 0.5),
                                items.seed(drug,
                                        Quality.of(plant.get().seedQuality()))));
            }
            player.sendActionBar(messages.msg("culture.plante-arrachee"));
            return;
        }
        boolean hadDripper = pots.hasDripper(pos);
        breakPot.execute(pos);
        renderer.removePotVisual(pos);
        fx.broken(loc);
        Location potDrop = loc.clone().add(0.5, 0.4, 0.5);
        loc.getWorld().dropItemNaturally(potDrop, items.pot());
        if (hadDripper) {
            loc.getWorld().dropItemNaturally(potDrop, items.dripper());
        }
        player.sendActionBar(messages.msg("culture.pot-casse"));
    }

    private void rackLeftClick(Player player, BlockPos pos) {
        Location loc = PosCodec.corner(pos).orElse(null);
        if (loc == null) {
            return;
        }
        var rack = racks.at(pos).orElse(null);
        if (rack == null) {
            return;
        }
        Location dropAt = loc.clone().add(0.5, 0.6, 0.5);
        if (!rack.isEmpty()) {
            // Premier coup : on rend les tetes, fraiches (sechage perdu).
            drugs.byId(rack.drugId()).ifPresent(drug ->
                    rack.slots().forEach(slot -> loc.getWorld().dropItemNaturally(
                            dropAt, items.freshBud(drug, slot.quality()))));
            var emptied = rack.emptied();
            racks.put(emptied);
            renderer.updateRack(pos, RackVisualState.EMPTY);
            fx.rackCollect(loc, true);
            player.sendActionBar(messages.msg("sechage.contenu-rendu"));
            return;
        }
        breakRack.execute(pos);
        renderer.removeRackVisual(pos);
        fx.rackBroken(loc);
        loc.getWorld().dropItemNaturally(dropAt, items.dryingRack());
        player.sendActionBar(messages.msg("sechage.rack-casse"));
    }

    private void jarLeftClick(Player player, BlockPos pos) {
        Location loc = PosCodec.corner(pos).orElse(null);
        if (loc == null) {
            return;
        }
        var jar = jars.at(pos).orElse(null);
        if (jar == null) {
            return;
        }
        Location dropAt = loc.clone().add(0.5, 0.5, 0.5);
        if (!jar.isEmpty()) {
            // Premier coup : on rend le contenu tel quel (affinage perdu).
            drugs.byId(jar.drugId()).ifPresent(drug ->
                    jar.slots().forEach(slot -> loc.getWorld().dropItemNaturally(
                            dropAt, items.dried(drug, slot.quality()))));
            jars.put(jar.emptied());
            renderer.updateJar(pos, JarVisualState.EMPTY);
            fx.jarCollect(loc, false);
            player.sendActionBar(messages.msg("curing.contenu-rendu"));
            return;
        }
        breakJar.execute(pos);
        renderer.removeJarVisual(pos);
        fx.jarBroken(loc);
        loc.getWorld().dropItemNaturally(dropAt, items.curingJar());
        player.sendActionBar(messages.msg("curing.jarre-cassee"));
    }
}
