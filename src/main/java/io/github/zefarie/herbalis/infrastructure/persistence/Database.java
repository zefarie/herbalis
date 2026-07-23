package io.github.zefarie.herbalis.infrastructure.persistence;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Connexion SQLite unique, serialisee par un executor mono-thread :
 * les ecritures sont asynchrones, les lectures de demarrage synchrones.
 */
public final class Database implements AutoCloseable {

    @FunctionalInterface
    public interface SqlWork {
        void run(Connection connection) throws SQLException;
    }

    private final Connection connection;
    private final ExecutorService executor;
    private final Logger logger;

    public Database(File file, Logger logger) {
        this.logger = logger;
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "Herbalis-SQLite");
            thread.setDaemon(false);
            return thread;
        });
        try {
            this.connection = DriverManager.getConnection(
                    "jdbc:sqlite:" + file.getAbsolutePath());
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA journal_mode=WAL");
                statement.execute("PRAGMA foreign_keys=ON");
                statement.execute("PRAGMA synchronous=NORMAL");
            }
            createSchema();
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible d'ouvrir la base SQLite", e);
        }
    }

    private void createSchema() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS pots (
                        world TEXT NOT NULL,
                        x INTEGER NOT NULL,
                        y INTEGER NOT NULL,
                        z INTEGER NOT NULL,
                        dripper INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY (world, x, y, z)
                    )""");
            // Migration vers le goutte-a-goutte.
            addColumnIfMissing(statement, "pots",
                    "dripper", "INTEGER NOT NULL DEFAULT 0");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS plants (
                        id TEXT PRIMARY KEY,
                        world TEXT NOT NULL,
                        x INTEGER NOT NULL,
                        y INTEGER NOT NULL,
                        z INTEGER NOT NULL,
                        drug_id TEXT NOT NULL,
                        stage INTEGER NOT NULL,
                        stage_growth_ms INTEGER NOT NULL,
                        ripen_ms INTEGER NOT NULL,
                        hydration REAL NOT NULL,
                        hydration_sum REAL NOT NULL,
                        hydration_samples INTEGER NOT NULL,
                        dry_ms INTEGER NOT NULL,
                        fertilized_stage INTEGER NOT NULL,
                        fertilizer_uses INTEGER NOT NULL,
                        seed_quality INTEGER NOT NULL DEFAULT 2,
                        topping INTEGER NOT NULL DEFAULT 0,
                        pest_ms INTEGER NOT NULL DEFAULT 0,
                        pest_damage INTEGER NOT NULL DEFAULT 0,
                        state TEXT NOT NULL,
                        planted_at INTEGER NOT NULL,
                        last_tick_at INTEGER NOT NULL DEFAULT 0,
                        UNIQUE (world, x, y, z)
                    )""");
            // Migration des bases anterieures a la genetique et a la taille.
            addColumnIfMissing(statement, "plants",
                    "seed_quality", "INTEGER NOT NULL DEFAULT 2");
            addColumnIfMissing(statement, "plants",
                    "topping", "INTEGER NOT NULL DEFAULT 0");
            // Migration vers la croissance en temps reel (0 = inconnu,
            // le chargement repart de l'instant present).
            addColumnIfMissing(statement, "plants",
                    "last_tick_at", "INTEGER NOT NULL DEFAULT 0");
            // Migration vers les nuisibles.
            addColumnIfMissing(statement, "plants",
                    "pest_ms", "INTEGER NOT NULL DEFAULT 0");
            addColumnIfMissing(statement, "plants",
                    "pest_damage", "INTEGER NOT NULL DEFAULT 0");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS racks (
                        id TEXT PRIMARY KEY,
                        world TEXT NOT NULL,
                        x INTEGER NOT NULL,
                        y INTEGER NOT NULL,
                        z INTEGER NOT NULL,
                        drug_id TEXT NOT NULL DEFAULT '',
                        UNIQUE (world, x, y, z)
                    )""");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS rack_slots (
                        rack_id TEXT NOT NULL REFERENCES racks(id) ON DELETE CASCADE,
                        slot INTEGER NOT NULL,
                        quality INTEGER NOT NULL,
                        started_at INTEGER NOT NULL,
                        PRIMARY KEY (rack_id, slot)
                    )""");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS jars (
                        id TEXT PRIMARY KEY,
                        world TEXT NOT NULL,
                        x INTEGER NOT NULL,
                        y INTEGER NOT NULL,
                        z INTEGER NOT NULL,
                        drug_id TEXT NOT NULL DEFAULT '',
                        UNIQUE (world, x, y, z)
                    )""");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS jar_slots (
                        jar_id TEXT NOT NULL REFERENCES jars(id) ON DELETE CASCADE,
                        slot INTEGER NOT NULL,
                        quality INTEGER NOT NULL,
                        started_at INTEGER NOT NULL,
                        PRIMARY KEY (jar_id, slot)
                    )""");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS tanks (
                        id TEXT PRIMARY KEY,
                        world TEXT NOT NULL,
                        x INTEGER NOT NULL,
                        y INTEGER NOT NULL,
                        z INTEGER NOT NULL,
                        size TEXT NOT NULL,
                        stock REAL NOT NULL DEFAULT 0,
                        UNIQUE (world, x, y, z)
                    )""");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS silos (
                        id TEXT PRIMARY KEY,
                        world TEXT NOT NULL,
                        x INTEGER NOT NULL,
                        y INTEGER NOT NULL,
                        z INTEGER NOT NULL,
                        doses INTEGER NOT NULL DEFAULT 0,
                        UNIQUE (world, x, y, z)
                    )""");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS pipes (
                        world TEXT NOT NULL,
                        x INTEGER NOT NULL,
                        y INTEGER NOT NULL,
                        z INTEGER NOT NULL,
                        PRIMARY KEY (world, x, y, z)
                    )""");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS lamps (
                        world TEXT NOT NULL,
                        x INTEGER NOT NULL,
                        y INTEGER NOT NULL,
                        z INTEGER NOT NULL,
                        enabled INTEGER NOT NULL DEFAULT 1,
                        PRIMARY KEY (world, x, y, z)
                    )""");
            addColumnIfMissing(statement, "lamps", "enabled",
                    "INTEGER NOT NULL DEFAULT 1");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS players (
                        uuid TEXT PRIMARY KEY,
                        tolerance REAL NOT NULL,
                        addiction REAL NOT NULL,
                        last_updated_at INTEGER NOT NULL,
                        last_consumed_at INTEGER NOT NULL,
                        recent_consumptions TEXT NOT NULL DEFAULT ''
                    )""");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS sessions (
                        uuid TEXT PRIMARY KEY,
                        drug_id TEXT NOT NULL,
                        quality INTEGER NOT NULL,
                        started_at INTEGER NOT NULL,
                        rise_ms INTEGER NOT NULL,
                        high_ms INTEGER NOT NULL,
                        comedown_ms INTEGER NOT NULL,
                        intensity REAL NOT NULL
                    )""");
        }
    }

    /** Ajoute une colonne si elle n'existe pas encore (migration douce). */
    private static void addColumnIfMissing(Statement statement, String table,
                                           String column, String definition)
            throws SQLException {
        try (var rs = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    return;
                }
            }
        }
        statement.executeUpdate("ALTER TABLE " + table
                + " ADD COLUMN " + column + " " + definition);
    }

    /** Execute un travail SQL sur le thread dedie, sans bloquer le serveur. */
    public void async(SqlWork work) {
        executor.execute(() -> {
            try {
                work.run(connection);
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Erreur SQLite asynchrone", e);
            }
        });
    }

    /** Execute un travail SQL en bloquant (demarrage, arret). */
    public void sync(SqlWork work) {
        try {
            work.run(connection);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur SQLite", e);
        }
    }

    /** Vide la file d'ecritures puis ferme la connexion. */
    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warning("File SQLite non videe apres 10 s");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            connection.close();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Fermeture SQLite", e);
        }
    }
}
