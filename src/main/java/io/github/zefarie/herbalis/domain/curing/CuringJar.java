package io.github.zefarie.herbalis.domain.curing;

import io.github.zefarie.herbalis.domain.drug.CuringProfile;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.quality.Quality;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Jarre de curing immuable. La weed sechee s'y affine en temps reel
 * (+1 etoile a terme); oubliee trop longtemps, la jarre moisit et son
 * contenu est ruine. La moisissure d'une seule tete gagne toute la jarre.
 *
 * @param id     identifiant stable de la jarre
 * @param pos    position de la jarre dans le monde
 * @param drugId type de drogue en cours d'affinage (vide = jarre vide)
 * @param slots  tetes en cours d'affinage
 */
public record CuringJar(
        UUID id,
        BlockPos pos,
        String drugId,
        List<CuringSlot> slots
) {

    public CuringJar {
        slots = List.copyOf(slots);
    }

    public static CuringJar empty(BlockPos pos) {
        return new CuringJar(UUID.randomUUID(), pos, "", List.of());
    }

    public boolean isEmpty() {
        return slots.isEmpty();
    }

    public boolean canAccept(String forDrugId, int capacity) {
        if (slots.size() >= capacity) {
            return false;
        }
        return isEmpty() || drugId.equals(forDrugId);
    }

    /** Ajoute une tete sechee, demarre son affinage maintenant. */
    public CuringJar withBud(String forDrugId, Quality quality, long now) {
        List<CuringSlot> next = new ArrayList<>(slots);
        next.add(new CuringSlot(quality, now));
        return new CuringJar(id, pos, forDrugId, next);
    }

    /** Vide la jarre apres recuperation. */
    public CuringJar emptied() {
        return new CuringJar(id, pos, "", List.of());
    }

    /** Vrai si toutes les tetes sont affinees. */
    public boolean isReady(long now, CuringProfile profile) {
        return !isEmpty()
                && slots.stream().allMatch(s -> s.isCured(now, profile.duration()));
    }

    /** Vrai si au moins une tete a moisi : toute la jarre est perdue. */
    public boolean isMoldy(long now, CuringProfile profile) {
        return slots.stream().anyMatch(
                s -> s.isMoldy(now, profile.duration(), profile.moldDelay()));
    }

    public JarVisualState visualState(long now, CuringProfile profile) {
        if (isEmpty()) {
            return JarVisualState.EMPTY;
        }
        if (isMoldy(now, profile)) {
            return JarVisualState.MOLDY;
        }
        return isReady(now, profile) ? JarVisualState.READY : JarVisualState.CURING;
    }

    /** Fraction d'affinage du lot le moins avance, 0 a 1. */
    public double overallProgress(long now, CuringProfile profile) {
        return slots.stream()
                .mapToDouble(s -> Math.min(1.0, s.completionRatio(now, profile.duration())))
                .min()
                .orElse(0.0);
    }
}
