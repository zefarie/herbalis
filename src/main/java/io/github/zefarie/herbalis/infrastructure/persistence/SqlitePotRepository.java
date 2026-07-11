package io.github.zefarie.herbalis.infrastructure.persistence;

import io.github.zefarie.herbalis.application.port.PotRepository;
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
 * Pots : memoire vive comme source de verite, SQLite en ecriture directe
 * (poser ou casser un pot est un evenement rare).
 */
public final class SqlitePotRepository implements PotRepository {

    private final Database database;
    private final Set<BlockPos> pots = ConcurrentHashMap.newKeySet();

    public SqlitePotRepository(Database database) {
        this.database = database;
        loadAll();
    }

    private void loadAll() {
        database.sync(connection -> {
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT world, x, y, z FROM pots")) {
                while (rs.next()) {
                    pots.add(new BlockPos(UUID.fromString(rs.getString(1)),
                            rs.getInt(2), rs.getInt(3), rs.getInt(4)));
                }
            }
        });
    }

    @Override
    public boolean exists(BlockPos pos) {
        return pots.contains(pos);
    }

    @Override
    public void add(BlockPos pos) {
        if (!pots.add(pos)) {
            return;
        }
        database.async(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT OR IGNORE INTO pots (world, x, y, z) VALUES (?, ?, ?, ?)")) {
                bind(statement, pos);
                statement.executeUpdate();
            }
        });
    }

    @Override
    public void remove(BlockPos pos) {
        if (!pots.remove(pos)) {
            return;
        }
        database.async(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM pots WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
                bind(statement, pos);
                statement.executeUpdate();
            }
        });
    }

    @Override
    public Collection<BlockPos> all() {
        return List.copyOf(pots);
    }

    private static void bind(PreparedStatement statement, BlockPos pos)
            throws java.sql.SQLException {
        statement.setString(1, pos.worldId().toString());
        statement.setInt(2, pos.x());
        statement.setInt(3, pos.y());
        statement.setInt(4, pos.z());
    }
}
