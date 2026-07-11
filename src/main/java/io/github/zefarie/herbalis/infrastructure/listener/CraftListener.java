package io.github.zefarie.herbalis.infrastructure.listener;

import com.destroystokyo.paper.event.inventory.PrepareResultEvent;
import io.github.zefarie.herbalis.domain.drug.DrugRegistry;
import io.github.zefarie.herbalis.domain.drug.DrugType;
import io.github.zefarie.herbalis.domain.quality.Quality;
import io.github.zefarie.herbalis.infrastructure.item.HerbalisItemType;
import io.github.zefarie.herbalis.infrastructure.item.ItemFactory;
import io.github.zefarie.herbalis.infrastructure.item.ItemKeys;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Craft du joint (pochon + feuille a rouler, qualite heritee) et garde-fou
 * contre l'utilisation des items Herbalis dans les recettes vanilla.
 */
public final class CraftListener implements Listener {

    private final ItemFactory items;
    private final DrugRegistry drugs;

    public CraftListener(ItemFactory items, DrugRegistry drugs) {
        this.items = items;
        this.drugs = drugs;
    }

    /** Enregistre une recette de joint par drogue (idempotent, reload inclus). */
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
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        ItemStack[] matrix = event.getInventory().getMatrix();
        boolean hasHerbalisInput = false;
        ItemStack pouch = null;
        for (ItemStack stack : matrix) {
            Optional<HerbalisItemType> type = ItemKeys.typeOf(stack);
            if (type.isPresent()) {
                hasHerbalisInput = true;
                if (type.get() == HerbalisItemType.POUCH) {
                    pouch = stack;
                }
            }
        }
        if (!hasHerbalisInput) {
            return;
        }

        ItemStack result = event.getInventory().getResult();
        boolean resultIsJoint = ItemKeys.typeOf(result)
                .filter(type -> type == HerbalisItemType.JOINT).isPresent();
        if (resultIsJoint && pouch != null) {
            // Le joint herite de la qualite du pochon utilise.
            Quality quality = ItemKeys.qualityOf(pouch).orElse(Quality.of(1));
            Optional<DrugType> drug = ItemKeys.drugOf(pouch).flatMap(drugs::byId);
            drug.ifPresent(d -> event.getInventory()
                    .setResult(items.joint(d, quality)));
            return;
        }
        if (!resultIsJoint && result != null) {
            // Pas question de transformer un pochon en carte ou en livre.
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
