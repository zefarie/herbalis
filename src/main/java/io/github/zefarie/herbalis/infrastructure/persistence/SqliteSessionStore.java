package io.github.zefarie.herbalis.infrastructure.persistence;

import io.github.zefarie.herbalis.domain.consumption.EffectTimeline;
import io.github.zefarie.herbalis.domain.quality.Quality;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persistance des sessions d'effets en cours, pour reprendre proprement
 * apres une deconnexion ou un redemarrage (le temps hors ligne compte,
 * la session reprend la ou les timestamps la placent).
 */
public final class SqliteSessionStore {

    /**
     * Session persistee.
     *
     * @param drugId    drogue consommee
     * @param timeline  chronologie des effets
     * @param startedAt debut de la session (epoch ms)
     */
    public record StoredSession(String drugId, EffectTimeline timeline, long startedAt) {
    }

    private final Database database;

    public SqliteSessionStore(Database database) {
        this.database = database;
    }

    /** Charge toutes les sessions (au demarrage). */
    public Map<UUID, StoredSession> loadAll() {
        Map<UUID, StoredSession> sessions = new HashMap<>();
        database.sync(connection -> {
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT * FROM sessions")) {
                while (rs.next()) {
                    EffectTimeline timeline = new EffectTimeline(
                            rs.getLong("rise_ms"),
                            rs.getLong("high_ms"),
                            rs.getLong("comedown_ms"),
                            rs.getDouble("intensity"),
                            Quality.of(rs.getInt("quality")));
                    sessions.put(UUID.fromString(rs.getString("uuid")),
                            new StoredSession(rs.getString("drug_id"), timeline,
                                    rs.getLong("started_at")));
                }
            }
        });
        return sessions;
    }

    public void save(UUID playerId, StoredSession session) {
        database.async(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO sessions (uuid, drug_id, quality, started_at,
                                          rise_ms, high_ms, comedown_ms, intensity)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(uuid) DO UPDATE SET
                        drug_id = excluded.drug_id,
                        quality = excluded.quality,
                        started_at = excluded.started_at,
                        rise_ms = excluded.rise_ms,
                        high_ms = excluded.high_ms,
                        comedown_ms = excluded.comedown_ms,
                        intensity = excluded.intensity
                    """)) {
                statement.setString(1, playerId.toString());
                statement.setString(2, session.drugId());
                statement.setInt(3, session.timeline().quality().stars());
                statement.setLong(4, session.startedAt());
                statement.setLong(5, session.timeline().riseMillis());
                statement.setLong(6, session.timeline().highMillis());
                statement.setLong(7, session.timeline().comedownMillis());
                statement.setDouble(8, session.timeline().intensity());
                statement.executeUpdate();
            }
        });
    }

    public void delete(UUID playerId) {
        database.async(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM sessions WHERE uuid = ?")) {
                statement.setString(1, playerId.toString());
                statement.executeUpdate();
            }
        });
    }
}
