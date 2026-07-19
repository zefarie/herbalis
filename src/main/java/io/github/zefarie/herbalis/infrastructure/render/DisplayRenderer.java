package io.github.zefarie.herbalis.infrastructure.render;

import io.github.zefarie.herbalis.domain.curing.JarVisualState;
import io.github.zefarie.herbalis.domain.drying.RackVisualState;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.irrigation.SiloVisualState;
import io.github.zefarie.herbalis.domain.irrigation.TankSize;
import io.github.zefarie.herbalis.domain.irrigation.TankVisualState;
import io.github.zefarie.herbalis.domain.plant.Plant;
import io.github.zefarie.herbalis.infrastructure.item.ItemFactory;
import io.github.zefarie.herbalis.infrastructure.item.ItemKeys;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Rendu des pots, plantes et racks : Item Display pour le visuel,
 * Interaction pour la hitbox. Les entites sont non persistantes, la base
 * de donnees fait foi et tout est respawne au chargement des chunks.
 */
public final class DisplayRenderer {

    /** Hauteur du terreau du pot dans le modele (6 px sur 16). */
    private static final double SOIL_HEIGHT = 6.0 / 16.0;

    private static final String MARKER_POT = "pot";
    private static final String MARKER_PLANT = "plant";
    private static final String MARKER_RACK = "rack";
    private static final String MARKER_JAR = "jar";
    private static final String MARKER_PIPE = "pipe";
    private static final String MARKER_TANK = "tank";
    private static final String MARKER_SILO = "silo";
    private static final String MARKER_LAMP = "lamp";

    private record Spawned(UUID potDisplay, UUID plantDisplay, UUID interaction) {
    }

    private final Plugin plugin;
    private final Map<BlockPos, Spawned> pots = new HashMap<>();
    private final Map<BlockPos, Spawned> racks = new HashMap<>();
    private final Map<BlockPos, Spawned> jars = new HashMap<>();
    private final Map<BlockPos, Spawned> pipes = new HashMap<>();
    private final Map<BlockPos, Spawned> tanks = new HashMap<>();
    private final Map<BlockPos, Spawned> silos = new HashMap<>();
    private final Map<BlockPos, Spawned> lamps = new HashMap<>();
    private final Map<BlockPos, String> potModels = new HashMap<>();
    private final Map<BlockPos, String> plantModels = new HashMap<>();
    private final Map<BlockPos, String> networkModels = new HashMap<>();

    public DisplayRenderer(Plugin plugin) {
        this.plugin = plugin;
    }

    // ----------------------------------------------------------------
    // Pots et plantes
    // ----------------------------------------------------------------

    /** Fait apparaitre le pot (et sa plante eventuelle) a une position. */
    public void showPot(BlockPos pos, Optional<Plant> plant, String plantModel,
                        String potModel, float plantScale) {
        removePotVisual(pos);
        Optional<Location> center = PosCodec.center(pos);
        if (center.isEmpty()) {
            return;
        }
        Location loc = center.get();

        ItemDisplay potDisplay = spawnDisplay(loc, potModel, MARKER_POT, pos);
        potModels.put(pos, potModel);
        UUID plantId = null;
        if (plant.isPresent()) {
            ItemDisplay plantDisplay = spawnPlantDisplay(pos, plantModel);
            if (plantDisplay != null) {
                plantDisplay.setTransformation(transform(plantScale));
                plantId = plantDisplay.getUniqueId();
            }
        }
        Interaction interaction = spawnInteraction(pos, MARKER_POT,
                plant.isPresent() ? 1.35f : 0.5f, 0.85f);

        pots.put(pos, new Spawned(potDisplay.getUniqueId(), plantId,
                interaction == null ? null : interaction.getUniqueId()));
    }

    /** Plante une graine : apparait avec un petit pop de scale. */
    public void spawnPlantWithPop(BlockPos pos, Plant plant, String model,
                                  float scale) {
        Spawned current = pots.get(pos);
        if (current == null) {
            showPot(pos, Optional.of(plant), model, "pot", scale);
            return;
        }
        removeEntity(current.plantDisplay());

        ItemDisplay display = spawnPlantDisplay(pos, model);
        if (display == null) {
            return;
        }
        display.setTransformation(transform(0.05f));
        animate(display, 3, 8, transform(scale));

        pots.put(pos, new Spawned(current.potDisplay(), display.getUniqueId(),
                current.interaction()));
        resizeInteraction(pos, 1.35f);
    }

    /** Met a jour le modele de la plante (stage ou etat) avec interpolation. */
    public void updatePlant(BlockPos pos, String model, boolean growPop,
                            float scale) {
        Spawned current = pots.get(pos);
        if (current == null || current.plantDisplay() == null) {
            return;
        }
        Entity entity = entity(current.plantDisplay());
        if (!(entity instanceof ItemDisplay display)) {
            return;
        }
        display.setItemStack(ItemFactory.displayItem(model));
        plantModels.put(pos, model);
        if (growPop) {
            display.setTransformation(transform(scale * 0.75f));
            animate(display, 2, 26, transform(scale));
        }
    }

    /**
     * Change le modele de la plante sans animation (givrage de la
     * fenetre optimale). Sans effet si le modele est deja affiche.
     */
    public void updatePlantModel(BlockPos pos, String model) {
        if (model.equals(plantModels.get(pos))) {
            return;
        }
        Spawned current = pots.get(pos);
        if (current == null || current.plantDisplay() == null
                || !(entity(current.plantDisplay()) instanceof ItemDisplay display)) {
            return;
        }
        display.setItemStack(ItemFactory.displayItem(model));
        plantModels.put(pos, model);
    }

    /** Change le modele du pot (terreau humide, sec, fertilise). */
    public void updatePotModel(BlockPos pos, String potModel) {
        if (potModel.equals(potModels.get(pos))) {
            return;
        }
        Spawned current = pots.get(pos);
        if (current == null
                || !(entity(current.potDisplay()) instanceof ItemDisplay display)) {
            return;
        }
        display.setItemStack(ItemFactory.displayItem(potModel));
        potModels.put(pos, potModel);
    }

    /** Micro pulsation de scale (arrosage, engrais). */
    public void pulsePlant(BlockPos pos) {
        Spawned current = pots.get(pos);
        if (current == null || current.plantDisplay() == null) {
            return;
        }
        if (entity(current.plantDisplay()) instanceof ItemDisplay display) {
            Vector3f scale = display.getTransformation().getScale();
            animate(display, 0, 4, transform(scale.x * 1.07f));
            animate(display, 6, 8, transform(scale.x));
        }
    }

    /** Retire la plante mais garde le pot (recolte, arrachage). */
    public void removePlantVisual(BlockPos pos) {
        plantModels.remove(pos);
        Spawned current = pots.get(pos);
        if (current == null) {
            return;
        }
        removeEntity(current.plantDisplay());
        pots.put(pos, new Spawned(current.potDisplay(), null, current.interaction()));
        resizeInteraction(pos, 0.5f);
    }

    /** Retire pot et plante. */
    public void removePotVisual(BlockPos pos) {
        potModels.remove(pos);
        plantModels.remove(pos);
        Spawned current = pots.remove(pos);
        if (current != null) {
            removeEntity(current.potDisplay());
            removeEntity(current.plantDisplay());
            removeEntity(current.interaction());
        }
    }

    // ----------------------------------------------------------------
    // Racks
    // ----------------------------------------------------------------

    public void showRack(BlockPos pos, RackVisualState state) {
        removeRackVisual(pos);
        Optional<Location> center = PosCodec.center(pos);
        if (center.isEmpty()) {
            return;
        }
        ItemDisplay display = spawnDisplay(center.get(), rackModel(state), MARKER_RACK, pos);
        Interaction interaction = spawnInteraction(pos, MARKER_RACK, 1.0f, 0.95f);
        racks.put(pos, new Spawned(display.getUniqueId(), null,
                interaction == null ? null : interaction.getUniqueId()));
    }

    public void updateRack(BlockPos pos, RackVisualState state) {
        Spawned current = racks.get(pos);
        if (current == null) {
            return;
        }
        if (entity(current.potDisplay()) instanceof ItemDisplay display) {
            display.setItemStack(ItemFactory.displayItem(rackModel(state)));
        }
    }

    public void removeRackVisual(BlockPos pos) {
        Spawned current = racks.remove(pos);
        if (current != null) {
            removeEntity(current.potDisplay());
            removeEntity(current.interaction());
        }
    }

    // ----------------------------------------------------------------
    // Jarres de curing
    // ----------------------------------------------------------------

    public void showJar(BlockPos pos, JarVisualState state) {
        removeJarVisual(pos);
        Optional<Location> center = PosCodec.center(pos);
        if (center.isEmpty()) {
            return;
        }
        ItemDisplay display = spawnDisplay(center.get(), jarModel(state), MARKER_JAR, pos);
        Interaction interaction = spawnInteraction(pos, MARKER_JAR, 0.75f, 0.7f);
        jars.put(pos, new Spawned(display.getUniqueId(), null,
                interaction == null ? null : interaction.getUniqueId()));
    }

    public void updateJar(BlockPos pos, JarVisualState state) {
        Spawned current = jars.get(pos);
        if (current == null) {
            return;
        }
        if (entity(current.potDisplay()) instanceof ItemDisplay display) {
            display.setItemStack(ItemFactory.displayItem(jarModel(state)));
        }
    }

    public void removeJarVisual(BlockPos pos) {
        Spawned current = jars.remove(pos);
        if (current != null) {
            removeEntity(current.potDisplay());
            removeEntity(current.interaction());
        }
    }

    // ----------------------------------------------------------------
    // Reseau d'irrigation : tuyaux, caissons, silos
    // ----------------------------------------------------------------

    /** Tuyau : le modele suit le masque de connexions (voisins relies). */
    public void showPipe(BlockPos pos, int mask) {
        removePipeVisual(pos);
        Optional<Location> center = PosCodec.center(pos);
        if (center.isEmpty()) {
            return;
        }
        String model = pipeModel(mask);
        ItemDisplay display = spawnDisplay(center.get(), model, MARKER_PIPE, pos);
        networkModels.put(pos, model);
        Interaction interaction = spawnInteraction(pos, MARKER_PIPE, 0.7f, 0.55f);
        pipes.put(pos, new Spawned(display.getUniqueId(), null,
                interaction == null ? null : interaction.getUniqueId()));
    }

    public void updatePipe(BlockPos pos, int mask) {
        updateNetworkModel(pipes.get(pos), pos, pipeModel(mask));
    }

    public void removePipeVisual(BlockPos pos) {
        removeSimple(pipes, pos);
    }

    public void showTank(BlockPos pos, TankSize size, TankVisualState state) {
        removeTankVisual(pos);
        Optional<Location> center = PosCodec.center(pos);
        if (center.isEmpty()) {
            return;
        }
        String model = tankModel(size, state);
        ItemDisplay display = spawnDisplay(center.get(), model, MARKER_TANK, pos);
        networkModels.put(pos, model);
        Interaction interaction = spawnInteraction(pos, MARKER_TANK,
                tankHeight(size), 0.95f);
        tanks.put(pos, new Spawned(display.getUniqueId(), null,
                interaction == null ? null : interaction.getUniqueId()));
    }

    /** Le niveau d'eau se lit dans la cuve : swap de modele sans effet si inchange. */
    public void updateTank(BlockPos pos, TankSize size, TankVisualState state) {
        updateNetworkModel(tanks.get(pos), pos, tankModel(size, state));
    }

    public void removeTankVisual(BlockPos pos) {
        removeSimple(tanks, pos);
    }

    public void showSilo(BlockPos pos, SiloVisualState state) {
        removeSiloVisual(pos);
        Optional<Location> center = PosCodec.center(pos);
        if (center.isEmpty()) {
            return;
        }
        String model = siloModel(state);
        ItemDisplay display = spawnDisplay(center.get(), model, MARKER_SILO, pos);
        networkModels.put(pos, model);
        Interaction interaction = spawnInteraction(pos, MARKER_SILO, 1.0f, 0.9f);
        silos.put(pos, new Spawned(display.getUniqueId(), null,
                interaction == null ? null : interaction.getUniqueId()));
    }

    public void updateSilo(BlockPos pos, SiloVisualState state) {
        updateNetworkModel(silos.get(pos), pos, siloModel(state));
    }

    public void removeSiloVisual(BlockPos pos) {
        removeSimple(silos, pos);
    }

    /** Lampe UV : le display est fullbright, elle parait allumee de loin. */
    public void showLamp(BlockPos pos) {
        removeLampVisual(pos);
        Optional<Location> center = PosCodec.center(pos);
        if (center.isEmpty()) {
            return;
        }
        ItemDisplay display = spawnDisplay(center.get(), "uv_lamp", MARKER_LAMP, pos);
        display.setBrightness(new Display.Brightness(15, 15));
        Interaction interaction = spawnInteraction(pos, MARKER_LAMP, 1.0f, 0.6f);
        lamps.put(pos, new Spawned(display.getUniqueId(), null,
                interaction == null ? null : interaction.getUniqueId()));
    }

    public void removeLampVisual(BlockPos pos) {
        removeSimple(lamps, pos);
    }

    /** Parcourt les lampes vivantes (halo violet ambiant). */
    public void forEachLampDisplay(java.util.function.BiConsumer<BlockPos, ItemDisplay> consumer) {
        lamps.forEach((pos, spawned) -> {
            if (entity(spawned.potDisplay()) instanceof ItemDisplay display
                    && display.isValid()) {
                consumer.accept(pos, display);
            }
        });
    }

    private void updateNetworkModel(Spawned current, BlockPos pos, String model) {
        if (current == null || model.equals(networkModels.get(pos))) {
            return;
        }
        if (entity(current.potDisplay()) instanceof ItemDisplay display) {
            display.setItemStack(ItemFactory.displayItem(model));
            networkModels.put(pos, model);
        }
    }

    private void removeSimple(Map<BlockPos, Spawned> family, BlockPos pos) {
        networkModels.remove(pos);
        Spawned current = family.remove(pos);
        if (current != null) {
            removeEntity(current.potDisplay());
            removeEntity(current.interaction());
        }
    }

    // ----------------------------------------------------------------
    // Cycle de vie des chunks
    // ----------------------------------------------------------------

    /** Oublie les entites d'un chunk decharge (elles disparaissent avec lui). */
    public void forgetChunk(UUID worldId, int chunkX, int chunkZ) {
        pots.keySet().removeIf(pos -> inChunk(pos, worldId, chunkX, chunkZ));
        racks.keySet().removeIf(pos -> inChunk(pos, worldId, chunkX, chunkZ));
        jars.keySet().removeIf(pos -> inChunk(pos, worldId, chunkX, chunkZ));
        pipes.keySet().removeIf(pos -> inChunk(pos, worldId, chunkX, chunkZ));
        tanks.keySet().removeIf(pos -> inChunk(pos, worldId, chunkX, chunkZ));
        silos.keySet().removeIf(pos -> inChunk(pos, worldId, chunkX, chunkZ));
        lamps.keySet().removeIf(pos -> inChunk(pos, worldId, chunkX, chunkZ));
        potModels.keySet().removeIf(pos -> inChunk(pos, worldId, chunkX, chunkZ));
        plantModels.keySet().removeIf(pos -> inChunk(pos, worldId, chunkX, chunkZ));
        networkModels.keySet().removeIf(pos -> inChunk(pos, worldId, chunkX, chunkZ));
    }

    /**
     * Supprime toutes les entites marquees Herbalis d'un chunk. Utilise
     * avant respawn pour eliminer les orphelines (crash, reload).
     */
    public void purgeChunk(Chunk chunk) {
        for (Entity entity : chunk.getEntities()) {
            if (entity.getPersistentDataContainer()
                    .has(ItemKeys.MARKER, PersistentDataType.STRING)) {
                entity.remove();
            }
        }
    }

    /** Parcourt les displays de plantes vivants (pour l'animation de sway). */
    public void forEachPlantDisplay(java.util.function.BiConsumer<BlockPos, ItemDisplay> consumer) {
        pots.forEach((pos, spawned) -> {
            if (spawned.plantDisplay() != null
                    && entity(spawned.plantDisplay()) instanceof ItemDisplay display
                    && display.isValid()) {
                consumer.accept(pos, display);
            }
        });
    }

    /** Position portee par une entite Herbalis (hitbox ou display). */
    public Optional<BlockPos> posOf(Entity entity) {
        String raw = entity.getPersistentDataContainer()
                .get(ItemKeys.POS, PersistentDataType.STRING);
        return PosCodec.decode(raw);
    }

    public Optional<String> markerOf(Entity entity) {
        return Optional.ofNullable(entity.getPersistentDataContainer()
                .get(ItemKeys.MARKER, PersistentDataType.STRING));
    }

    // ----------------------------------------------------------------
    // Interne
    // ----------------------------------------------------------------

    private ItemDisplay spawnPlantDisplay(BlockPos pos, String model) {
        Optional<Location> center = PosCodec.center(pos);
        if (center.isEmpty()) {
            return null;
        }
        Location loc = center.get().add(0, SOIL_HEIGHT, 0);
        // Orientation propre a la position : pas deux plants identiques.
        loc.setYaw(PlantVisuals.yawOf(pos));
        plantModels.put(pos, model);
        return spawnDisplayAt(loc, model, MARKER_PLANT, pos);
    }

    private ItemDisplay spawnDisplay(Location center, String model, String marker, BlockPos pos) {
        return spawnDisplayAt(center, model, marker, pos);
    }

    private ItemDisplay spawnDisplayAt(Location loc, String model, String marker, BlockPos pos) {
        return loc.getWorld().spawn(loc, ItemDisplay.class, display -> {
            display.setItemStack(ItemFactory.displayItem(model));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            display.setBillboard(Display.Billboard.FIXED);
            display.setPersistent(false);
            display.setShadowRadius(0f);
            display.setViewRange(0.7f);
            mark(display, marker, pos);
        });
    }

    private Interaction spawnInteraction(BlockPos pos, String marker,
                                         float height, float width) {
        Optional<Location> base = PosCodec.base(pos);
        return base.map(loc -> loc.getWorld().spawn(loc, Interaction.class, interaction -> {
            interaction.setInteractionHeight(height);
            interaction.setInteractionWidth(width);
            interaction.setResponsive(true);
            interaction.setPersistent(false);
            mark(interaction, marker, pos);
        })).orElse(null);
    }

    private void resizeInteraction(BlockPos pos, float height) {
        Spawned current = pots.get(pos);
        if (current != null && entity(current.interaction()) instanceof Interaction interaction) {
            interaction.setInteractionHeight(height);
        }
    }

    private void mark(Entity entity, String marker, BlockPos pos) {
        var pdc = entity.getPersistentDataContainer();
        pdc.set(ItemKeys.MARKER, PersistentDataType.STRING, marker);
        pdc.set(ItemKeys.POS, PersistentDataType.STRING, PosCodec.encode(pos));
    }

    /** Applique une transformation apres un delai, avec interpolation. */
    private void animate(ItemDisplay display, int delayTicks, int durationTicks,
                         Transformation target) {
        Consumer<ItemDisplay> apply = d -> {
            if (!d.isValid()) {
                return;
            }
            d.setInterpolationDelay(0);
            d.setInterpolationDuration(durationTicks);
            d.setTransformation(target);
        };
        if (delayTicks <= 0) {
            apply.accept(display);
        } else {
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> apply.accept(display), delayTicks);
        }
    }

    private static Transformation transform(float scale) {
        return new Transformation(new Vector3f(0f, 0f, 0f), new Quaternionf(),
                new Vector3f(scale, scale, scale), new Quaternionf());
    }

    private String rackModel(RackVisualState state) {
        return switch (state) {
            case EMPTY -> "drying_rack";
            case DRYING -> "drying_rack_full";
            case READY -> "drying_rack_ready";
        };
    }

    private String jarModel(JarVisualState state) {
        return switch (state) {
            case EMPTY -> "curing_jar";
            case CURING -> "curing_jar_full";
            case READY -> "curing_jar_ready";
            case MOLDY -> "curing_jar_moldy";
        };
    }

    private static String pipeModel(int mask) {
        return "pipe_" + mask;
    }

    private static String tankModel(TankSize size, TankVisualState state) {
        String base = "tank_" + size.id();
        return switch (state) {
            case EMPTY -> base;
            case LOW -> base + "_low";
            case MID -> base + "_mid";
            case FULL -> base + "_full";
        };
    }

    private static String siloModel(SiloVisualState state) {
        return switch (state) {
            case EMPTY -> "silo";
            case PARTIAL -> "silo_mid";
            case FULL -> "silo_full";
        };
    }

    private static float tankHeight(TankSize size) {
        return switch (size) {
            case CUVE -> 0.8f;
            case CITERNE -> 1.0f;
            case RESERVOIR -> 1.4f;
        };
    }

    private Entity entity(UUID id) {
        return id == null ? null : plugin.getServer().getEntity(id);
    }

    private void removeEntity(UUID id) {
        Entity entity = entity(id);
        if (entity != null) {
            entity.remove();
        }
    }

    private static boolean inChunk(BlockPos pos, UUID worldId, int chunkX, int chunkZ) {
        return pos.worldId().equals(worldId)
                && pos.chunkX() == chunkX && pos.chunkZ() == chunkZ;
    }
}
