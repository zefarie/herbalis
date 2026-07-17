package io.github.zefarie.herbalis.infrastructure.config;

import io.github.zefarie.herbalis.domain.drug.ConsumptionRules;
import io.github.zefarie.herbalis.domain.drug.CuringProfile;
import io.github.zefarie.herbalis.domain.drug.DrugType;
import io.github.zefarie.herbalis.domain.drug.DryingProfile;
import io.github.zefarie.herbalis.domain.drug.ToppingProfile;
import io.github.zefarie.herbalis.domain.drug.EffectProfile;
import io.github.zefarie.herbalis.domain.drug.EffectSpec;
import io.github.zefarie.herbalis.domain.drug.FertilizerProfile;
import io.github.zefarie.herbalis.domain.drug.GrowthProfile;
import io.github.zefarie.herbalis.domain.drug.HarvestWindow;
import io.github.zefarie.herbalis.domain.drug.HydrationProfile;
import io.github.zefarie.herbalis.domain.drug.PestProfile;
import io.github.zefarie.herbalis.domain.quality.QualityWeights;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Charge les definitions de drogues depuis le dossier {@code drugs/}.
 * Un fichier YAML par drogue : ajouter une drogue ne demande aucun code.
 */
public final class DrugConfigLoader {

    private final Logger logger;

    public DrugConfigLoader(Logger logger) {
        this.logger = logger;
    }

    public List<DrugType> loadAll(File drugsFolder) {
        File[] files = drugsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            logger.warning("Aucune drogue trouvee dans " + drugsFolder.getPath());
            return List.of();
        }
        List<DrugType> types = new ArrayList<>();
        for (File file : files) {
            try {
                types.add(load(YamlConfiguration.loadConfiguration(file),
                        file.getName().replace(".yml", "")));
            } catch (RuntimeException e) {
                logger.severe("Drogue invalide (" + file.getName() + ") : " + e.getMessage());
            }
        }
        return types;
    }

    private DrugType load(YamlConfiguration yaml, String fallbackId) {
        String id = yaml.getString("id", fallbackId);
        String displayName = yaml.getString("nom", id);

        ConfigurationSection growth = section(yaml, "croissance");
        List<Duration> stageDurations = growth.getStringList("durees-stages").stream()
                .map(DurationParser::parse)
                .toList();
        GrowthProfile growthProfile = new GrowthProfile(
                stageDurations.size(),
                stageDurations,
                growth.getInt("lumiere-minimum", 12),
                growth.getInt("rendement-min", 2),
                growth.getInt("rendement-max", 5));

        ConfigurationSection hydration = section(yaml, "hydratation");
        HydrationProfile hydrationProfile = new HydrationProfile(
                hydration.getDouble("perte-par-minute", 2.5),
                hydration.getDouble("restauration-arrosage", 40.0),
                hydration.getDouble("seuil-soif", 25.0),
                DurationParser.parse(hydration.getString("delai-jaunissement", "4m")),
                DurationParser.parse(hydration.getString("delai-mort", "12m")));

        ConfigurationSection fertilizer = section(yaml, "engrais");
        FertilizerProfile fertilizerProfile = new FertilizerProfile(
                fertilizer.getDouble("acceleration", 0.5),
                fertilizer.getDouble("bonus-qualite", 0.25));

        ConfigurationSection window = section(yaml, "fenetre-recolte");
        HarvestWindow harvestWindow = new HarvestWindow(
                DurationParser.parse(window.getString("duree-optimale", "10m")),
                DurationParser.parse(window.getString("duree-declin", "20m")));

        ConfigurationSection drying = section(yaml, "sechage");
        DryingProfile dryingProfile = new DryingProfile(
                DurationParser.parse(drying.getString("duree", "20m")),
                drying.getInt("capacite", 6));

        // Taille et curing : sections optionnelles, defauts raisonnables
        // pour les configs anterieures.
        ToppingProfile toppingProfile = ToppingProfile.DEFAULT;
        ConfigurationSection topping = yaml.getConfigurationSection("taille");
        if (topping != null) {
            List<Integer> stages = topping.getIntegerList("stages");
            toppingProfile = new ToppingProfile(
                    stages.isEmpty() ? ToppingProfile.DEFAULT.stages() : stages,
                    topping.getDouble("fenetre-debut", 0.30),
                    topping.getDouble("fenetre-fin", 0.60),
                    DurationParser.parse(topping.getString("rallonge", "2m")),
                    topping.getInt("bonus-tetes-min", 1),
                    topping.getInt("bonus-tetes-max", 2),
                    topping.getInt("malus-etoiles-rate", 1));
        }
        CuringProfile curingProfile = CuringProfile.DEFAULT;
        ConfigurationSection curing = yaml.getConfigurationSection("curing");
        if (curing != null) {
            curingProfile = new CuringProfile(
                    DurationParser.parse(curing.getString("duree", "45m")),
                    DurationParser.parse(curing.getString("delai-moisissure", "90m")),
                    curing.getInt("bonus-etoiles", 1),
                    curing.getInt("capacite", 6));
        }

        // Nuisibles : desactives si la section est absente, pour ne pas
        // surprendre une config anterieure.
        PestProfile pestProfile = PestProfile.DISABLED;
        ConfigurationSection pests = yaml.getConfigurationSection("nuisibles");
        if (pests != null) {
            pestProfile = new PestProfile(
                    pests.getDouble("chance-par-jour", PestProfile.DEFAULT.dailyChance()),
                    pests.getDouble("ralentissement", PestProfile.DEFAULT.slowdown()),
                    DurationParser.parse(pests.getString("delai-degats", "12h")),
                    pests.getInt("malus-etoiles", PestProfile.DEFAULT.damageStars()));
        }

        ConfigurationSection effects = section(yaml, "effets");
        EffectProfile effectProfile = new EffectProfile(
                DurationParser.parse(effects.getString("montee", "15s")),
                DurationParser.parse(effects.getString("high-min", "2m")),
                DurationParser.parse(effects.getString("high-max", "6m")),
                effects.getDouble("ratio-descente", 0.35),
                effectSpecs(effects.getMapList("effets-high")),
                effectSpecs(effects.getMapList("effets-descente")));

        ConfigurationSection consumption = section(yaml, "consommation");
        ConsumptionRules rules = new ConsumptionRules(
                Math.max(1, consumption.getInt("taffes-par-joint", 3)),
                consumption.getInt("blackout-nombre", 3),
                DurationParser.parse(consumption.getString("blackout-fenetre", "5m")),
                DurationParser.parse(consumption.getString("blackout-duree", "30s")),
                consumption.getDouble("tolerance-gain", 12.0),
                consumption.getDouble("tolerance-perte-par-heure", 4.0),
                consumption.getDouble("tolerance-reduction-max", 0.6),
                consumption.getDouble("addiction-gain", 8.0),
                consumption.getDouble("addiction-perte-par-heure", 1.5),
                consumption.getDouble("addiction-seuil", 50.0),
                DurationParser.parse(consumption.getString("delai-manque", "45m")));

        // genetique vaut 0 par defaut : les configs anterieures (trois
        // poids sommant a 1) restent valides sans modification.
        ConfigurationSection weights = section(yaml, "poids-qualite");
        QualityWeights qualityWeights = new QualityWeights(
                weights.getDouble("hydratation", 0.40),
                weights.getDouble("engrais", 0.25),
                weights.getDouble("timing", 0.20),
                weights.getDouble("genetique", 0.0));

        return new DrugType(id, displayName, growthProfile, hydrationProfile,
                fertilizerProfile, harvestWindow, dryingProfile, toppingProfile,
                curingProfile, pestProfile, effectProfile, rules, qualityWeights);
    }

    private static List<EffectSpec> effectSpecs(List<Map<?, ?>> raw) {
        List<EffectSpec> specs = new ArrayList<>();
        for (Map<?, ?> entry : raw) {
            Object key = entry.get("effet");
            if (key == null) {
                continue;
            }
            Object amplifier = entry.get("amplificateur");
            Object scales = entry.get("augmente-avec-qualite");
            Object oneShot = entry.get("une-seule-fois");
            specs.add(new EffectSpec(
                    key.toString(),
                    amplifier instanceof Number n ? n.intValue() : 0,
                    scales instanceof Boolean b && b,
                    oneShot instanceof Boolean o && o));
        }
        return specs;
    }

    private static ConfigurationSection section(YamlConfiguration yaml, String path) {
        ConfigurationSection section = yaml.getConfigurationSection(path);
        if (section == null) {
            throw new IllegalArgumentException("Section manquante : " + path);
        }
        return section;
    }
}
