package io.github.zefarie.herbalis.domain.drying;

import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.quality.Quality;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Rack de sechage immuable. Les tetes fraiches y sechent en temps reel;
 * les retirer avant la fin coute de la qualite.
 *
 * @param id    identifiant stable du rack
 * @param pos   position du rack dans le monde
 * @param drugId type de drogue en cours de sechage (vide = rack vide)
 * @param slots  tetes en cours de sechage
 */
public record DryingRack(
        UUID id,
        BlockPos pos,
        String drugId,
        List<DryingSlot> slots
) {

    public DryingRack {
        slots = List.copyOf(slots);
    }

    public static DryingRack empty(BlockPos pos) {
        return new DryingRack(UUID.randomUUID(), pos, "", List.of());
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

    /** Ajoute une tete, demarre son sechage maintenant. */
    public DryingRack withBud(String forDrugId, Quality quality, long now) {
        List<DryingSlot> next = new ArrayList<>(slots);
        next.add(new DryingSlot(quality, now));
        return new DryingRack(id, pos, forDrugId, next);
    }

    /** Vide le rack apres recuperation. */
    public DryingRack emptied() {
        return new DryingRack(id, pos, "", List.of());
    }

    /** Avance le sechage de {@code millis} (outillage de test admin). */
    public DryingRack shiftedBy(long millis) {
        List<DryingSlot> shifted = slots.stream()
                .map(s -> new DryingSlot(s.quality(), s.startedAt() - millis))
                .toList();
        return new DryingRack(id, pos, drugId, shifted);
    }

    /** Vrai si toutes les tetes sont seches. */
    public boolean isReady(long now, Duration dryingDuration) {
        return !isEmpty() && slots.stream().allMatch(s -> s.isDry(now, dryingDuration));
    }

    public RackVisualState visualState(long now, Duration dryingDuration) {
        if (isEmpty()) {
            return RackVisualState.EMPTY;
        }
        return isReady(now, dryingDuration) ? RackVisualState.READY : RackVisualState.DRYING;
    }

    /** Fraction de sechage du lot le moins avance, 0 a 1. */
    public double overallProgress(long now, Duration dryingDuration) {
        return slots.stream()
                .mapToDouble(s -> Math.min(1.0, s.completionRatio(now, dryingDuration)))
                .min()
                .orElse(0.0);
    }
}
