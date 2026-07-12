package io.github.zefarie.herbalis.domain.consumption;

import io.github.zefarie.herbalis.domain.TestFixtures;
import io.github.zefarie.herbalis.domain.drug.DrugType;
import io.github.zefarie.herbalis.domain.quality.Quality;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectTimelineTest {

    private final DrugType weed = TestFixtures.weed();

    @Test
    void laDureeDuHighSuitLaQualite() {
        EffectTimeline one = EffectTimeline.create(weed.effects(),
                weed.consumption(), Quality.of(1), 0.0);
        EffectTimeline five = EffectTimeline.create(weed.effects(),
                weed.consumption(), Quality.of(5), 0.0);
        assertEquals(2 * 60_000, one.highMillis());
        assertEquals(6 * 60_000, five.highMillis());
    }

    @Test
    void laDescenteEstProportionnelleAuHigh() {
        EffectTimeline timeline = EffectTimeline.create(weed.effects(),
                weed.consumption(), Quality.of(5), 0.0);
        assertEquals(Math.round(timeline.highMillis() * 0.35),
                timeline.comedownMillis());
    }

    @Test
    void lesPhasesSeSuccedentDansLOrdre() {
        EffectTimeline timeline = EffectTimeline.create(weed.effects(),
                weed.consumption(), Quality.of(3), 0.0);
        assertEquals(EffectPhase.RISE, timeline.phaseAt(0));
        assertEquals(EffectPhase.RISE, timeline.phaseAt(timeline.riseMillis() - 1));
        assertEquals(EffectPhase.HIGH, timeline.phaseAt(timeline.riseMillis()));
        assertEquals(EffectPhase.COMEDOWN, timeline.phaseAt(
                timeline.riseMillis() + timeline.highMillis()));
        assertEquals(EffectPhase.DONE, timeline.phaseAt(timeline.totalMillis()));
    }

    @Test
    void laProgressionDeMonteeEstBornee() {
        EffectTimeline timeline = EffectTimeline.create(weed.effects(),
                weed.consumption(), Quality.of(3), 0.0);
        assertEquals(0.0, timeline.riseProgress(0), 0.001);
        assertEquals(0.5, timeline.riseProgress(timeline.riseMillis() / 2), 0.001);
        assertEquals(1.0, timeline.riseProgress(timeline.riseMillis() * 2), 0.001);
    }

    @Test
    void uneToleranceMaximaleReduitSansAnnulerLesEffets() {
        EffectTimeline timeline = EffectTimeline.create(weed.effects(),
                weed.consumption(), Quality.of(3), 100.0);
        assertTrue(timeline.highMillis() > 0);
        assertEquals(0.4, timeline.intensity(), 0.001);
    }
}
