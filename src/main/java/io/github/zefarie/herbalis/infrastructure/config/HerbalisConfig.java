package io.github.zefarie.herbalis.infrastructure.config;

import io.github.zefarie.herbalis.domain.irrigation.TankSize;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.Duration;

/**
 * Vue typee de config.yml, rechargeable a chaud : les services gardent
 * leur reference, {@link #reload} rafraichit les valeurs en place.
 */
public final class HerbalisConfig {

    private record Data(
            int growthTickSeconds,
            Duration autosaveInterval,
            boolean particlesEnabled,
            boolean soundsEnabled,
            boolean plantSwayEnabled,
            boolean hudEnabled,
            double hudRange,
            int wateringCanCharges,
            int sprayerCharges,
            double dripperDecayFactor,
            double waterPerBucket,
            int tankBucketsCuve,
            int tankBucketsCiterne,
            int tankBucketsReservoir,
            int siloCapacityDoses,
            int lampLightLevel,
            int shearsWearPerPruning,
            boolean dropSeedOnBreak,
            boolean explosionKillsPlants,
            double explosionRadius,
            Duration withdrawalSymptomMin,
            Duration withdrawalSymptomMax
    ) {
    }

    private volatile Data data;

    private HerbalisConfig(Data data) {
        this.data = data;
    }

    public static HerbalisConfig from(FileConfiguration config) {
        return new HerbalisConfig(parse(config));
    }

    public void reload(FileConfiguration config) {
        this.data = parse(config);
    }

    private static Data parse(FileConfiguration config) {
        return new Data(
                Math.max(1, config.getInt("croissance.tick-secondes", 1)),
                DurationParser.parse(config.getString("persistence.autosave", "5m")),
                config.getBoolean("effets-visuels.particules", true),
                config.getBoolean("effets-visuels.sons", true),
                config.getBoolean("effets-visuels.animation-plantes", true),
                config.getBoolean("hud.actif", true),
                config.getDouble("hud.portee", 5.0),
                Math.max(1, config.getInt("arrosoir.charges", 8)),
                Math.max(1, config.getInt("pulverisateur.charges", 6)),
                Math.clamp(config.getDouble("goutte-a-goutte.facteur-perte", 0.5),
                        0.0, 1.0),
                Math.max(1.0, config.getDouble("irrigation.eau-par-seau", 100.0)),
                Math.max(1, config.getInt("irrigation.capacite-seaux.cuve", 16)),
                Math.max(1, config.getInt("irrigation.capacite-seaux.citerne", 64)),
                Math.max(1, config.getInt("irrigation.capacite-seaux.reservoir", 256)),
                Math.max(1, config.getInt("silo.capacite-doses", 16)),
                Math.clamp(config.getInt("lampe.niveau-lumiere", 15), 1, 15),
                Math.max(0, config.getInt("taille.usure-cisailles", 1)),
                config.getBoolean("plantes.drop-graine-si-cassee", true),
                config.getBoolean("plantes.explosion-detruit", true),
                config.getDouble("plantes.rayon-explosion", 4.0),
                DurationParser.parse(config.getString("manque.symptome-min", "40s")),
                DurationParser.parse(config.getString("manque.symptome-max", "90s"))
        );
    }

    public int growthTickSeconds() {
        return data.growthTickSeconds();
    }

    public Duration autosaveInterval() {
        return data.autosaveInterval();
    }

    public boolean particlesEnabled() {
        return data.particlesEnabled();
    }

    public boolean soundsEnabled() {
        return data.soundsEnabled();
    }

    public boolean plantSwayEnabled() {
        return data.plantSwayEnabled();
    }

    public boolean hudEnabled() {
        return data.hudEnabled();
    }

    public double hudRange() {
        return data.hudRange();
    }

    public int wateringCanCharges() {
        return data.wateringCanCharges();
    }

    public int sprayerCharges() {
        return data.sprayerCharges();
    }

    public double dripperDecayFactor() {
        return data.dripperDecayFactor();
    }

    /** Points d'hydratation gagnes par seau verse dans un caisson. */
    public double waterPerBucket() {
        return data.waterPerBucket();
    }

    /** Capacite d'un caisson, en seaux. */
    public int tankCapacityBuckets(TankSize size) {
        return switch (size) {
            case CUVE -> data.tankBucketsCuve();
            case CITERNE -> data.tankBucketsCiterne();
            case RESERVOIR -> data.tankBucketsReservoir();
        };
    }

    /** Capacite d'un caisson, en points d'hydratation. */
    public double tankCapacity(TankSize size) {
        return tankCapacityBuckets(size) * data.waterPerBucket();
    }

    public int siloCapacityDoses() {
        return data.siloCapacityDoses();
    }

    public int lampLightLevel() {
        return data.lampLightLevel();
    }

    public int shearsWearPerPruning() {
        return data.shearsWearPerPruning();
    }

    public boolean dropSeedOnBreak() {
        return data.dropSeedOnBreak();
    }

    public boolean explosionKillsPlants() {
        return data.explosionKillsPlants();
    }

    public double explosionRadius() {
        return data.explosionRadius();
    }

    public Duration withdrawalSymptomMin() {
        return data.withdrawalSymptomMin();
    }

    public Duration withdrawalSymptomMax() {
        return data.withdrawalSymptomMax();
    }
}
