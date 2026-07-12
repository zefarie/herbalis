package io.github.zefarie.herbalis.domain.consumption;

import io.github.zefarie.herbalis.domain.TestFixtures;
import io.github.zefarie.herbalis.domain.drug.DrugType;
import io.github.zefarie.herbalis.domain.quality.Quality;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsumptionEngineTest {

    private final DrugType weed = TestFixtures.weed();

    @Test
    void laToleranceMonteAChaqueConsommation() {
        ConsumerProfile profile = ConsumerProfile.fresh(0L);
        var outcome = ConsumptionEngine.consume(profile, 0L, Quality.of(3), weed);
        assertEquals(12.0, outcome.profile().tolerance(), 0.01);
        assertEquals(8.0, outcome.profile().addiction(), 0.01);
    }

    @Test
    void laToleranceDecroitAvecLeTempsMemeHorsLigne() {
        ConsumerProfile profile = ConsumptionEngine
                .consume(ConsumerProfile.fresh(0L), 0L, Quality.of(3), weed)
                .profile();
        // 2 heures plus tard : 12 - 2 * 4 = 4.
        ConsumerProfile decayed = ConsumptionEngine.decayed(
                profile, 2 * 3_600_000L, weed.consumption());
        assertEquals(4.0, decayed.tolerance(), 0.01);
        // 10 heures : plancher a zero.
        assertEquals(0.0, ConsumptionEngine.decayed(
                profile, 10 * 3_600_000L, weed.consumption()).tolerance(), 0.01);
    }

    @Test
    void laToleranceReduitLaDureeDuHigh() {
        ConsumerProfile fresh = ConsumerProfile.fresh(0L);
        var first = ConsumptionEngine.consume(fresh, 0L, Quality.of(5), weed);

        ConsumerProfile tolerant = new ConsumerProfile(100.0, 0.0, 0L, 0L,
                java.util.List.of());
        var second = ConsumptionEngine.consume(tolerant, 0L, Quality.of(5), weed);

        assertTrue(second.timeline().highMillis() < first.timeline().highMillis());
        assertEquals(0.4, second.timeline().intensity(), 0.01);
    }

    @Test
    void troisJointsEnCinqMinutesDeclenchentLeBlackout() {
        long now = 0L;
        ConsumerProfile profile = ConsumerProfile.fresh(now);
        var first = ConsumptionEngine.consume(profile, now, Quality.of(2), weed);
        assertFalse(first.blackout());
        var second = ConsumptionEngine.consume(first.profile(),
                now + 60_000, Quality.of(2), weed);
        assertFalse(second.blackout());
        var third = ConsumptionEngine.consume(second.profile(),
                now + 120_000, Quality.of(2), weed);
        assertTrue(third.blackout());
    }

    @Test
    void lesConsommationsEspaceesNeDeclenchentPasLeBlackout() {
        long now = 0L;
        ConsumerProfile profile = ConsumerProfile.fresh(now);
        var first = ConsumptionEngine.consume(profile, now, Quality.of(2), weed);
        var second = ConsumptionEngine.consume(first.profile(),
                now + 6 * 60_000, Quality.of(2), weed);
        var third = ConsumptionEngine.consume(second.profile(),
                now + 12 * 60_000, Quality.of(2), weed);
        assertFalse(third.blackout());
    }

    @Test
    void leManqueFrappeLesAddictsApresLeDelai() {
        // Profil addict : au-dessus du seuil de 50, derniere conso a t0.
        long t0 = 1_000L;
        ConsumerProfile addicted = new ConsumerProfile(0.0, 80.0, t0, t0,
                java.util.List.of(t0));
        // 10 minutes apres : pas encore de manque (delai 45 min).
        assertEquals(WithdrawalState.NONE, ConsumptionEngine.withdrawal(
                addicted, t0 + 10 * 60_000L, weed.consumption()));
        // 1 heure apres : manque (addiction 80 - 1.5 = 78.5, toujours addict).
        assertEquals(WithdrawalState.WITHDRAWAL, ConsumptionEngine.withdrawal(
                addicted, t0 + 3_600_000L, weed.consumption()));
    }

    @Test
    void leManqueDisparaitQuandLAddictionRetombe() {
        long t0 = 1_000L;
        ConsumerProfile addicted = new ConsumerProfile(0.0, 55.0, t0, t0,
                java.util.List.of(t0));
        // Apres 10 heures d'abstinence : 55 - 15 = 40, sous le seuil de 50.
        assertEquals(WithdrawalState.NONE, ConsumptionEngine.withdrawal(
                addicted, t0 + 10 * 3_600_000L, weed.consumption()));
    }

    @Test
    void lesJoueursSansHistoriqueNeSontJamaisEnManque() {
        assertEquals(WithdrawalState.NONE, ConsumptionEngine.withdrawal(
                ConsumerProfile.fresh(0L), 3_600_000L, weed.consumption()));
    }
}
