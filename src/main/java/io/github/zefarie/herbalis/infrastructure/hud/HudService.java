package io.github.zefarie.herbalis.infrastructure.hud;

import io.github.zefarie.herbalis.application.port.JarRepository;
import io.github.zefarie.herbalis.application.port.PlantEnvironment;
import io.github.zefarie.herbalis.application.port.PlantRepository;
import io.github.zefarie.herbalis.application.port.PotRepository;
import io.github.zefarie.herbalis.application.port.RackRepository;
import io.github.zefarie.herbalis.domain.curing.CuringJar;
import io.github.zefarie.herbalis.domain.curing.JarVisualState;
import io.github.zefarie.herbalis.domain.drug.DrugRegistry;
import io.github.zefarie.herbalis.domain.drug.DrugType;
import io.github.zefarie.herbalis.domain.drying.DryingRack;
import io.github.zefarie.herbalis.domain.drying.RackVisualState;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.plant.GrowthEngine;
import io.github.zefarie.herbalis.domain.plant.Plant;
import io.github.zefarie.herbalis.domain.plant.PlantState;
import io.github.zefarie.herbalis.domain.quality.Quality;
import io.github.zefarie.herbalis.domain.quality.QualityCalculator;
import io.github.zefarie.herbalis.infrastructure.config.HerbalisConfig;
import io.github.zefarie.herbalis.infrastructure.config.Messages;
import io.github.zefarie.herbalis.infrastructure.item.ItemFactory;
import io.github.zefarie.herbalis.infrastructure.render.DisplayRenderer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.FluidCollisionMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Etat des cibles du regard : construit le contenu des hologrammes
 * (plante, pot, rack, jarre) et les lignes d'action bar utilisees en
 * retour d'action (progression d'un rack au clic, /herbalis info).
 */
public final class HudService {

    /** Contenu d'un hologramme et hauteur d'ancrage au-dessus du bloc. */
    public record Hologram(Component text, float height) {
    }

    private final HerbalisConfig config;
    private final Messages messages;
    private final ItemFactory items;
    private final DisplayRenderer renderer;
    private final PlantRepository plants;
    private final PotRepository pots;
    private final RackRepository racks;
    private final JarRepository jars;
    private final DrugRegistry drugs;
    private final PlantEnvironment environment;

    public HudService(HerbalisConfig config, Messages messages, ItemFactory items,
                      DisplayRenderer renderer, PlantRepository plants,
                      PotRepository pots, RackRepository racks, JarRepository jars,
                      DrugRegistry drugs, PlantEnvironment environment) {
        this.config = config;
        this.messages = messages;
        this.items = items;
        this.renderer = renderer;
        this.plants = plants;
        this.pots = pots;
        this.racks = racks;
        this.jars = jars;
        this.drugs = drugs;
        this.environment = environment;
    }

    /** Position Herbalis visee par le joueur, s'il y en a une. */
    public Optional<BlockPos> targetPos(Player player) {
        RayTraceResult result = player.getWorld().rayTrace(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                config.hudRange(),
                FluidCollisionMode.NEVER,
                true,
                0.1,
                entity -> renderer.markerOf(entity).isPresent());
        if (result == null || result.getHitEntity() == null) {
            return Optional.empty();
        }
        Entity entity = result.getHitEntity();
        return renderer.posOf(entity);
    }

    /** Ligne d'etat pour une position (pot vide, rack ou jarre). */
    public Optional<Component> buildLine(BlockPos pos, long now) {
        if (pots.exists(pos) && plants.at(pos).isEmpty()) {
            return Optional.of(messages.msg("hud.pot-vide"));
        }
        Optional<DryingRack> rack = racks.at(pos);
        if (rack.isPresent()) {
            return rackLine(rack.get(), now);
        }
        Optional<CuringJar> jar = jars.at(pos);
        if (jar.isPresent()) {
            return jarLine(jar.get(), now);
        }
        return Optional.empty();
    }

    // ----------------------------------------------------------------
    // Hologrammes
    // ----------------------------------------------------------------

    /** Hologramme d'etat pour une position (plante, pot, rack, jarre). */
    public Optional<Hologram> buildHologram(BlockPos pos, long now) {
        Optional<Plant> plant = plants.at(pos);
        if (plant.isPresent()) {
            return plantHologram(pos, plant.get());
        }
        if (pots.exists(pos)) {
            List<Component> lines = new ArrayList<>(List.of(
                    messages.msg("hud.holo-pot-vide"),
                    messages.msg("hud.holo-pot-vide-astuce")));
            if (pots.hasDripper(pos)) {
                lines.add(messages.msg("hud.holo-goutte"));
            }
            return Optional.of(new Hologram(join(lines), 1.05f));
        }
        Optional<DryingRack> rack = racks.at(pos);
        if (rack.isPresent()) {
            return Optional.of(rackHologram(rack.get(), now));
        }
        Optional<CuringJar> jar = jars.at(pos);
        if (jar.isPresent()) {
            return Optional.of(jarHologram(jar.get(), now));
        }
        return Optional.empty();
    }

    private Optional<Hologram> plantHologram(BlockPos pos, Plant plant) {
        DrugType drug = drugs.byId(plant.drugId()).orElse(null);
        if (drug == null) {
            return Optional.empty();
        }
        float height = plant.stage() >= 3 ? 2.0f : 1.55f;
        if (plant.isDead()) {
            return Optional.of(new Hologram(messages.msg("hud.plante-morte",
                    Messages.ph("nom", drug.displayName())), 1.35f));
        }

        List<Component> lines = new ArrayList<>(5);
        lines.add(messages.msg("hud.holo-titre",
                Messages.ph("nom", drug.displayName()),
                Messages.ph("segments", messages.deserialize(
                        segments(plant.stage(), drug.growth().stageCount())))));
        lines.add(messages.msg(hydrationTemplate(plant.hydration()),
                Messages.ph("valeur", String.valueOf((int) plant.hydration()))));
        lines.add(messages.msg("hud.holo-terreau",
                Messages.ph("etat", messages.msg(soilStateKey(plant, drug)))));
        Quality potential = QualityCalculator.harvestQuality(plant, drug);
        lines.add(messages.msg("hud.holo-qualite",
                Messages.ph("etoiles",
                        messages.deserialize(items.starsMarkup(potential)))));
        if (pots.hasDripper(pos)) {
            lines.add(messages.msg("hud.holo-goutte"));
        }
        String alert = alert(plant, drug);
        if (!alert.isEmpty()) {
            lines.add(messages.msg(alert));
        }
        return Optional.of(new Hologram(join(lines), height));
    }

    private Hologram rackHologram(DryingRack rack, long now) {
        DrugType drug = drugs.byId(rack.drugId()).orElse(null);
        RackVisualState state = drug == null
                ? RackVisualState.EMPTY
                : rack.visualState(now, drug.drying().duration());
        Component status = switch (state) {
            case EMPTY -> messages.msg("hud.holo-rack-vide");
            case DRYING -> messages.msg("hud.holo-rack-sechage",
                    Messages.ph("pourcent", String.valueOf((int) Math.round(
                            rack.overallProgress(now, drug.drying().duration()) * 100))),
                    Messages.ph("nombre", String.valueOf(rack.slots().size())));
            case READY -> messages.msg("hud.holo-rack-pret",
                    Messages.ph("nombre", String.valueOf(rack.slots().size())));
        };
        return new Hologram(join(List.of(
                messages.msg("hud.holo-rack-titre"), status)), 1.45f);
    }

    private Hologram jarHologram(CuringJar jar, long now) {
        DrugType drug = drugs.byId(jar.drugId()).orElse(null);
        JarVisualState state = drug == null
                ? JarVisualState.EMPTY
                : jar.visualState(now, drug.curing());
        Component status = switch (state) {
            case EMPTY -> messages.msg("hud.holo-jarre-vide");
            case CURING -> messages.msg("hud.holo-jarre-curing",
                    Messages.ph("pourcent", String.valueOf((int) Math.round(
                            jar.overallProgress(now, drug.curing()) * 100))),
                    Messages.ph("nombre", String.valueOf(jar.slots().size())));
            case READY -> messages.msg("hud.holo-jarre-prete");
            case MOLDY -> messages.msg("hud.holo-jarre-moisie");
        };
        return new Hologram(join(List.of(
                messages.msg("hud.holo-jarre-titre"), status)), 1.05f);
    }

    private String soilStateKey(Plant plant, DrugType drug) {
        if (plant.state() != PlantState.HEALTHY
                || plant.hydration() <= drug.hydration().thirstyThreshold()) {
            return "hud.holo-terreau-sec";
        }
        return plant.isFertilizedThisStage()
                ? "hud.holo-terreau-fertilise" : "hud.holo-terreau-humide";
    }

    private static Component join(List<Component> lines) {
        return Component.join(
                JoinConfiguration.separator(Component.newline()), lines);
    }

    private Optional<Component> rackLine(DryingRack rack, long now) {
        DrugType drug = drugs.byId(rack.drugId()).orElse(null);
        RackVisualState state = drug == null
                ? RackVisualState.EMPTY
                : rack.visualState(now, drug.drying().duration());
        return Optional.of(switch (state) {
            case EMPTY -> messages.msg("hud.rack-vide");
            case DRYING -> {
                int percent = (int) Math.round(
                        rack.overallProgress(now, drug.drying().duration()) * 100);
                yield messages.msg("hud.rack-sechage",
                        Messages.ph("pourcent", String.valueOf(percent)),
                        Messages.ph("nombre", String.valueOf(rack.slots().size())));
            }
            case READY -> messages.msg("hud.rack-pret",
                    Messages.ph("nombre", String.valueOf(rack.slots().size())));
        });
    }

    private Optional<Component> jarLine(CuringJar jar, long now) {
        DrugType drug = drugs.byId(jar.drugId()).orElse(null);
        JarVisualState state = drug == null
                ? JarVisualState.EMPTY
                : jar.visualState(now, drug.curing());
        return Optional.of(switch (state) {
            case EMPTY -> messages.msg("hud.jarre-vide");
            case CURING -> {
                int percent = (int) Math.round(
                        jar.overallProgress(now, drug.curing()) * 100);
                yield messages.msg("hud.jarre-curing",
                        Messages.ph("pourcent", String.valueOf(percent)),
                        Messages.ph("nombre", String.valueOf(jar.slots().size())));
            }
            case READY -> messages.msg("hud.jarre-prete",
                    Messages.ph("nombre", String.valueOf(jar.slots().size())));
            case MOLDY -> messages.msg("hud.jarre-moisie");
        });
    }

    private String segments(int stage, int stageCount) {
        String full = messages.raw("hud.segment-plein", "<color:#4ade80>▰</color>");
        String hollow = messages.raw("hud.segment-vide", "<color:#374151>▱</color>");
        return full.repeat(stage) + hollow.repeat(Math.max(0, stageCount - stage));
    }

    private String hydrationTemplate(double hydration) {
        if (hydration >= 60) {
            return "hud.hydratation-haute";
        }
        return hydration >= 30 ? "hud.hydratation-moyenne" : "hud.hydratation-basse";
    }

    private String alert(Plant plant, DrugType drug) {
        if (plant.state() == PlantState.WITHERED) {
            return "hud.alerte-assoiffee";
        }
        if (plant.isInfested()) {
            return "hud.alerte-nuisibles";
        }
        if (GrowthEngine.isHarvestable(plant, drug)) {
            return drug.harvestWindow().isOptimal(plant.ripenMillis())
                    ? "hud.alerte-recolte-optimale"
                    : "hud.alerte-recolte-tardive";
        }
        if (GrowthEngine.isLightStarved(plant, drug,
                environment.lightLevel(plant.pos()))) {
            return "hud.alerte-lumiere";
        }
        if (plant.hydration() <= drug.hydration().thirstyThreshold()) {
            return "hud.alerte-soif";
        }
        if (!plant.isToppingAttempted() && drug.topping().isWindowOpen(
                plant.stage(), stageProgress(plant, drug))) {
            return "hud.alerte-taille";
        }
        return "";
    }

    private static double stageProgress(Plant plant, DrugType drug) {
        long total = Math.max(1,
                drug.growth().durationOf(plant.stage()).toMillis());
        return plant.stageGrowthMillis() / (double) total;
    }
}
