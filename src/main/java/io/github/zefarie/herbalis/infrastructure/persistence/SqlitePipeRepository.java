package io.github.zefarie.herbalis.infrastructure.persistence;

import io.github.zefarie.herbalis.application.port.PipeRepository;
import io.github.zefarie.herbalis.domain.geo.BlockPos;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tuyaux : memoire vive comme source de verite, SQLite en ecriture
 * directe (poser ou casser un tuyau est un evenement rare).
 */
public final class SqlitePipeRepository implements PipeRepository {

    private final Database database;
    private final Set<BlockPos> pipes = ConcurrentHashMap.newKeySet();

    public SqlitePipeRepository(Database database) {
        this.database = database;
        loadAll();
    }

    private void loadAll() {
        database.sync(connection -> {
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery(
                         "SELECT world, x, y, z FROM pipes")) {
                while (rs.next()) {
                    pipes.add(new BlockPos(UUID.fromString(rs.getString(1)),
                            rs.getInt(2), rs.getInt(3), rs.getInt(4)));
                }
            }
        });
    }

    @Override
    public boolean has(BlockPos pos) {
        return pipes.contains(pos);
    }

    @Override
    public void add(BlockPos pos) {
        if (!pipes.add(pos)) {
            return;
        }
        database.async(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT OR IGNORE INTO pipes (world, x, y, z) VALUES (?, ?, ?, ?)")) {
                bind(statement, pos);
                statement.executeUpdate();
            }
        });
    }

    @Override
    public void remove(BlockPos pos) {
        if (!pipes.remove(pos)) {
            return;
        }
        database.async(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM pipes WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
                bind(statement, pos);
                statement.executeUpdate();
            }
        });
    }

    @Override
    public Set<BlockPos> all() {
        return Set.copyOf(pipes);
    }

    private static void bind(PreparedStatement statement, BlockPos pos)
            throws java.sql.SQLException {
        statement.setString(1, pos.worldId().toString());
        statement.setInt(2, pos.x());
        statement.setInt(3, pos.y());
        statement.setInt(4, pos.z());
    }
}
