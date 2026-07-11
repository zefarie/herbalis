package io.github.zefarie.herbalis.infrastructure.render;

import io.github.zefarie.herbalis.domain.drying.RackVisualState;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.plant.Plant;
import io.github.zefarie.herbalis.domain.plant.PlantState;
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

    private record Spawned(UUID potDisplay, UUID plantDisplay, UUID interaction) {
    }

    private final Plugin plugin;
    private final Map<BlockPos, Spawned> pots = new HashMap<>();
    private final Map<BlockPos, Spawned> racks = new HashMap<>();

    public DisplayRenderer(Plugin plugin) {
        this.plugin = plugin;
    }

    // ----------------------------------------------------------------
    // Pots et plantes
    // ----------------------------------------------------------------

    /** Fait apparaitre le pot (et sa plante eventuelle) a une position. */
    public void showPot(BlockPos pos, Optional<Plant> plant, String drugModelPrefix) {
        removePotVisual(pos);
        Optional<Location> center = PosCodec.center(pos);
        if (center.isEmpty()) {
            return;
        }
        Location loc = center.get();

        ItemDisplay potDisplay = spawnDisplay(loc, "pot", MARKER_POT, pos);
        UUID plantId = null;
        if (plant.isPresent()) {
            ItemDisplay plantDisplay = spawnPlantDisplay(pos, plant.get(), drugModelPrefix);
            plantId = plantDisplay == null ? null : plantDisplay.getUniqueId();
        }
        Interaction interaction = spawnInteraction(pos, MARKER_POT,
                plant.isPresent() ? 1.35f : 0.5f, 0.85f);

        pots.put(pos, new Spawned(potDisplay.getUniqueId(), plantId,
                interaction == null ? null : interaction.getUniqueId()));
    }

    /** Plante une graine : apparait avec un petit pop de scale. */
    public void spawnPlantWithPop(BlockPos pos, Plant plant, String modelPrefix) {
        Spawned current = pots.get(pos);
        if (current == null) {
            showPot(pos, Optional.of(plant), modelPrefix);
            return;
        }
        removeEntity(current.plantDisplay());

        ItemDisplay display = spawnPlantDisplay(pos, plant, modelPrefix);
        if (display == null) {
            return;
        }
        display.setTransformation(transform(0.05f));
        animate(display, 3, 8, transform(1.0f));

        pots.put(pos, new Spawned(current.potDisplay(), display.getUniqueId(),
                current.interaction()));
        resizeInteraction(pos, 1.35f);
    }

    /** Met a jour le modele de la plante (stage ou etat) avec interpolation. */
    public void updatePlant(BlockPos pos, Plant plant, String modelPrefix, boolean growPop) {
        Spawned current = pots.get(pos);
        if (current == null || current.plantDisplay() == null) {
            return;
        }
        Entity entity = entity(current.plantDisplay());
        if (!(entity instanceof ItemDisplay display)) {
            return;
        }
        display.setItemStack(ItemFactory.displayItem(plantModel(plant, modelPrefix)));
        if (growPop) {
            display.setTransformation(transform(0.72f));
            animate(display, 2, 26, transform(1.0f));
        }
    }

    /** Micro pulsation de scale (arrosage, engrais). */
    public void pulsePlant(BlockPos pos) {
        Spawned current = pots.get(pos);
        if (current == null || current.plantDisplay() == null) {
            return;
        }
        if (entity(current.plantDisplay()) instanceof ItemDisplay display) {
            animate(display, 0, 4, transform(1.07f));
            animate(display, 6, 8, transform(1.0f));
        }
    }

    /** Retire la plante mais garde le pot (recolte, arrachage). */
    public void removePlantVisual(BlockPos pos) {
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
    // Cycle de vie des chunks
    // ----------------------------------------------------------------

    /** Oublie les entites d'un chunk decharge (elles disparaissent avec lui). */
    public void forgetChunk(UUID worldId, int chunkX, int chunkZ) {
        pots.keySet().removeIf(pos -> inChunk(pos, worldId, chunkX, chunkZ));
        racks.keySet().removeIf(pos -> inChunk(pos, worldId, chunkX, chunkZ));
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

    private ItemDisplay spawnPlantDisplay(BlockPos pos, Plant plant, String modelPrefix) {
        Optional<Location> center = PosCodec.center(pos);
        if (center.isEmpty()) {
            return null;
        }
        Location loc = center.get().add(0, SOIL_HEIGHT, 0);
        return spawnDisplayAt(loc, plantModel(plant, modelPrefix), MARKER_PLANT, pos);
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

    private String plantModel(Plant plant, String modelPrefix) {
        if (plant.state() == PlantState.DEAD) {
            return "plant_" + modelPrefix + "_dead";
        }
        String suffix = plant.state() == PlantState.WITHERED && plant.stage() >= 2
                ? "_dry" : "";
        return "plant_" + modelPrefix + "_stage_" + plant.stage() + suffix;
    }

    private String rackModel(RackVisualState state) {
        return switch (state) {
            case EMPTY -> "drying_rack";
            case DRYING -> "drying_rack_full";
            case READY -> "drying_rack_ready";
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
