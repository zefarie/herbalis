package io.github.zefarie.herbalis;

import io.github.zefarie.herbalis.application.usecase.AddBudToRackUseCase;
import io.github.zefarie.herbalis.application.usecase.AddToJarUseCase;
import io.github.zefarie.herbalis.application.usecase.BreakJarUseCase;
import io.github.zefarie.herbalis.application.usecase.BreakPlantUseCase;
import io.github.zefarie.herbalis.application.usecase.BreakPotUseCase;
import io.github.zefarie.herbalis.application.usecase.BreakRackUseCase;
import io.github.zefarie.herbalis.application.usecase.CollectJarUseCase;
import io.github.zefarie.herbalis.application.usecase.CollectRackUseCase;
import io.github.zefarie.herbalis.application.usecase.ConsumeUseCase;
import io.github.zefarie.herbalis.application.usecase.FertilizePlantUseCase;
import io.github.zefarie.herbalis.application.usecase.GrowPlantsUseCase;
import io.github.zefarie.herbalis.application.usecase.HarvestPlantUseCase;
import io.github.zefarie.herbalis.application.usecase.PlaceJarUseCase;
import io.github.zefarie.herbalis.application.usecase.PlacePotUseCase;
import io.github.zefarie.herbalis.application.usecase.PlaceRackUseCase;
import io.github.zefarie.herbalis.application.usecase.PlantSeedUseCase;
import io.github.zefarie.herbalis.application.usecase.PrunePlantUseCase;
import io.github.zefarie.herbalis.application.usecase.TreatPlantUseCase;
import io.github.zefarie.herbalis.application.usecase.WaterPlantUseCase;
import io.github.zefarie.herbalis.domain.drug.DrugRegistry;
import io.github.zefarie.herbalis.infrastructure.command.HerbalisCommand;
import io.github.zefarie.herbalis.infrastructure.config.DrugConfigLoader;
import io.github.zefarie.herbalis.infrastructure.config.HerbalisConfig;
import io.github.zefarie.herbalis.infrastructure.config.Messages;
import io.github.zefarie.herbalis.infrastructure.effects.BlackoutService;
import io.github.zefarie.herbalis.infrastructure.effects.EffectService;
import io.github.zefarie.herbalis.infrastructure.effects.WithdrawalService;
import io.github.zefarie.herbalis.infrastructure.fx.Fx;
import io.github.zefarie.herbalis.infrastructure.hud.HologramService;
import io.github.zefarie.herbalis.infrastructure.hud.HudService;
import io.github.zefarie.herbalis.infrastructure.item.ItemFactory;
import io.github.zefarie.herbalis.infrastructure.listener.ChunkListener;
import io.github.zefarie.herbalis.infrastructure.listener.ConnectionListener;
import io.github.zefarie.herbalis.infrastructure.listener.ConsumeListener;
import io.github.zefarie.herbalis.infrastructure.listener.CraftListener;
import io.github.zefarie.herbalis.infrastructure.listener.ItemUseListener;
import io.github.zefarie.herbalis.infrastructure.listener.MoveListener;
import io.github.zefarie.herbalis.infrastructure.listener.PlantInteractListener;
import io.github.zefarie.herbalis.infrastructure.listener.ProtectionListener;
import io.github.zefarie.herbalis.infrastructure.persistence.Database;
import io.github.zefarie.herbalis.infrastructure.persistence.SqliteConsumerRepository;
import io.github.zefarie.herbalis.infrastructure.persistence.SqliteJarRepository;
import io.github.zefarie.herbalis.infrastructure.persistence.SqlitePlantRepository;
import io.github.zefarie.herbalis.infrastructure.persistence.SqlitePotRepository;
import io.github.zefarie.herbalis.infrastructure.persistence.SqliteRackRepository;
import io.github.zefarie.herbalis.infrastructure.persistence.SqliteSessionStore;
import io.github.zefarie.herbalis.infrastructure.render.DisplayRenderer;
import io.github.zefarie.herbalis.infrastructure.render.WorldSync;
import io.github.zefarie.herbalis.infrastructure.scheduler.GrowthTicker;
import io.github.zefarie.herbalis.infrastructure.scheduler.JarTicker;
import io.github.zefarie.herbalis.infrastructure.scheduler.PlantSwayTicker;
import io.github.zefarie.herbalis.infrastructure.scheduler.PlayerTicker;
import io.github.zefarie.herbalis.infrastructure.scheduler.RackTicker;
import io.github.zefarie.herbalis.infrastructure.world.BukkitPlantEnvironment;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;

import java.io.File;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Point d'entree du plugin Herbalis : cable le domaine, les cas d'usage
 * et l'infrastructure Bukkit.
 */
public final class HerbalisPlugin extends JavaPlugin {

    private HerbalisConfig config;
    private Messages messages;
    private DrugRegistry drugs;
    private DrugConfigLoader drugLoader;
    private CraftListener craftListener;

    private Database database;
    private SqlitePlantRepository plantRepo;
    private WorldSync worldSync;
    private HologramService holograms;

    @Override
    public void onEnable() {
        // Configuration et definitions de drogues.
        saveDefaultConfig();
        saveResourceIfMissing("messages.yml");
        saveResourceIfMissing("drugs/weed.yml");

        config = HerbalisConfig.from(getConfig());
        messages = new Messages(YamlConfiguration.loadConfiguration(
                new File(getDataFolder(), "messages.yml")));
        drugLoader = new DrugConfigLoader(getLogger());
        drugs = new DrugRegistry();
        drugLoader.loadAll(new File(getDataFolder(), "drugs"))
                .forEach(drugs::register);
        getLogger().info(drugs.all().size() + " drogue(s) chargee(s).");

        // Persistence.
        database = new Database(new File(getDataFolder(), "herbalis.db"), getLogger());
        var potRepo = new SqlitePotRepository(database);
        plantRepo = new SqlitePlantRepository(database);
        var rackRepo = new SqliteRackRepository(database);
        var jarRepo = new SqliteJarRepository(database);
        var consumerRepo = new SqliteConsumerRepository(database);
        var sessionStore = new SqliteSessionStore(database);
        getLogger().info(potRepo.all().size() + " pot(s), "
                + plantRepo.all().size() + " plante(s), "
                + rackRepo.all().size() + " rack(s), "
                + jarRepo.all().size() + " jarre(s) charges.");

        // Briques d'infrastructure.
        var items = new ItemFactory(messages);
        var fx = new Fx(config);
        var renderer = new DisplayRenderer(this);
        var environment = new BukkitPlantEnvironment();
        worldSync = new WorldSync(renderer, potRepo, plantRepo, rackRepo,
                jarRepo, drugs);

        // Cas d'usage.
        long now = System.currentTimeMillis();
        var placePot = new PlacePotUseCase(potRepo);
        var breakPot = new BreakPotUseCase(potRepo, plantRepo);
        var plantSeed = new PlantSeedUseCase(potRepo, plantRepo, drugs);
        var waterPlant = new WaterPlantUseCase(plantRepo, drugs);
        var fertilizePlant = new FertilizePlantUseCase(plantRepo);
        var prunePlant = new PrunePlantUseCase(plantRepo, drugs);
        var treatPlant = new TreatPlantUseCase(plantRepo);
        var harvestPlant = new HarvestPlantUseCase(plantRepo, drugs,
                new java.util.Random());
        var breakPlant = new BreakPlantUseCase(plantRepo);
        var growPlants = new GrowPlantsUseCase(plantRepo, potRepo, drugs,
                environment, new java.util.Random(), config.dripperDecayFactor());
        var placeRack = new PlaceRackUseCase(rackRepo);
        var breakRack = new BreakRackUseCase(rackRepo);
        var addBud = new AddBudToRackUseCase(rackRepo, drugs);
        var collectRack = new CollectRackUseCase(rackRepo, drugs);
        var placeJar = new PlaceJarUseCase(jarRepo);
        var breakJar = new BreakJarUseCase(jarRepo);
        var addToJar = new AddToJarUseCase(jarRepo, drugs);
        var collectJar = new CollectJarUseCase(jarRepo, drugs);
        var consume = new ConsumeUseCase(consumerRepo, drugs);

        // Services.
        var blackout = new BlackoutService(messages, fx);
        var effects = new EffectService(messages, fx, drugs, sessionStore, blackout);
        var withdrawal = new WithdrawalService(consumerRepo, drugs, config, messages, fx);
        var hud = new HudService(config, messages, items, renderer, plantRepo,
                potRepo, rackRepo, jarRepo, drugs, environment);
        holograms = new HologramService(this, config, hud);

        // Reprise apres redemarrage : sessions et visuels.
        sessionStore.loadAll().forEach((id, stored) -> effects.restore(id, stored, now));
        worldSync.respawnLoadedChunks();

        // Listeners.
        craftListener = new CraftListener(items, drugs, config);
        craftListener.registerRecipes(this);
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new ItemUseListener(messages, fx, items, renderer, drugs,
                placePot, placeRack, placeJar), this);
        pm.registerEvents(new PlantInteractListener(messages, fx, items, renderer,
                drugs, config, hud, plantRepo, potRepo, rackRepo, jarRepo,
                plantSeed, waterPlant, fertilizePlant, prunePlant, treatPlant,
                harvestPlant, breakPlant, breakPot, addBud, collectRack,
                breakRack, addToJar, collectJar, breakJar), this);
        pm.registerEvents(new ProtectionListener(config, messages, fx, items,
                renderer, drugs, potRepo, plantRepo, rackRepo, jarRepo, breakPlant,
                breakPot, breakRack, breakJar), this);
        pm.registerEvents(new ChunkListener(worldSync), this);
        pm.registerEvents(new ConsumeListener(messages, drugs, consume, effects), this);
        pm.registerEvents(new ConnectionListener(blackout, withdrawal,
                holograms), this);
        pm.registerEvents(new MoveListener(blackout), this);
        pm.registerEvents(craftListener, this);

        // Commande.
        var command = new HerbalisCommand(messages, items, drugs, config,
                plantRepo, consumerRepo, hud, this::reloadEverything);
        PluginCommand pluginCommand = getCommand("herbalis");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        // Taches planifiees : un scheduler global par preoccupation.
        var scheduler = getServer().getScheduler();
        long growthPeriod = 20L * config.growthTickSeconds();
        scheduler.runTaskTimer(this,
                new GrowthTicker(growPlants, drugs, potRepo, renderer, fx),
                growthPeriod, growthPeriod);
        scheduler.runTaskTimer(this,
                new RackTicker(rackRepo, drugs, environment, renderer, fx), 60L, 60L);
        scheduler.runTaskTimer(this,
                new JarTicker(jarRepo, drugs, environment, renderer, fx), 70L, 60L);
        scheduler.runTaskTimer(this,
                new PlantSwayTicker(renderer, plantRepo, potRepo, drugs, config, fx),
                PlantSwayTicker.PERIOD_TICKS, PlantSwayTicker.PERIOD_TICKS);
        scheduler.runTaskTimer(this,
                new PlayerTicker(effects, withdrawal), 20L, 20L);
        scheduler.runTaskTimer(this, () -> {
            long tick = System.currentTimeMillis();
            getServer().getOnlinePlayers().forEach(
                    player -> holograms.tick(player, tick));
        }, 10L, 10L);
        long autosaveTicks = config.autosaveInterval().toSeconds() * 20L;
        scheduler.runTaskTimer(this, plantRepo::flush, autosaveTicks, autosaveTicks);

        getLogger().info("Herbalis actif.");
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
        if (holograms != null) {
            holograms.clearAll();
        }
        if (worldSync != null) {
            // Les entites sont recreees a la prochaine activation.
            worldSync.purgeLoadedChunks();
        }
        if (plantRepo != null) {
            plantRepo.flushSync();
        }
        if (database != null) {
            database.close();
        }
        getLogger().info("Herbalis arrete, donnees sauvegardees.");
    }

    /** Recharge config.yml, messages.yml et les definitions de drogues. */
    private void reloadEverything() {
        reloadConfig();
        config.reload(getConfig());
        messages.reload(YamlConfiguration.loadConfiguration(
                new File(getDataFolder(), "messages.yml")));
        drugs.clear();
        drugLoader.loadAll(new File(getDataFolder(), "drugs"))
                .forEach(drugs::register);
        craftListener.registerRecipes(this);
        getLogger().info("Configuration rechargee (" + drugs.all().size()
                + " drogue(s)).");
    }

    private void saveResourceIfMissing(String path) {
        if (!new File(getDataFolder(), path).exists()) {
            saveResource(path, false);
        }
    }
}
