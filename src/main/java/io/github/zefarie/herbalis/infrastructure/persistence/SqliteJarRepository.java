package io.github.zefarie.herbalis.infrastructure.persistence;

import io.github.zefarie.herbalis.application.port.JarRepository;
import io.github.zefarie.herbalis.domain.curing.CuringJar;
import io.github.zefarie.herbalis.domain.curing.CuringSlot;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.quality.Quality;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Jarres de curing : memoire vive et ecriture directe (les depots et
 * recuperations sont rares). L'affinage lui-meme repose sur des
 * timestamps, rien a ticker en base.
 */
public final class SqliteJarRepository implements JarRepository {

    private final Database database;
    private final Map<BlockPos, CuringJar> jars = new ConcurrentHashMap<>();

    public SqliteJarRepository(Database database) {
        this.database = database;
        loadAll();
    }

    private void loadAll() {
        database.sync(connection -> {
            Map<String, List<CuringSlot>> slotsByJar = new HashMap<>();
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery(
                         "SELECT jar_id, quality, started_at FROM jar_slots ORDER BY slot")) {
                while (rs.next()) {
                    slotsByJar.computeIfAbsent(rs.getString(1), k -> new ArrayList<>())
                            .add(new CuringSlot(Quality.of(rs.getInt(2)), rs.getLong(3)));
                }
            }
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery(
                         "SELECT id, world, x, y, z, drug_id FROM jars")) {
                while (rs.next()) {
                    String id = rs.getString(1);
                    CuringJar jar = new CuringJar(
                            UUID.fromString(id),
                            new BlockPos(UUID.fromString(rs.getString(2)),
                                    rs.getInt(3), rs.getInt(4), rs.getInt(5)),
                            rs.getString(6),
                            slotsByJar.getOrDefault(id, List.of()));
                    jars.put(jar.pos(), jar);
                }
            }
        });
    }

    @Override
    public Optional<CuringJar> at(BlockPos pos) {
        return Optional.ofNullable(jars.get(pos));
    }

    @Override
    public void put(CuringJar jar) {
        jars.put(jar.pos(), jar);
        database.async(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO jars (id, world, x, y, z, drug_id)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT(id) DO UPDATE SET drug_id = excluded.drug_id
                    """)) {
                statement.setString(1, jar.id().toString());
                statement.setString(2, jar.pos().worldId().toString());
                statement.setInt(3, jar.pos().x());
                statement.setInt(4, jar.pos().y());
                statement.setInt(5, jar.pos().z());
                statement.setString(6, jar.drugId());
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM jar_slots WHERE jar_id = ?")) {
                statement.setString(1, jar.id().toString());
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO jar_slots (jar_id, slot, quality, started_at) "
                            + "VALUES (?, ?, ?, ?)")) {
                int slot = 0;
                for (CuringSlot curingSlot : jar.slots()) {
                    statement.setString(1, jar.id().toString());
                    statement.setInt(2, slot++);
                    statement.setInt(3, curingSlot.quality().stars());
                    statement.setLong(4, curingSlot.startedAt());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        });
    }

    @Override
    public void remove(BlockPos pos) {
        CuringJar removed = jars.remove(pos);
        if (removed == null) {
            return;
        }
        database.async(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM jars WHERE id = ?")) {
                statement.setString(1, removed.id().toString());
                statement.executeUpdate();
            }
        });
    }

    @Override
    public Collection<CuringJar> all() {
        return List.copyOf(jars.values());
    }
}
