package de.gergh0stface.ghostyhud.managers;

import de.gergh0stface.ghostyhud.GhostyHUD;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.UUID;

/**
 * Handles persistent player data storage.
 * Supports YAML (default) and MySQL – configured via config.yml.
 */
public class DatabaseManager {

    private final GhostyHUD plugin;
    private boolean useMySQL;

    // ── YAML ──────────────────────────────────────────────────────────────────
    private File dataFile;
    private FileConfiguration dataConfig;

    // ── MySQL ─────────────────────────────────────────────────────────────────
    private Connection connection;
    private String mysqlUrl, mysqlUser, mysqlPass;

    public DatabaseManager(GhostyHUD plugin) {
        this.plugin = plugin;
    }

    // ── Initialisation ────────────────────────────────────────────────────────

    public void initialize() {
        String type = plugin.getConfig().getString("database.type", "yaml");
        useMySQL = "mysql".equalsIgnoreCase(type);

        if (useMySQL) {
            initMySQL();
        } else {
            initYaml();
        }
    }

    private void initYaml() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) plugin.saveResource("data.yml", false);
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        plugin.getLogger().info("Database: Using YAML (data.yml).");
    }

    private void initMySQL() {
        String host = plugin.getConfig().getString("database.mysql.host", "localhost");
        int    port = plugin.getConfig().getInt("database.mysql.port", 3306);
        String db   = plugin.getConfig().getString("database.mysql.database", "ghostyhud");
        mysqlUser   = plugin.getConfig().getString("database.mysql.username", "root");
        mysqlPass   = plugin.getConfig().getString("database.mysql.password", "");

        mysqlUrl = "jdbc:mysql://" + host + ":" + port + "/" + db
                + "?useSSL=false&autoReconnect=true&characterEncoding=utf8mb4"
                + "&serverTimezone=UTC";

        try {
            openConnection();
            createTable();
            plugin.getLogger().info("Database: Connected to MySQL (" + host + ":" + port + "/" + db + ").");
        } catch (SQLException ex) {
            plugin.getLogger().severe("Database: Could not connect to MySQL! Falling back to YAML.");
            plugin.getLogger().severe("  Reason: " + ex.getMessage());
            useMySQL = false;
            initYaml();
        }
    }

    private void openConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) return;
        connection = DriverManager.getConnection(mysqlUrl, mysqlUser, mysqlPass);
    }

    private void createTable() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS ghostyhud_players (
                        uuid        VARCHAR(36)  NOT NULL PRIMARY KEY,
                        name        VARCHAR(16)  NOT NULL,
                        first_join  BIGINT       NOT NULL,
                        last_seen   BIGINT       NOT NULL,
                        join_count  INT          NOT NULL DEFAULT 1
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """);
        }
    }

    // ── Player operations ─────────────────────────────────────────────────────

    /**
     * Called when a player joins. Creates or updates their record.
     */
    public void loadPlayer(Player player) {
        UUID   uuid = player.getUniqueId();
        String name = player.getName();
        long   now  = System.currentTimeMillis();

        if (useMySQL) {
            try {
                openConnection();
                // Upsert
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO ghostyhud_players (uuid, name, first_join, last_seen, join_count) "
                        + "VALUES (?, ?, ?, ?, 1) "
                        + "ON DUPLICATE KEY UPDATE name=VALUES(name), last_seen=VALUES(last_seen), join_count=join_count+1");
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.setLong  (3, now);
                ps.setLong  (4, now);
                ps.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().warning("MySQL error on loadPlayer: " + ex.getMessage());
            }
        } else {
            String path = "players." + uuid;
            boolean isNew = !dataConfig.contains(path);
            dataConfig.set(path + ".name",       name);
            dataConfig.set(path + ".last-seen",  now);
            if (isNew) {
                dataConfig.set(path + ".first-join", now);
                dataConfig.set(path + ".join-count", 1);
            } else {
                dataConfig.set(path + ".join-count",
                        dataConfig.getInt(path + ".join-count", 0) + 1);
            }
            saveYaml();
        }
    }

    /**
     * Returns true if this is the very first time the player has joined.
     * Must be called BEFORE loadPlayer() or immediately after.
     */
    public boolean isFirstJoin(UUID uuid) {
        if (useMySQL) {
            try {
                openConnection();
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT join_count FROM ghostyhud_players WHERE uuid=?");
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getInt("join_count") <= 1;
            } catch (SQLException ex) {
                plugin.getLogger().warning("MySQL error on isFirstJoin: " + ex.getMessage());
            }
            return false;
        } else {
            return dataConfig.getInt("players." + uuid + ".join-count", 0) <= 1;
        }
    }

    public int getJoinCount(UUID uuid) {
        if (useMySQL) {
            try {
                openConnection();
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT join_count FROM ghostyhud_players WHERE uuid=?");
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getInt("join_count");
            } catch (SQLException ex) {
                plugin.getLogger().warning("MySQL error on getJoinCount: " + ex.getMessage());
            }
            return 0;
        } else {
            return dataConfig.getInt("players." + uuid + ".join-count", 0);
        }
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void saveYaml() {
        if (dataFile == null || dataConfig == null) return;
        try {
            dataConfig.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not save data.yml: " + ex.getMessage());
        }
    }

    public void close() {
        if (!useMySQL) {
            saveYaml();
            return;
        }
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException ex) {
            plugin.getLogger().warning("Error closing MySQL connection: " + ex.getMessage());
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public boolean isUsingMySQL() { return useMySQL; }
    public FileConfiguration getDataConfig() { return dataConfig; }
}
