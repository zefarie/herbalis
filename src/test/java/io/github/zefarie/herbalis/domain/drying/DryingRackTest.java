package io.github.zefarie.herbalis.domain.drying;

import io.github.zefarie.herbalis.domain.TestFixtures;
import io.github.zefarie.herbalis.domain.quality.Quality;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DryingRackTest {

    private static final Duration DRYING = Duration.ofMinutes(20);

    @Test
    void unRackVideEstVide() {
        DryingRack rack = DryingRack.empty(TestFixtures.pos());
        assertTrue(rack.isEmpty());
        assertEquals(RackVisualState.EMPTY, rack.visualState(0L, DRYING));
    }

    @Test
    void respecteLaCapacite() {
        DryingRack rack = DryingRack.empty(TestFixtures.pos());
        for (int i = 0; i < 6; i++) {
            assertTrue(rack.canAccept("weed", 6));
            rack = rack.withBud("weed", Quality.of(3), 0L);
        }
        assertFalse(rack.canAccept("weed", 6));
    }

    @Test
    void refuseLeMelangeDeDrogues() {
        DryingRack rack = DryingRack.empty(TestFixtures.pos())
                .withBud("weed", Quality.of(3), 0L);
        assertFalse(rack.canAccept("autre", 6));
        assertTrue(rack.canAccept("weed", 6));
    }

    @Test
    void pretQuandToutesLesTetesSontSeches() {
        long start = 0L;
        DryingRack rack = DryingRack.empty(TestFixtures.pos())
                .withBud("weed", Quality.of(3), start)
                .withBud("weed", Quality.of(4), start + 10 * 60_000);

        long firstDry = start + DRYING.toMillis();
        assertEquals(RackVisualState.DRYING, rack.visualState(firstDry, DRYING));

        long allDry = start + 10 * 60_000 + DRYING.toMillis();
        assertEquals(RackVisualState.READY, rack.visualState(allDry, DRYING));
        assertTrue(rack.isReady(allDry, DRYING));
    }

    @Test
    void leSechageSurvitAUnRedemarrageViaLesTimestamps() {
        // Une tete posee a t0 : meme si le serveur redemarre, a t0 + duree
        // elle est seche, seul le timestamp compte.
        DryingSlot slot = new DryingSlot(Quality.of(3), 1_000_000L);
        assertFalse(slot.isDry(1_000_000L + DRYING.toMillis() - 1, DRYING));
        assertTrue(slot.isDry(1_000_000L + DRYING.toMillis(), DRYING));
        assertEquals(0.5, slot.completionRatio(
                1_000_000L + DRYING.toMillis() / 2, DRYING), 0.001);
    }

    @Test
    void laProgressionGlobaleSuitLaTeteLaMoinsAvancee() {
        DryingRack rack = DryingRack.empty(TestFixtures.pos())
                .withBud("weed", Quality.of(3), 0L)
                .withBud("weed", Quality.of(3), 10 * 60_000L);
        // A t = 20 min : premiere tete seche (1.0), seconde a 0.5.
        assertEquals(0.5, rack.overallProgress(20 * 60_000L, DRYING), 0.001);
    }
}
