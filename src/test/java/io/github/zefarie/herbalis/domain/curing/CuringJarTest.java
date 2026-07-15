package io.github.zefarie.herbalis.domain.curing;

import io.github.zefarie.herbalis.domain.TestFixtures;
import io.github.zefarie.herbalis.domain.drug.CuringProfile;
import io.github.zefarie.herbalis.domain.quality.Quality;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CuringJarTest {

    private final CuringProfile profile = TestFixtures.weed().curing();

    private static long minutes(long value) {
        return value * 60_000L;
    }

    @Test
    void jarreVidePuisEnCoursPuisPrete() {
        CuringJar jar = CuringJar.empty(TestFixtures.pos());
        assertEquals(JarVisualState.EMPTY, jar.visualState(0L, profile));

        jar = jar.withBud("weed", Quality.of(3), 0L);
        assertEquals(JarVisualState.CURING, jar.visualState(minutes(30), profile));
        assertEquals(JarVisualState.READY, jar.visualState(minutes(45), profile));
        assertTrue(jar.isReady(minutes(45), profile));
    }

    @Test
    void laJarreOublieeMoisit() {
        CuringJar jar = CuringJar.empty(TestFixtures.pos())
                .withBud("weed", Quality.of(4), 0L);
        // 45m d'affinage + 90m de delai : encore bonne juste avant.
        assertFalse(jar.isMoldy(minutes(134), profile));
        assertTrue(jar.isMoldy(minutes(135), profile));
        assertEquals(JarVisualState.MOLDY, jar.visualState(minutes(140), profile));
    }

    @Test
    void uneSeuleTeteMoisieContamineLaJarre() {
        CuringJar jar = CuringJar.empty(TestFixtures.pos())
                .withBud("weed", Quality.of(4), 0L)
                .withBud("weed", Quality.of(4), minutes(120));
        // La premiere tete a moisi, la seconde est fraiche : jarre perdue.
        assertTrue(jar.isMoldy(minutes(140), profile));
    }

    @Test
    void laProgressionSuitLeLotLeMoinsAvance() {
        CuringJar jar = CuringJar.empty(TestFixtures.pos())
                .withBud("weed", Quality.of(3), 0L)
                .withBud("weed", Quality.of(3), minutes(22));
        assertEquals(0.5, jar.overallProgress(minutes(22) + minutes(45) / 2,
                profile), 0.02);
    }

    @Test
    void capaciteEtMelangeRespectes() {
        CuringJar jar = CuringJar.empty(TestFixtures.pos());
        for (int i = 0; i < profile.capacity(); i++) {
            assertTrue(jar.canAccept("weed", profile.capacity()));
            jar = jar.withBud("weed", Quality.of(3), 0L);
        }
        assertFalse(jar.canAccept("weed", profile.capacity()));
        CuringJar started = CuringJar.empty(TestFixtures.pos())
                .withBud("weed", Quality.of(3), 0L);
        assertFalse(started.canAccept("autre", profile.capacity()));
    }
}
