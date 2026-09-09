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

    private DatabaseConnection() {
        // Prevent instantiation.
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();

        try (InputStream input =
                     DatabaseConnection.class
                             .getClassLoader()
                             .getResourceAsStream(CONFIG_FILE)) {

            if (input == null) {
                throw new DatabaseException(
                        "Database configuration file not found: " + CONFIG_FILE
                );
            }

            properties.load(input);
            return properties;

        } catch (IOException e) {
            throw new DatabaseException(
                    "Failed to load database configuration.", e
            );
        }
    }

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(
                    PROPERTIES.getProperty("db.url"),
                    PROPERTIES.getProperty("db.username"),
                    PROPERTIES.getProperty("db.password")
            );
        } catch (SQLException e) {
            throw new DatabaseException(
                    "Failed to establish database connection.", e
            );
        }
    }
}
