package io.github.zefarie.herbalis.infrastructure.hud;

import io.github.zefarie.herbalis.application.port.PlantEnvironment;
import io.github.zefarie.herbalis.application.port.PlantRepository;
import io.github.zefarie.herbalis.application.port.PotRepository;
import io.github.zefarie.herbalis.application.port.RackRepository;
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
import org.bukkit.FluidCollisionMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import java.util.Optional;

/**
 * HUD principal : quand un joueur regarde une plante, un pot ou un rack,
 * une action bar stylee resume son etat (stage, hydratation, qualite,
 * alertes).
 */
public final class HudService {

    private final HerbalisConfig config;
    private final Messages messages;
    private final ItemFactory items;
    private final DisplayRenderer renderer;
    private final PlantRepository plants;
    private final PotRepository pots;
    private final RackRepository racks;
    private final DrugRegistry drugs;
    private final PlantEnvironment environment;

    public HudService(HerbalisConfig config, Messages messages, ItemFactory items,
                      DisplayRenderer renderer, PlantRepository plants,
                      PotRepository pots, RackRepository racks, DrugRegistry drugs,
                      PlantEnvironment environment) {
        this.config = config;
        this.messages = messages;
        this.items = items;
        this.renderer = renderer;
        this.plants = plants;
        this.pots = pots;
        this.racks = racks;
        this.drugs = drugs;
        this.environment = environment;
    }

    /** Tick regulier : cherche la cible du regard et affiche le HUD. */
    public void tick(Player player, long now) {
        if (!config.hudEnabled()) {
            return;
        }
        targetPos(player).ifPresent(pos -> buildLine(pos, now)
                .ifPresent(player::sendActionBar));
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

    /** Ligne d'etat pour une position (plante, pot vide ou rack). */
    public Optional<Component> buildLine(BlockPos pos, long now) {
        Optional<Plant> plant = plants.at(pos);
        if (plant.isPresent()) {
            return plantLine(plant.get(), now);
        }
        if (pots.exists(pos)) {
            return Optional.of(messages.msg("hud.pot-vide"));
        }
        Optional<DryingRack> rack = racks.at(pos);
        if (rack.isPresent()) {
            return rackLine(rack.get(), now);
        }
        return Optional.empty();
    }

    private Optional<Component> plantLine(Plant plant, long now) {
        DrugType drug = drugs.byId(plant.drugId()).orElse(null);
        if (drug == null) {
            return Optional.empty();
        }
        if (plant.isDead()) {
            return Optional.of(messages.msg("hud.plante-morte",
                    Messages.ph("nom", drug.displayName())));
        }

        String segments = segments(plant.stage(), drug.growth().stageCount());
        Quality potential = QualityCalculator.harvestQuality(plant, drug);
        String alert = alert(plant, drug);

        String hydraTemplate = hydrationTemplate(plant.hydration());
        Component hydra = messages.msg(hydraTemplate,
                Messages.ph("valeur", String.valueOf((int) plant.hydration())));

        return Optional.of(messages.msg("hud.plante",
                Messages.ph("nom", drug.displayName()),
                Messages.ph("segments", messages.deserialize(segments)),
                Messages.ph("hydratation", hydra),
                Messages.ph("etoiles", messages.deserialize(items.starsMarkup(potential))),
                Messages.ph("alerte", alert.isEmpty()
                        ? Component.empty() : messages.msg(alert))));
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
        return "";
    }
}
