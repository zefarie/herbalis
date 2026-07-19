package io.github.zefarie.herbalis.infrastructure.persistence;

import io.github.zefarie.herbalis.application.port.TankRepository;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.irrigation.TankSize;
import io.github.zefarie.herbalis.domain.irrigation.WaterTank;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caissons d'eau : memoire vive comme source de verite, ecriture differee.
 * Le stock bouge a chaque tick de croissance; on marque les positions
 * sales et la sauvegarde periodique (ou l'arret) pousse le tout en base.
 */
public final class SqliteTankRepository implements TankRepository {

    private static final String UPSERT = """
            INSERT INTO tanks (id, world, x, y, z, size, stock)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET stock = excluded.stock
            """;

    private final Database database;
    private final Map<BlockPos, WaterTank> tanks = new ConcurrentHashMap<>();
    private final Set<BlockPos> dirty = ConcurrentHashMap.newKeySet();

    public SqliteTankRepository(Database database) {
        this.database = database;
        loadAll();
    }

    private void loadAll() {
        database.sync(connection -> {
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery(
                         "SELECT id, world, x, y, z, size, stock FROM tanks")) {
                while (rs.next()) {
                    TankSize size = TankSize.byId(rs.getString("size"))
                            .orElse(TankSize.CUVE);
                    WaterTank tank = new WaterTank(
                            UUID.fromString(rs.getString("id")),
                            new BlockPos(UUID.fromString(rs.getString("world")),
                                    rs.getInt("x"), rs.getInt("y"), rs.getInt("z")),
                            size,
                            rs.getDouble("stock"));
                    tanks.put(tank.pos(), tank);
                }
            }
        });
    }

    @Override
    public Optional<WaterTank> at(BlockPos pos) {
        return Optional.ofNullable(tanks.get(pos));
    }

    @Override
    public void put(WaterTank tank) {
        tanks.put(tank.pos(), tank);
        dirty.add(tank.pos());
    }

    @Override
    public void remove(BlockPos pos) {
        WaterTank removed = tanks.remove(pos);
        dirty.remove(pos);
        if (removed == null) {
            return;
        }
        database.async(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM tanks WHERE id = ?")) {
                statement.setString(1, removed.id().toString());
                statement.executeUpdate();
            }
        });
    }

    @Override
    public Collection<WaterTank> all() {
        return List.copyOf(tanks.values());
    }

    /** Pousse tous les caissons modifies vers SQLite (asynchrone). */
    public void flush() {
        List<WaterTank> snapshot = dirty.stream()
                .map(tanks::get)
                .filter(java.util.Objects::nonNull)
                .toList();
        dirty.clear();
        if (snapshot.isEmpty()) {
            return;
        }
        database.async(connection -> upsertBatch(connection, snapshot));
    }

    /** Sauvegarde bloquante de tout l'etat (arret du serveur). */
    public void flushSync() {
        List<WaterTank> snapshot = List.copyOf(tanks.values());
        dirty.clear();
        if (snapshot.isEmpty()) {
            return;
        }
        database.sync(connection -> upsertBatch(connection, snapshot));
    }

    private static void upsertBatch(java.sql.Connection connection,
                                    List<WaterTank> batch)
            throws java.sql.SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPSERT)) {
            for (WaterTank tank : batch) {
                statement.setString(1, tank.id().toString());
                statement.setString(2, tank.pos().worldId().toString());
                statement.setInt(3, tank.pos().x());
                statement.setInt(4, tank.pos().y());
                statement.setInt(5, tank.pos().z());
                statement.setString(6, tank.size().id());
                statement.setDouble(7, tank.stock());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }
}
