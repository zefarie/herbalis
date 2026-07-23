package io.github.zefarie.herbalis.infrastructure.persistence;

import io.github.zefarie.herbalis.application.port.LampRepository;
import io.github.zefarie.herbalis.domain.geo.BlockPos;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lampes horticoles : memoire vive comme source de verite, SQLite en
 * ecriture directe (poser, casser ou basculer une lampe est un
 * evenement rare). La valeur associee est l'etat allume/eteint.
 */
public final class SqliteLampRepository implements LampRepository {

    private final Database database;
    private final Map<BlockPos, Boolean> lamps = new ConcurrentHashMap<>();

    public SqliteLampRepository(Database database) {
        this.database = database;
        loadAll();
    }

    private void loadAll() {
        database.sync(connection -> {
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery(
                         "SELECT world, x, y, z, enabled FROM lamps")) {
                while (rs.next()) {
                    lamps.put(new BlockPos(UUID.fromString(rs.getString(1)),
                                    rs.getInt(2), rs.getInt(3), rs.getInt(4)),
                            rs.getInt(5) != 0);
                }
            }
        });
    }

    @Override
    public boolean has(BlockPos pos) {
        return lamps.containsKey(pos);
    }

    @Override
    public void add(BlockPos pos) {
        if (lamps.putIfAbsent(pos, true) != null) {
            return;
        }
        database.async(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT OR IGNORE INTO lamps (world, x, y, z, enabled) "
                            + "VALUES (?, ?, ?, ?, 1)")) {
                bind(statement, pos);
                statement.executeUpdate();
            }
        });
    }

    @Override
    public void remove(BlockPos pos) {
        if (lamps.remove(pos) == null) {
            return;
        }
        database.async(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM lamps WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
                bind(statement, pos);
                statement.executeUpdate();
            }
        });
    }

    @Override
    public boolean isEnabled(BlockPos pos) {
        return lamps.getOrDefault(pos, true);
    }

    @Override
    public void setEnabled(BlockPos pos, boolean enabled) {
        if (lamps.replace(pos, enabled) == null) {
            return;
        }
        database.async(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE lamps SET enabled = ? "
                            + "WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
                statement.setInt(1, enabled ? 1 : 0);
                statement.setString(2, pos.worldId().toString());
                statement.setInt(3, pos.x());
                statement.setInt(4, pos.y());
                statement.setInt(5, pos.z());
                statement.executeUpdate();
            }
        });
    }

    @Override
    public Collection<BlockPos> all() {
        return List.copyOf(lamps.keySet());
    }

    private static void bind(PreparedStatement statement, BlockPos pos)
            throws java.sql.SQLException {
        statement.setString(1, pos.worldId().toString());
        statement.setInt(2, pos.x());
        statement.setInt(3, pos.y());
        statement.setInt(4, pos.z());
    }
}
