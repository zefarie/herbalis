package io.github.zefarie.herbalis.infrastructure.item;

import io.github.zefarie.herbalis.domain.drug.DrugType;
import io.github.zefarie.herbalis.domain.plant.Plant;
import io.github.zefarie.herbalis.domain.quality.Quality;
import io.github.zefarie.herbalis.infrastructure.config.Messages;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Fabrique des items custom. Chaque item est un {@code minecraft:paper}
 * habille par le composant {@code item_model} (resource pack) et
 * identifie par son PDC. Les noms et lores viennent de messages.yml.
 */
public final class ItemFactory {

    private final Messages messages;

    public ItemFactory(Messages messages) {
        this.messages = messages;
    }

    // ----------------------------------------------------------------
    // Items generiques
    // ----------------------------------------------------------------

    public ItemStack pot() {
        return generic(HerbalisItemType.POT, 16);
    }

    public ItemStack dryingRack() {
        return generic(HerbalisItemType.DRYING_RACK, 16);
    }

    public ItemStack curingJar() {
        return generic(HerbalisItemType.CURING_JAR, 16);
    }

    public ItemStack fertilizer() {
        return generic(HerbalisItemType.FERTILIZER, 16);
    }

    public ItemStack rollingPaper() {
        return generic(HerbalisItemType.ROLLING_PAPER, 16);
    }

    public ItemStack emptyPouch() {
        return generic(HerbalisItemType.POUCH_EMPTY, 16);
    }

    /** Arrosoir : les charges restantes s'affichent via la barre de durabilite. */
    public ItemStack wateringCan(int maxCharges) {
        ItemStack stack = base(HerbalisItemType.WATERING_CAN, null, null);
        stack.setData(DataComponentTypes.MAX_STACK_SIZE, 1);
        stack.setData(DataComponentTypes.MAX_DAMAGE, maxCharges);
        stack.setData(DataComponentTypes.DAMAGE, 0);
        return stack;
    }

    // ----------------------------------------------------------------
    // Items lies a une drogue
    // ----------------------------------------------------------------

    /** Graine commune, sans lignee particuliere. */
    public ItemStack seed(DrugType drug) {
        return seed(drug, Quality.of(Plant.DEFAULT_SEED_QUALITY));
    }

    /** Graine issue d'une lignee : sa qualite pese sur la recolte. */
    public ItemStack seed(DrugType drug, Quality quality) {
        return qualityItem(HerbalisItemType.SEED, drug, quality);
    }

    public ItemStack freshBud(DrugType drug, Quality quality) {
        return qualityItem(HerbalisItemType.BUD_FRESH, drug, quality);
    }

    public ItemStack dried(DrugType drug, Quality quality) {
        return qualityItem(HerbalisItemType.DRIED, drug, quality);
    }

    public ItemStack pouch(DrugType drug, Quality quality) {
        return qualityItem(HerbalisItemType.POUCH, drug, quality);
    }

    /** Joint : consommable vanilla, animation portee a la bouche. */
    public ItemStack joint(DrugType drug, Quality quality) {
        ItemStack stack = qualityItem(HerbalisItemType.JOINT, drug, quality);
        stack.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
                .consumeSeconds(2.8f)
                .animation(ItemUseAnimation.TOOT_HORN)
                .sound(Key.key("minecraft", "block.campfire.crackle"))
                .hasConsumeParticles(false)
                .build());
        return stack;
    }

    // ----------------------------------------------------------------
    // Outils
    // ----------------------------------------------------------------

    /** Item de rendu pur pour les Item Display (aucune interaction inventaire). */
    public static ItemStack displayItem(String modelKey) {
        ItemStack stack = ItemStack.of(Material.PAPER);
        stack.setData(DataComponentTypes.ITEM_MODEL, Key.key(ItemKeys.NAMESPACE, modelKey));
        return stack;
    }

    /**
     * Reconstruit l'item de reference d'un type donne (pour comparaison
     * de recettes ou /herbalis give).
     */
    public Optional<ItemStack> byId(String itemId, List<DrugType> drugs,
                                    int maxCharges) {
        for (HerbalisItemType type : HerbalisItemType.values()) {
            if (!type.isDrugScoped() && type.id().equals(itemId)) {
                return Optional.of(switch (type) {
                    case WATERING_CAN -> wateringCan(maxCharges);
                    default -> generic(type, 16);
                });
            }
        }
        for (DrugType drug : drugs) {
            for (HerbalisItemType type : HerbalisItemType.values()) {
                if (type.isDrugScoped() && (drug.id() + "_" + type.id()).equals(itemId)) {
                    return Optional.of(switch (type) {
                        case SEED -> seed(drug);
                        case BUD_FRESH -> freshBud(drug, Quality.of(3));
                        case DRIED -> dried(drug, Quality.of(3));
                        case POUCH -> pouch(drug, Quality.of(3));
                        case JOINT -> joint(drug, Quality.of(3));
                        default -> throw new IllegalStateException();
                    });
                }
            }
        }
        return Optional.empty();
    }

    /** Recree l'item avec une autre qualite (heritage lors des crafts). */
    public ItemStack withQuality(ItemStack stack, DrugType drug, Quality quality) {
        HerbalisItemType type = ItemKeys.typeOf(stack).orElseThrow();
        return switch (type) {
            case SEED -> seed(drug, quality);
            case BUD_FRESH -> freshBud(drug, quality);
            case DRIED -> dried(drug, quality);
            case POUCH -> pouch(drug, quality);
            case JOINT -> joint(drug, quality);
            default -> stack.clone();
        };
    }

    /** Ligne d'etoiles de qualite, stylisee via messages.yml. */
    public String starsMarkup(Quality quality) {
        String full = messages.raw("qualite.etoile-pleine", "<color:#fbbf24>✦</color>");
        String hollow = messages.raw("qualite.etoile-vide", "<color:#4b5563>✧</color>");
        return full.repeat(quality.stars())
                + hollow.repeat(Quality.MAX - quality.stars());
    }

    // ----------------------------------------------------------------
    // Interne
    // ----------------------------------------------------------------

    private ItemStack generic(HerbalisItemType type, int stackSize) {
        ItemStack stack = base(type, null, null);
        stack.setData(DataComponentTypes.MAX_STACK_SIZE, stackSize);
        return stack;
    }

    private ItemStack qualityItem(HerbalisItemType type, DrugType drug, Quality quality) {
        ItemStack stack = base(type, drug, quality);
        stack.setData(DataComponentTypes.MAX_STACK_SIZE, 16);
        return stack;
    }

    private ItemStack base(HerbalisItemType type, DrugType drug, Quality quality) {
        ItemStack stack = ItemStack.of(Material.PAPER);

        String modelKey = drug == null
                ? type.id()
                : drug.id() + "_" + type.id();
        stack.setData(DataComponentTypes.ITEM_MODEL, Key.key(ItemKeys.NAMESPACE, modelKey));

        List<TagResolver> resolvers = new ArrayList<>();
        if (drug != null) {
            resolvers.add(Messages.ph("drogue", drug.displayName()));
        }
        if (quality != null) {
            resolvers.add(Messages.ph("etoiles", messages.deserialize(starsMarkup(quality))));
        }
        TagResolver[] tags = resolvers.toArray(TagResolver[]::new);

        Component name = messages.msg("items." + type.id() + ".nom", tags);
        stack.setData(DataComponentTypes.ITEM_NAME, name);

        List<Component> lore = messages.list("items." + type.id() + ".lore", tags);
        if (!lore.isEmpty()) {
            stack.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        }

        stack.editMeta(meta -> {
            var pdc = meta.getPersistentDataContainer();
            pdc.set(ItemKeys.TYPE, PersistentDataType.STRING, type.id());
            if (drug != null) {
                pdc.set(ItemKeys.DRUG, PersistentDataType.STRING, drug.id());
            }
            if (quality != null) {
                pdc.set(ItemKeys.QUALITY, PersistentDataType.INTEGER, quality.stars());
            }
        });
        return stack;
    }
}
