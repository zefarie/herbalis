package io.github.zefarie.herbalis.infrastructure.command;

import io.github.zefarie.herbalis.application.port.ConsumerRepository;
import io.github.zefarie.herbalis.application.port.PlantRepository;
import io.github.zefarie.herbalis.domain.consumption.ConsumerProfile;
import io.github.zefarie.herbalis.domain.consumption.ConsumptionEngine;
import io.github.zefarie.herbalis.domain.drug.DrugRegistry;
import io.github.zefarie.herbalis.domain.drug.DrugType;
import io.github.zefarie.herbalis.domain.plant.Plant;
import io.github.zefarie.herbalis.domain.quality.Quality;
import io.github.zefarie.herbalis.domain.quality.QualityCalculator;
import io.github.zefarie.herbalis.infrastructure.config.HerbalisConfig;
import io.github.zefarie.herbalis.infrastructure.config.Messages;
import io.github.zefarie.herbalis.infrastructure.hud.HudService;
import io.github.zefarie.herbalis.infrastructure.item.HerbalisItemType;
import io.github.zefarie.herbalis.infrastructure.item.ItemFactory;
import io.github.zefarie.herbalis.infrastructure.item.ItemKeys;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * /herbalis give|info|tolerance|reload, avec tab completion complete.
 */
public final class HerbalisCommand implements TabExecutor {

    /** Recharge la config a chaud (implante par le plugin principal). */
    @FunctionalInterface
    public interface Reloader {
        void reload();
    }

    private final Messages messages;
    private final ItemFactory items;
    private final DrugRegistry drugs;
    private final HerbalisConfig config;
    private final PlantRepository plants;
    private final ConsumerRepository consumers;
    private final HudService hud;
    private final Reloader reloader;

    public HerbalisCommand(Messages messages, ItemFactory items, DrugRegistry drugs,
                           HerbalisConfig config, PlantRepository plants,
                           ConsumerRepository consumers, HudService hud,
                           Reloader reloader) {
        this.messages = messages;
        this.items = items;
        this.drugs = drugs;
        this.config = config;
        this.plants = plants;
        this.consumers = consumers;
        this.hud = hud;
        this.reloader = reloader;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(messages.msg("commande.usage"));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> give(sender, args);
            case "info" -> info(sender);
            case "tolerance" -> tolerance(sender, args);
            case "reload" -> reload(sender);
            default -> sender.sendMessage(messages.msg("commande.usage"));
        }
        return true;
    }

    private void give(CommandSender sender, String[] args) {
        if (!sender.hasPermission("herbalis.admin")) {
            sender.sendMessage(messages.msg("erreurs.permission"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(messages.msg("commande.give-usage"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(messages.msg("erreurs.joueur-introuvable",
                    Messages.ph("joueur", args[1])));
            return;
        }
        String itemId = args[2].toLowerCase(Locale.ROOT);
        int amount = args.length >= 4 ? parseInt(args[3], 1) : 1;
        amount = Math.clamp(amount, 1, 64);

        Optional<ItemStack> built = items.byId(itemId,
                List.copyOf(drugs.all()), config.wateringCanCharges(),
                config.secateurUses());
        if (built.isEmpty()) {
            sender.sendMessage(messages.msg("erreurs.item-inconnu",
                    Messages.ph("item", itemId)));
            return;
        }
        ItemStack stack = built.get();

        if (args.length >= 5) {
            int stars = parseInt(args[4], 3);
            Optional<DrugType> drug = ItemKeys.drugOf(stack).flatMap(drugs::byId);
            if (drug.isPresent() && ItemKeys.qualityOf(stack).isPresent()) {
                stack = items.withQuality(stack, drug.get(),
                        Quality.of(Math.clamp(stars, Quality.MIN, Quality.MAX)));
            }
        }

        int remaining = amount;
        while (remaining > 0) {
            ItemStack batch = stack.clone();
            int size = Math.min(remaining, batch.getMaxStackSize());
            batch.setAmount(size);
            remaining -= size;
            target.getInventory().addItem(batch).values().forEach(rest ->
                    target.getWorld().dropItemNaturally(target.getLocation(), rest));
        }
        sender.sendMessage(messages.msg("commande.give-ok",
                Messages.ph("quantite", String.valueOf(amount)),
                Messages.ph("item", itemId),
                Messages.ph("joueur", target.getName())));
    }

    private void info(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.msg("erreurs.joueur-seulement"));
            return;
        }
        if (!player.hasPermission("herbalis.info")) {
            player.sendMessage(messages.msg("erreurs.permission"));
            return;
        }
        var pos = hud.targetPos(player).orElse(null);
        if (pos == null) {
            player.sendMessage(messages.msg("commande.info-aucune-cible"));
            return;
        }
        Optional<Plant> plant = plants.at(pos);
        if (plant.isEmpty()) {
            hud.buildLine(pos, System.currentTimeMillis())
                    .ifPresent(player::sendMessage);
            return;
        }
        Plant p = plant.get();
        DrugType drug = drugs.byId(p.drugId()).orElse(null);
        if (drug == null) {
            return;
        }
        long stageTotal = drug.growth().durationOf(
                Math.min(p.stage(), drug.growth().stageCount())).toMillis();
        int progress = drug.growth().isFinalStage(p.stage()) ? 100
                : (int) Math.round(100.0 * p.stageGrowthMillis() / Math.max(1, stageTotal));
        Quality potential = QualityCalculator.harvestQuality(p, drug);

        player.sendMessage(messages.msg("commande.info-plante",
                Messages.ph("drogue", drug.displayName()),
                Messages.ph("stage", String.valueOf(p.stage())),
                Messages.ph("stages", String.valueOf(drug.growth().stageCount())),
                Messages.ph("progression", String.valueOf(progress)),
                Messages.ph("hydratation", String.valueOf((int) p.hydration())),
                Messages.ph("hydratation_moyenne", String.valueOf((int) p.averageHydration())),
                Messages.ph("engrais", String.valueOf(p.fertilizerUses())),
                Messages.ph("etat", p.state().name()),
                Messages.ph("etoiles", messages.deserialize(items.starsMarkup(potential)))));
    }

    private void tolerance(CommandSender sender, String[] args) {
        if (!sender.hasPermission("herbalis.admin")) {
            sender.sendMessage(messages.msg("erreurs.permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(messages.msg("commande.tolerance-usage"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(messages.msg("erreurs.joueur-introuvable",
                    Messages.ph("joueur", args[1])));
            return;
        }
        long now = System.currentTimeMillis();
        if (args.length >= 3 && args[2].equalsIgnoreCase("reset")) {
            consumers.put(target.getUniqueId(), ConsumerProfile.fresh(now));
            sender.sendMessage(messages.msg("commande.tolerance-reset",
                    Messages.ph("joueur", target.getName())));
            return;
        }
        DrugType drug = drugs.all().stream().findFirst().orElse(null);
        ConsumerProfile profile = consumers.of(target.getUniqueId(), now);
        if (drug != null) {
            profile = ConsumptionEngine.decayed(profile, now, drug.consumption());
        }
        sender.sendMessage(messages.msg("commande.tolerance-info",
                Messages.ph("joueur", target.getName()),
                Messages.ph("tolerance", String.valueOf((int) profile.tolerance())),
                Messages.ph("addiction", String.valueOf((int) profile.addiction()))));
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("herbalis.admin")) {
            sender.sendMessage(messages.msg("erreurs.permission"));
            return;
        }
        reloader.reload();
        sender.sendMessage(messages.msg("commande.reload-ok"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            if (sender.hasPermission("herbalis.info")) {
                subs.add("info");
            }
            if (sender.hasPermission("herbalis.admin")) {
                subs.addAll(List.of("give", "tolerance", "reload"));
            }
            return filter(subs, args[0]);
        }
        if (!sender.hasPermission("herbalis.admin")) {
            return List.of();
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> switch (args.length) {
                case 2 -> filter(playerNames(), args[1]);
                case 3 -> filter(itemIds(), args[2]);
                case 4 -> filter(List.of("1", "8", "16", "64"), args[3]);
                case 5 -> filter(List.of("1", "2", "3", "4", "5"), args[4]);
                default -> List.of();
            };
            case "tolerance" -> switch (args.length) {
                case 2 -> filter(playerNames(), args[1]);
                case 3 -> filter(List.of("reset"), args[2]);
                default -> List.of();
            };
            default -> List.of();
        };
    }

    private List<String> itemIds() {
        List<String> ids = new ArrayList<>();
        for (HerbalisItemType type : HerbalisItemType.values()) {
            if (!type.isDrugScoped()) {
                ids.add(type.id());
            }
        }
        for (DrugType drug : drugs.all()) {
            for (HerbalisItemType type : HerbalisItemType.values()) {
                if (type.isDrugScoped()) {
                    ids.add(drug.id() + "_" + type.id());
                }
            }
        }
        return ids;
    }

    private static List<String> playerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .map(String::toString)
                .toList();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower))
                .sorted()
                .toList();
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
