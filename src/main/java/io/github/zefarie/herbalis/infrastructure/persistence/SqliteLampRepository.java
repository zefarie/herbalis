package io.github.zefarie.herbalis.infrastructure.persistence;

import io.github.zefarie.herbalis.application.port.LampRepository;
import io.github.zefarie.herbalis.domain.geo.BlockPos;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lampes horticoles : memoire vive comme source de verite, SQLite en
 * ecriture directe (poser ou casser une lampe est un evenement rare).
 */
public final class SqliteLampRepository implements LampRepository {

    private final Database database;
    private final Set<BlockPos> lamps = ConcurrentHashMap.newKeySet();

    public SqliteLampRepository(Database database) {
        this.database = database;
        loadAll();
    }

    private void loadAll() {
        database.sync(connection -> {
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery(
                         "SELECT world, x, y, z FROM lamps")) {
                while (rs.next()) {
                    lamps.add(new BlockPos(UUID.fromString(rs.getString(1)),
                            rs.getInt(2), rs.getInt(3), rs.getInt(4)));
                }
            }
        });
    }

    @Override
    public boolean has(BlockPos pos) {
        return lamps.contains(pos);
    }

    @Override
    public void add(BlockPos pos) {
        if (!lamps.add(pos)) {
            return;
        }
        database.async(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT OR IGNORE INTO lamps (world, x, y, z) VALUES (?, ?, ?, ?)")) {
                bind(statement, pos);
                statement.executeUpdate();
            }
        });
    }

    @Override
    public void remove(BlockPos pos) {
        if (!lamps.remove(pos)) {
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
    public Collection<BlockPos> all() {
        return List.copyOf(lamps);
    }

    private static void bind(PreparedStatement statement, BlockPos pos)
            throws java.sql.SQLException {
        statement.setString(1, pos.worldId().toString());
        statement.setInt(2, pos.x());
        statement.setInt(3, pos.y());
        statement.setInt(4, pos.z());
    }
}
