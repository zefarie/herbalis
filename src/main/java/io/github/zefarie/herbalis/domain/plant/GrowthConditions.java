package io.github.zefarie.herbalis.domain.plant;

/**
 * Photo de l'environnement d'une plante au moment d'un tick.
 *
 * @param lightLevel  niveau de lumiere au bloc de la plante (0 a 15)
 * @param deltaMillis temps ecoule depuis le tick precedent
 */
public record GrowthConditions(int lightLevel, long deltaMillis) {
}
