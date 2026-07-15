package io.github.zefarie.herbalis.infrastructure.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DurationParserTest {

    @Test
    void parseLesUnitesSimples() {
        assertEquals(Duration.ofSeconds(30), DurationParser.parse("30s"));
        assertEquals(Duration.ofMinutes(8), DurationParser.parse("8m"));
        assertEquals(Duration.ofHours(2), DurationParser.parse("2h"));
        assertEquals(Duration.ofSeconds(90), DurationParser.parse("90"));
    }

    @Test
    void parseLesJoursEnFrancaisEtEnAnglais() {
        assertEquals(Duration.ofDays(2), DurationParser.parse("2d"));
        assertEquals(Duration.ofDays(2), DurationParser.parse("2j"));
        assertEquals(Duration.ofHours(36), DurationParser.parse("1d12h"));
        assertEquals(Duration.ofHours(36), DurationParser.parse("1j12h"));
    }

    @Test
    void parseLesCombinaisons() {
        assertEquals(Duration.ofMinutes(90), DurationParser.parse("1h30m"));
        assertEquals(Duration.ofSeconds(86_400 + 3600 + 60 + 1),
                DurationParser.parse("1d1h1m1s"));
    }

    @Test
    void rejetteLesFormatsInvalides() {
        assertThrows(IllegalArgumentException.class,
                () -> DurationParser.parse("abc"));
        assertThrows(IllegalArgumentException.class,
                () -> DurationParser.parse("1w"));
        assertThrows(IllegalArgumentException.class,
                () -> DurationParser.parse(""));
    }
}
