package io.github.zefarie.herbalis.infrastructure.persistence;

import io.github.zefarie.herbalis.application.port.SiloRepository;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.irrigation.FertilizerSilo;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Silos d'engrais : memoire vive comme source de verite, SQLite en
 * ecriture directe (une dose part au plus une fois par stage de plante,
 * l'evenement est rare).
 */
public final class SqliteSiloRepository implements SiloRepository {

    private static final String UPSERT = """
            INSERT INTO silos (id, world, x, y, z, doses)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET doses = excluded.doses
            """;

    private final Database database;
    private final Map<BlockPos, FertilizerSilo> silos = new ConcurrentHashMap<>();

    public SqliteSiloRepository(Database database) {
        this.database = database;
        loadAll();
    }

    private void loadAll() {
        database.sync(connection -> {
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery(
                         "SELECT id, world, x, y, z, doses FROM silos")) {
                while (rs.next()) {
                    FertilizerSilo silo = new FertilizerSilo(
                            UUID.fromString(rs.getString("id")),
                            new BlockPos(UUID.fromString(rs.getString("world")),
                                    rs.getInt("x"), rs.getInt("y"), rs.getInt("z")),
                            rs.getInt("doses"));
                    silos.put(silo.pos(), silo);
                }
            }
        });
    }

    @Override
    public Optional<FertilizerSilo> at(BlockPos pos) {
        return Optional.ofNullable(silos.get(pos));
    }

    @Override
    public void put(FertilizerSilo silo) {
        silos.put(silo.pos(), silo);
        database.async(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(UPSERT)) {
                statement.setString(1, silo.id().toString());
                statement.setString(2, silo.pos().worldId().toString());
                statement.setInt(3, silo.pos().x());
                statement.setInt(4, silo.pos().y());
                statement.setInt(5, silo.pos().z());
                statement.setInt(6, silo.doses());
                statement.executeUpdate();
            }
        });
    }

    @Override
    public void remove(BlockPos pos) {
        FertilizerSilo removed = silos.remove(pos);
        if (removed == null) {
            return;
        }
        database.async(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM silos WHERE id = ?")) {
                statement.setString(1, removed.id().toString());
                statement.executeUpdate();
            }
        });
    }

    @Override
    public Collection<FertilizerSilo> all() {
        return List.copyOf(silos.values());
    }
}
