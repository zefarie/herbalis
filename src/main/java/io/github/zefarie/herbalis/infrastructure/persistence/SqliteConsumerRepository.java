package io.github.zefarie.herbalis.infrastructure.persistence;

import io.github.zefarie.herbalis.application.port.ConsumerRepository;
import io.github.zefarie.herbalis.domain.consumption.ConsumerProfile;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Profils de consommation des joueurs. Tout est charge au demarrage
 * (la decroissance hors ligne se calcule via les timestamps).
 */
public final class SqliteConsumerRepository implements ConsumerRepository {

    private final Database database;
    private final Map<UUID, ConsumerProfile> profiles = new ConcurrentHashMap<>();

    public SqliteConsumerRepository(Database database) {
        this.database = database;
        loadAll();
    }

    private void loadAll() {
        database.sync(connection -> {
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT * FROM players")) {
                while (rs.next()) {
                    List<Long> recent = parseTimestamps(rs.getString("recent_consumptions"));
                    profiles.put(UUID.fromString(rs.getString("uuid")),
                            new ConsumerProfile(
                                    rs.getDouble("tolerance"),
                                    rs.getDouble("addiction"),
                                    rs.getLong("last_updated_at"),
                                    rs.getLong("last_consumed_at"),
                                    recent));
                }
            }
        });
    }

    @Override
    public ConsumerProfile of(UUID playerId, long now) {
        return profiles.computeIfAbsent(playerId, id -> ConsumerProfile.fresh(now));
    }

    @Override
    public void put(UUID playerId, ConsumerProfile profile) {
        profiles.put(playerId, profile);
        database.async(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO players (uuid, tolerance, addiction, last_updated_at,
                                         last_consumed_at, recent_consumptions)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT(uuid) DO UPDATE SET
                        tolerance = excluded.tolerance,
                        addiction = excluded.addiction,
                        last_updated_at = excluded.last_updated_at,
                        last_consumed_at = excluded.last_consumed_at,
                        recent_consumptions = excluded.recent_consumptions
                    """)) {
                statement.setString(1, playerId.toString());
                statement.setDouble(2, profile.tolerance());
                statement.setDouble(3, profile.addiction());
                statement.setLong(4, profile.lastUpdatedAt());
                statement.setLong(5, profile.lastConsumedAt());
                statement.setString(6, profile.recentConsumptions().stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")));
                statement.executeUpdate();
            }
        });
    }

    private static List<Long> parseTimestamps(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<Long> result = new ArrayList<>();
        Arrays.stream(raw.split(",")).forEach(part -> {
            try {
                result.add(Long.parseLong(part.trim()));
            } catch (NumberFormatException ignored) {
                // Entree corrompue : on l'ignore.
            }
        });
        return result;
    }
}
