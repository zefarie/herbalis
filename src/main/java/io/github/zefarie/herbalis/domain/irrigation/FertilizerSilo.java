package io.github.zefarie.herbalis.domain.irrigation;

import io.github.zefarie.herbalis.domain.geo.BlockPos;

import java.util.UUID;

/**
 * Silo d'engrais immuable. Charge en doses, il fertilise automatiquement
 * les pots relies par des tuyaux des qu'une plante y a droit.
 *
 * @param id    identifiant stable du silo
 * @param pos   position du silo dans le monde
 * @param doses doses d'engrais restantes
 */
public record FertilizerSilo(
        UUID id,
        BlockPos pos,
        int doses
) {

    public static FertilizerSilo empty(BlockPos pos) {
        return new FertilizerSilo(UUID.randomUUID(), pos, 0);
    }

    public boolean isEmpty() {
        return doses <= 0;
    }

    public FertilizerSilo withDoses(int doses) {
        return new FertilizerSilo(id, pos, Math.max(0, doses));
    }

    /** Ajoute une dose sans depasser la capacite. */
    public FertilizerSilo filled(int capacity) {
        return withDoses(Math.min(capacity, doses + 1));
    }

    /** Consomme une dose. */
    public FertilizerSilo consumed() {
        return withDoses(doses - 1);
    }
}
