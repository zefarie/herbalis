package io.github.zefarie.herbalis.infrastructure.item;

import java.util.Arrays;
import java.util.Optional;

/**
 * Types d'items custom du plugin. Les types marques {@code drugScoped}
 * portent en plus l'identifiant de la drogue (et parfois une qualite)
 * dans leur PDC.
 */
public enum HerbalisItemType {
    POT("pot", false),
    DRYING_RACK("drying_rack", false),
    WATERING_CAN("watering_can", false),
    FERTILIZER("fertilizer", false),
    ROLLING_PAPER("rolling_paper", false),
    POUCH_EMPTY("pouch_empty", false),
    SEED("seed", true),
    BUD_FRESH("bud_fresh", true),
    DRIED("dried", true),
    POUCH("pouch", true),
    JOINT("joint", true);

    private final String id;
    private final boolean drugScoped;

    HerbalisItemType(String id, boolean drugScoped) {
        this.id = id;
        this.drugScoped = drugScoped;
    }

    public String id() {
        return id;
    }

    public boolean isDrugScoped() {
        return drugScoped;
    }

    public static Optional<HerbalisItemType> byId(String id) {
        return Arrays.stream(values())
                .filter(type -> type.id.equals(id))
                .findFirst();
    }
}
