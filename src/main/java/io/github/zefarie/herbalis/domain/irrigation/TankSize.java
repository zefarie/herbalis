package io.github.zefarie.herbalis.domain.irrigation;

import java.util.Arrays;
import java.util.Optional;

/**
 * Tailles de caisson d'eau, de l'artisanal a l'industriel. La capacite
 * de chaque taille vient de la configuration.
 */
public enum TankSize {
    CUVE("cuve"),
    CITERNE("citerne"),
    RESERVOIR("reservoir");

    private final String id;

    TankSize(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Optional<TankSize> byId(String id) {
        return Arrays.stream(values())
                .filter(size -> size.id.equals(id))
                .findFirst();
    }
}
