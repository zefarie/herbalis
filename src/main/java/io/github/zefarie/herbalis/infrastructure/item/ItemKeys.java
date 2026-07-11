package io.github.zefarie.herbalis.infrastructure.item;

import io.github.zefarie.herbalis.domain.quality.Quality;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

/**
 * Cles PDC des items et entites Herbalis, et lecture typee.
 */
public final class ItemKeys {

    public static final String NAMESPACE = "herbalis";

    /** Type d'item custom (valeurs de {@link HerbalisItemType}). */
    public static final NamespacedKey TYPE = key("type");
    /** Identifiant de la drogue portee par l'item. */
    public static final NamespacedKey DRUG = key("drug");
    /** Qualite en etoiles (1 a 5). */
    public static final NamespacedKey QUALITY = key("quality");
    /** Charges restantes de l'arrosoir. */
    public static final NamespacedKey CHARGES = key("charges");
    /** Marqueur d'entite : "pot", "plant" ou "rack". */
    public static final NamespacedKey MARKER = key("marker");
    /** Position du bloc porteur, encodee "monde;x;y;z". */
    public static final NamespacedKey POS = key("pos");

    private ItemKeys() {
    }

    private static NamespacedKey key(String name) {
        return new NamespacedKey(NAMESPACE, name);
    }

    public static Optional<HerbalisItemType> typeOf(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return Optional.empty();
        }
        PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
        String id = pdc.get(TYPE, PersistentDataType.STRING);
        return id == null ? Optional.empty() : HerbalisItemType.byId(id);
    }

    public static Optional<String> drugOf(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return Optional.empty();
        }
        return Optional.ofNullable(stack.getItemMeta()
                .getPersistentDataContainer().get(DRUG, PersistentDataType.STRING));
    }

    public static Optional<Quality> qualityOf(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return Optional.empty();
        }
        Integer stars = stack.getItemMeta()
                .getPersistentDataContainer().get(QUALITY, PersistentDataType.INTEGER);
        return stars == null ? Optional.empty() : Optional.of(Quality.of(stars));
    }

    public static boolean isHerbalisItem(ItemStack stack) {
        return typeOf(stack).isPresent();
    }
}
