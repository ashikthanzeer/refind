package com.refind.database;

import com.refind.exception.DatabaseException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public final class DatabaseConnection {
    private static final String CONFIG_FILE = "database.properties";
    private static final Properties PROPERTIES = loadProperties();
    private static final String FALLBACK_H2_URL = "jdbc:h2:mem:lostfound;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    private static volatile boolean useFallback = false;
    private static volatile boolean schemaInitialized = false;

    private DatabaseConnection() {}

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = DatabaseConnection.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input != null) properties.load(input);
        } catch (IOException e) {
            throw new DatabaseException("Failed to load database configuration.", e);
        }
        String url = System.getenv("REFIND_DB_URL");
        String username = System.getenv("REFIND_DB_USERNAME");
        String password = System.getenv("REFIND_DB_PASSWORD");
        if (url != null && !url.isBlank()) properties.setProperty("db.url", url);
        if (username != null) properties.setProperty("db.username", username);
        if (password != null) properties.setProperty("db.password", password);
        if (properties.getProperty("db.url") == null) properties.setProperty("db.url", "jdbc:mysql://localhost:3306/lostfound");
        if (properties.getProperty("db.username") == null) properties.setProperty("db.username", "root");
        if (properties.getProperty("db.password") == null) properties.setProperty("db.password", "");
        return properties;
    }

    public static Connection getConnection() {
        if (useFallback) {
            return getFallbackConnection();
        }

        try {
            return DriverManager.getConnection(
                    PROPERTIES.getProperty("db.url"),
                    PROPERTIES.getProperty("db.username"),
                    PROPERTIES.getProperty("db.password")
            );
        } catch (SQLException e) {
            System.err.println("[DatabaseConnection] MySQL connection unavailable (" + e.getMessage()
                    + "). Seamlessly switching to resilient in-memory database.");
            useFallback = true;
            return getFallbackConnection();
        }
    }

    public static boolean isUsingFallback() {
        return useFallback;
    }

    private static synchronized Connection getFallbackConnection() {
        try {
            Connection conn = DriverManager.getConnection(FALLBACK_H2_URL, "sa", "");
            if (!schemaInitialized) {
                initFallbackSchema(conn);
                schemaInitialized = true;
            }
            return conn;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to establish fallback in-memory database connection.", e);
        }
    }

    private static void initFallbackSchema(Connection conn) {
        String[] ddlStatements = {
                "CREATE TABLE IF NOT EXISTS users (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "name VARCHAR(120) NOT NULL, " +
                        "email VARCHAR(255) NOT NULL UNIQUE, " +
                        "password_hash VARCHAR(255) NOT NULL, " +
                        "role VARCHAR(30) NOT NULL DEFAULT 'USER', " +
                        "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP)",

                "CREATE TABLE IF NOT EXISTS categories (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "name VARCHAR(60) NOT NULL UNIQUE)",

                "CREATE TABLE IF NOT EXISTS locations (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "campus VARCHAR(120) NOT NULL, " +
                        "building VARCHAR(120), " +
                        "room VARCHAR(120))",

                "CREATE TABLE IF NOT EXISTS items (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "title VARCHAR(120) NOT NULL, " +
                        "description TEXT, " +
                        "type VARCHAR(10) NOT NULL, " +
                        "status VARCHAR(20) NOT NULL DEFAULT 'FOUND', " +
                        "category_id INT NOT NULL, " +
                        "location_id INT, " +
                        "reporter_id INT NOT NULL, " +
                        "reported_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                        "image_path VARCHAR(255))",

                "CREATE TABLE IF NOT EXISTS claims (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "user_id INT NOT NULL, " +
                        "item_id INT NOT NULL, " +
                        "message TEXT, " +
                        "status VARCHAR(20) NOT NULL DEFAULT 'PENDING', " +
                        "submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                        "decided_at DATETIME)",

                "CREATE TABLE IF NOT EXISTS moderation (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "claim_id INT NOT NULL UNIQUE, " +
                        "moderator_id INT NOT NULL, " +
                        "decision VARCHAR(20) NOT NULL, " +
                        "comment TEXT, " +
                        "decided_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP)",

                "CREATE TABLE IF NOT EXISTS notifications (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "user_id INT NOT NULL, " +
                        "text TEXT NOT NULL, " +
                        "read_flag BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP)"
        };

        try (Statement stmt = conn.createStatement()) {
            for (String sql : ddlStatements) {
                stmt.execute(sql);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to initialize fallback database schema.", e);
        }
    }
}
