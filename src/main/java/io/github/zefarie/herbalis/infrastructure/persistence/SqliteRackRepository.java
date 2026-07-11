package io.github.zefarie.herbalis.infrastructure.persistence;

import io.github.zefarie.herbalis.application.port.RackRepository;
import io.github.zefarie.herbalis.domain.drying.DryingRack;
import io.github.zefarie.herbalis.domain.drying.DryingSlot;
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
 * Racks de sechage : memoire vive et ecriture directe (les depots et
 * recuperations sont rares). Le sechage lui-meme repose sur des
 * timestamps, rien a ticker en base.
 */
public final class SqliteRackRepository implements RackRepository {

    private final Database database;
    private final Map<BlockPos, DryingRack> racks = new ConcurrentHashMap<>();

    public SqliteRackRepository(Database database) {
        this.database = database;
        loadAll();
    }

    private void loadAll() {
        database.sync(connection -> {
            Map<String, List<DryingSlot>> slotsByRack = new HashMap<>();
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery(
                         "SELECT rack_id, quality, started_at FROM rack_slots ORDER BY slot")) {
                while (rs.next()) {
                    slotsByRack.computeIfAbsent(rs.getString(1), k -> new ArrayList<>())
                            .add(new DryingSlot(Quality.of(rs.getInt(2)), rs.getLong(3)));
                }
            }
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery(
                         "SELECT id, world, x, y, z, drug_id FROM racks")) {
                while (rs.next()) {
                    String id = rs.getString(1);
                    DryingRack rack = new DryingRack(
                            UUID.fromString(id),
                            new BlockPos(UUID.fromString(rs.getString(2)),
                                    rs.getInt(3), rs.getInt(4), rs.getInt(5)),
                            rs.getString(6),
                            slotsByRack.getOrDefault(id, List.of()));
                    racks.put(rack.pos(), rack);
                }
            }
        });
    }

    @Override
    public Optional<DryingRack> at(BlockPos pos) {
        return Optional.ofNullable(racks.get(pos));
    }

    @Override
    public void put(DryingRack rack) {
        racks.put(rack.pos(), rack);
        database.async(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO racks (id, world, x, y, z, drug_id)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT(id) DO UPDATE SET drug_id = excluded.drug_id
                    """)) {
                statement.setString(1, rack.id().toString());
                statement.setString(2, rack.pos().worldId().toString());
                statement.setInt(3, rack.pos().x());
                statement.setInt(4, rack.pos().y());
                statement.setInt(5, rack.pos().z());
                statement.setString(6, rack.drugId());
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM rack_slots WHERE rack_id = ?")) {
                statement.setString(1, rack.id().toString());
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO rack_slots (rack_id, slot, quality, started_at) "
                            + "VALUES (?, ?, ?, ?)")) {
                int slot = 0;
                for (DryingSlot dryingSlot : rack.slots()) {
                    statement.setString(1, rack.id().toString());
                    statement.setInt(2, slot++);
                    statement.setInt(3, dryingSlot.quality().stars());
                    statement.setLong(4, dryingSlot.startedAt());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        });
    }

    @Override
    public void remove(BlockPos pos) {
        DryingRack removed = racks.remove(pos);
        if (removed == null) {
            return;
        }
        database.async(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM racks WHERE id = ?")) {
                statement.setString(1, removed.id().toString());
                statement.executeUpdate();
            }
        });
    }

    @Override
    public Collection<DryingRack> all() {
        return List.copyOf(racks.values());
    }
}
