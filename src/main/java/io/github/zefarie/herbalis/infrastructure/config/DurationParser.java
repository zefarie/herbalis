package io.github.zefarie.herbalis.infrastructure.config;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parse les durees de la config : "30s", "8m", "2h", "1h30m", "2d",
 * "1j12h" (j et d valent tous deux un jour), "90" (secondes).
 */
public final class DurationParser {

    private static final Pattern PART = Pattern.compile("(\\d+)([djhms])");

    private DurationParser() {
    }

    public static Duration parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Duree vide");
        }
        String value = raw.trim().toLowerCase().replace(" ", "");
        if (value.matches("\\d+")) {
            return Duration.ofSeconds(Long.parseLong(value));
        }
        Matcher matcher = PART.matcher(value);
        long seconds = 0;
        int consumed = 0;
        while (matcher.find()) {
            if (matcher.start() != consumed) {
                throw new IllegalArgumentException("Duree invalide : " + raw);
            }
            consumed = matcher.end();
            long amount = Long.parseLong(matcher.group(1));
            seconds += switch (matcher.group(2)) {
                case "d", "j" -> amount * 86_400;
                case "h" -> amount * 3600;
                case "m" -> amount * 60;
                default -> amount;
            };
        }
        if (consumed != value.length()) {
            throw new IllegalArgumentException("Duree invalide : " + raw);
        }
        return Duration.ofSeconds(seconds);
    }
}
