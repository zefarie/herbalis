package io.github.zefarie.herbalis.domain.irrigation;

import io.github.zefarie.herbalis.domain.geo.BlockPos;

import java.util.UUID;

/**
 * Caisson d'eau immuable. Rempli au seau, il abreuve les pots relies
 * par des tuyaux tant qu'il a du stock.
 *
 * @param id    identifiant stable du caisson
 * @param pos   position du caisson dans le monde
 * @param size  taille (cuve, citerne, reservoir)
 * @param stock eau restante, en points d'hydratation
 */
public record WaterTank(
        UUID id,
        BlockPos pos,
        TankSize size,
        double stock
) {

    public static WaterTank empty(BlockPos pos, TankSize size) {
        return new WaterTank(UUID.randomUUID(), pos, size, 0.0);
    }

    public boolean isEmpty() {
        return stock <= 0.0;
    }

    public WaterTank withStock(double stock) {
        return new WaterTank(id, pos, size, Math.max(0.0, stock));
    }

    /** Ajoute de l'eau sans depasser la capacite. */
    public WaterTank filled(double amount, double capacity) {
        return withStock(Math.min(capacity, stock + amount));
    }

    /** Preleve de l'eau, sans passer sous zero. */
    public WaterTank drained(double amount) {
        return withStock(stock - amount);
    }

    /** Fraction de remplissage, 0 a 1. */
    public double fillRatio(double capacity) {
        if (capacity <= 0.0) {
            return 0.0;
        }
        return Math.clamp(stock / capacity, 0.0, 1.0);
    }
}
