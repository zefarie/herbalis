package io.github.zefarie.herbalis.infrastructure.persistence;

import io.github.zefarie.herbalis.application.port.PlantRepository;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.plant.Plant;
import io.github.zefarie.herbalis.domain.plant.PlantState;

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
 * Plantes : memoire vive comme source de verite, ecriture differee.
 * Les plantes sont tickees chaque seconde; on marque les positions sales
 * et la sauvegarde periodique (ou l'arret) pousse le tout en base.
 */
public final class SqlitePlantRepository implements PlantRepository {

    private static final String UPSERT = """
            INSERT INTO plants (id, world, x, y, z, drug_id, stage, stage_growth_ms,
                                ripen_ms, hydration, hydration_sum, hydration_samples,
                                dry_ms, fertilized_stage, fertilizer_uses,
                                seed_quality, topping, pest_ms, pest_damage,
                                state, planted_at, last_tick_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                stage = excluded.stage,
                stage_growth_ms = excluded.stage_growth_ms,
                ripen_ms = excluded.ripen_ms,
                hydration = excluded.hydration,
                hydration_sum = excluded.hydration_sum,
                hydration_samples = excluded.hydration_samples,
                dry_ms = excluded.dry_ms,
                fertilized_stage = excluded.fertilized_stage,
                fertilizer_uses = excluded.fertilizer_uses,
                seed_quality = excluded.seed_quality,
                topping = excluded.topping,
                pest_ms = excluded.pest_ms,
                pest_damage = excluded.pest_damage,
                state = excluded.state,
                last_tick_at = excluded.last_tick_at
            """;

    private final Database database;
    private final Map<BlockPos, Plant> plants = new ConcurrentHashMap<>();
    private final Set<BlockPos> dirty = ConcurrentHashMap.newKeySet();

    public SqlitePlantRepository(Database database) {
        this.database = database;
        loadAll();
    }

    private void loadAll() {
        database.sync(connection -> {
            long loadedAt = System.currentTimeMillis();
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT * FROM plants")) {
                while (rs.next()) {
                    // Bases d'avant la croissance temps reel : pas de date
                    // de tick connue, on repart d'ici (aucun rattrapage).
                    long lastTickAt = rs.getLong("last_tick_at");
                    Plant plant = new Plant(
                            UUID.fromString(rs.getString("id")),
                            rs.getString("drug_id"),
                            new BlockPos(UUID.fromString(rs.getString("world")),
                                    rs.getInt("x"), rs.getInt("y"), rs.getInt("z")),
                            rs.getInt("stage"),
                            rs.getLong("stage_growth_ms"),
                            rs.getLong("ripen_ms"),
                            rs.getDouble("hydration"),
                            rs.getDouble("hydration_sum"),
                            rs.getLong("hydration_samples"),
                            rs.getLong("dry_ms"),
                            rs.getInt("fertilized_stage"),
                            rs.getInt("fertilizer_uses"),
                            rs.getInt("seed_quality"),
                            rs.getInt("topping"),
                            rs.getLong("pest_ms"),
                            rs.getInt("pest_damage"),
                            PlantState.valueOf(rs.getString("state")),
                            rs.getLong("planted_at"),
                            lastTickAt > 0 ? lastTickAt : loadedAt);
                    plants.put(plant.pos(), plant);
                }
            }
        });
    }

    @Override
    public Optional<Plant> at(BlockPos pos) {
        return Optional.ofNullable(plants.get(pos));
    }

    @Override
    public void put(Plant plant) {
        plants.put(plant.pos(), plant);
        dirty.add(plant.pos());
    }

    @Override
    public void remove(BlockPos pos) {
        Plant removed = plants.remove(pos);
        dirty.remove(pos);
        if (removed == null) {
            return;
        }
        database.async(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM plants WHERE id = ?")) {
                statement.setString(1, removed.id().toString());
                statement.executeUpdate();
            }
        });
    }

    @Override
    public Collection<Plant> all() {
        return List.copyOf(plants.values());
    }

    /** Pousse toutes les plantes modifiees vers SQLite (asynchrone). */
    public void flush() {
        List<Plant> snapshot = dirty.stream()
                .map(plants::get)
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
        List<Plant> snapshot = List.copyOf(plants.values());
        dirty.clear();
        if (snapshot.isEmpty()) {
            return;
        }
        database.sync(connection -> upsertBatch(connection, snapshot));
    }

    private static void upsertBatch(java.sql.Connection connection, List<Plant> batch)
            throws java.sql.SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPSERT)) {
            for (Plant plant : batch) {
                statement.setString(1, plant.id().toString());
                statement.setString(2, plant.pos().worldId().toString());
                statement.setInt(3, plant.pos().x());
                statement.setInt(4, plant.pos().y());
                statement.setInt(5, plant.pos().z());
                statement.setString(6, plant.drugId());
                statement.setInt(7, plant.stage());
                statement.setLong(8, plant.stageGrowthMillis());
                statement.setLong(9, plant.ripenMillis());
                statement.setDouble(10, plant.hydration());
                statement.setDouble(11, plant.hydrationSum());
                statement.setLong(12, plant.hydrationSamples());
                statement.setLong(13, plant.dryMillis());
                statement.setInt(14, plant.fertilizedStage());
                statement.setInt(15, plant.fertilizerUses());
                statement.setInt(16, plant.seedQuality());
                statement.setInt(17, plant.topping());
                statement.setLong(18, plant.pestMillis());
                statement.setInt(19, plant.pestDamage());
                statement.setString(20, plant.state().name());
                statement.setLong(21, plant.plantedAt());
                statement.setLong(22, plant.lastTickAt());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }
}
