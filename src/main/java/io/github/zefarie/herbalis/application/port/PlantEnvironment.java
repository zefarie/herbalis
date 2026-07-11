package io.github.zefarie.herbalis.application.port;

import io.github.zefarie.herbalis.domain.geo.BlockPos;

/**
 * Photo de l'environnement physique des plantes, fournie par
 * l'infrastructure (monde Bukkit).
 */
public interface PlantEnvironment {

    /** Vrai si le chunk de la position est charge et simulable. */
    boolean isLoaded(BlockPos pos);

    /** Niveau de lumiere au bloc donne, 0 a 15. */
    int lightLevel(BlockPos pos);
}
