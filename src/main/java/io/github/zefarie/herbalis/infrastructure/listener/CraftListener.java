package io.github.zefarie.herbalis.infrastructure.listener;

import com.destroystokyo.paper.event.inventory.PrepareResultEvent;
import io.github.zefarie.herbalis.domain.drug.DrugRegistry;
import io.github.zefarie.herbalis.domain.drug.DrugType;
import io.github.zefarie.herbalis.domain.irrigation.TankSize;
import io.github.zefarie.herbalis.domain.quality.Quality;
import io.github.zefarie.herbalis.infrastructure.config.HerbalisConfig;
import io.github.zefarie.herbalis.infrastructure.item.HerbalisItemType;
import io.github.zefarie.herbalis.infrastructure.item.ItemFactory;
import io.github.zefarie.herbalis.infrastructure.item.ItemKeys;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Recettes de la pipeline (tout l'outillage se craft, sauf les graines),
 * craft du joint (pochon + feuille a rouler, qualite heritee) et
 * garde-fou contre l'utilisation des items Herbalis dans les recettes
 * vanilla.
 */
public final class CraftListener implements Listener {

    private final ItemFactory items;
    private final DrugRegistry drugs;
    private final HerbalisConfig config;

    public CraftListener(ItemFactory items, DrugRegistry drugs,
                         HerbalisConfig config) {
        this.items = items;
        this.drugs = drugs;
        this.config = config;
    }

    /** Enregistre toutes les recettes (idempotent, reload inclus). */
    public void registerRecipes(Plugin plugin) {
        for (DrugType drug : drugs.all()) {
            NamespacedKey key = new NamespacedKey(plugin, drug.id() + "_joint");
            Bukkit.removeRecipe(key);
            ShapelessRecipe recipe = new ShapelessRecipe(key,
                    items.joint(drug, Quality.of(3)));
            List<ItemStack> pouches = new ArrayList<>();
            for (int stars = Quality.MIN; stars <= Quality.MAX; stars++) {
                pouches.add(items.pouch(drug, Quality.of(stars)));
            }
            recipe.addIngredient(new RecipeChoice.ExactChoice(pouches));
            recipe.addIngredient(new RecipeChoice.ExactChoice(items.rollingPaper()));
            Bukkit.addRecipe(recipe);
        }
        registerToolRecipes(plugin);
    }

    /** L'outillage complet se craft avec des materiaux vanilla. */
    private void registerToolRecipes(Plugin plugin) {
        shaped(plugin, "pot", items.pot(),
                new String[]{"b b", "b b", "bbb"},
                Map.of('b', Material.BRICK));
        shaped(plugin, "drying_rack", items.dryingRack(),
                new String[]{"bbb", "sss", "b b"},
                Map.of('b', Material.STICK, 's', Material.STRING));
        shaped(plugin, "curing_jar", items.curingJar(),
                new String[]{" w ", "g g", "ggg"},
                Map.of('w', Material.OAK_SLAB, 'g', Material.GLASS));
        shaped(plugin, "watering_can", items.wateringCan(config.wateringCanCharges()),
                new String[]{"n  ", "iii", " i "},
                Map.of('n', Material.IRON_NUGGET, 'i', Material.IRON_INGOT));
        shaped(plugin, "sprayer", items.sprayer(config.sprayerCharges()),
                new String[]{"n", "i", "b"},
                Map.of('n', Material.IRON_NUGGET, 'i', Material.IRON_INGOT,
                        'b', Material.GLASS_BOTTLE));
        shaped(plugin, "dripper", items.dripper(),
                new String[]{"ggg", " s ", " r "},
                Map.of('g', Material.GLASS, 's', Material.STICK,
                        'r', Material.STRING));
        shaped(plugin, "pipe", withAmount(items.pipe(), 4),
                new String[]{"ccc"},
                Map.of('c', Material.COPPER_INGOT));
        shaped(plugin, "tank_cuve", items.tank(TankSize.CUVE),
                new String[]{"c c", "p p", "ppp"},
                Map.of('c', Material.COPPER_INGOT, 'p', Material.OAK_PLANKS));
        shaped(plugin, "tank_citerne", items.tank(TankSize.CITERNE),
                new String[]{"iii", "ibi", "iii"},
                Map.of('i', Material.IRON_INGOT, 'b', Material.BUCKET));
        shaped(plugin, "tank_reservoir", items.tank(TankSize.RESERVOIR),
                new String[]{"BiB", "ibi", "BiB"},
                Map.of('B', Material.IRON_BLOCK, 'i', Material.IRON_INGOT,
                        'b', Material.BUCKET));
        shaped(plugin, "silo", items.silo(),
                new String[]{"p p", "php", "ppp"},
                Map.of('p', Material.OAK_PLANKS, 'h', Material.HOPPER));
        shaped(plugin, "uv_lamp", items.uvLamp(),
                new String[]{"aaa", "grg", " i "},
                Map.of('a', Material.AMETHYST_SHARD, 'g', Material.GLASS,
                        'r', Material.REDSTONE, 'i', Material.IRON_INGOT));
        shapeless(plugin, "fertilizer", withAmount(items.fertilizer(), 2),
                Material.BONE_MEAL, Material.BONE_MEAL, Material.DIRT);
        shapeless(plugin, "rolling_paper", withAmount(items.rollingPaper(), 3),
                Material.PAPER, Material.SUGAR_CANE);
        shapeless(plugin, "pouch_empty", withAmount(items.emptyPouch(), 2),
                Material.LEATHER, Material.STRING);
    }

    private static void shaped(Plugin plugin, String name, ItemStack result,
                               String[] rows, Map<Character, Material> ingredients) {
        NamespacedKey key = new NamespacedKey(plugin, "craft_" + name);
        Bukkit.removeRecipe(key);
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(rows);
        ingredients.forEach(recipe::setIngredient);
        Bukkit.addRecipe(recipe);
    }

    private static void shapeless(Plugin plugin, String name, ItemStack result,
                                  Material... ingredients) {
        NamespacedKey key = new NamespacedKey(plugin, "craft_" + name);
        Bukkit.removeRecipe(key);
        ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        for (Material material : ingredients) {
            recipe.addIngredient(material);
        }
        Bukkit.addRecipe(recipe);
    }

    private static ItemStack withAmount(ItemStack stack, int amount) {
        stack.setAmount(amount);
        return stack;
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        ItemStack pouch = null;
        ItemStack paper = null;
        int herbalisInputs = 0;
        int totalInputs = 0;
        int otherInputs = 0;
        for (ItemStack stack : event.getInventory().getMatrix()) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            totalInputs++;
            Optional<HerbalisItemType> type = ItemKeys.typeOf(stack);
            if (type.isEmpty()) {
                otherInputs++;
                continue;
            }
            herbalisInputs++;
            switch (type.get()) {
                case POUCH -> pouch = stack;
                case ROLLING_PAPER -> paper = stack;
                default -> {
                }
            }
        }
        if (herbalisInputs == 0) {
            return;
        }

        // Un pochon + une feuille, rien d'autre : joint, qualite heritee.
        // Correspondance manuelle, plus robuste que l'ExactChoice de la
        // recette enregistree (les items anciens gardent leur vieux lore).
        if (totalInputs == 2 && pouch != null && paper != null && otherInputs == 0) {
            Quality quality = ItemKeys.qualityOf(pouch).orElse(Quality.of(1));
            Optional<DrugType> drug = ItemKeys.drugOf(pouch).flatMap(drugs::byId);
            if (drug.isPresent()) {
                event.getInventory().setResult(items.joint(drug.get(), quality));
                return;
            }
        }
        // Sinon, aucun item Herbalis ne participe a une recette vanilla.
        if (event.getInventory().getResult() != null) {
            event.getInventory().setResult(null);
        }
    }

    /** Anvil, cartographie, meule, forge : aucun item Herbalis n'y passe. */
    @EventHandler
    public void onPrepareResult(PrepareResultEvent event) {
        int size = event.getInventory().getSize();
        for (int slot = 0; slot < size - 1; slot++) {
            if (ItemKeys.isHerbalisItem(event.getInventory().getItem(slot))) {
                event.setResult(null);
                return;
            }
        }
    }
}
