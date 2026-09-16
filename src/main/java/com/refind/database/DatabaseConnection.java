package com.refind.database;

import com.refind.exception.DatabaseException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DatabaseConnection {
    private static final String CONFIG_FILE = "database.properties";
    private static final Properties PROPERTIES = loadProperties();
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
        if (properties.getProperty("db.url") == null) properties.setProperty("db.url", "jdbc:mysql://localhost:3306/refind");
        if (properties.getProperty("db.username") == null) properties.setProperty("db.username", "root");
        if (properties.getProperty("db.password") == null) properties.setProperty("db.password", "");
        return properties;
    }
    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(PROPERTIES.getProperty("db.url"), PROPERTIES.getProperty("db.username"), PROPERTIES.getProperty("db.password"));
        } catch (SQLException e) {
            throw new DatabaseException("Failed to establish database connection to " + PROPERTIES.getProperty("db.url"), e);
        }
    }
}
