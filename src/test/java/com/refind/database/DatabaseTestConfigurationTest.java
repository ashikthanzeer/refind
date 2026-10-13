package com.refind.database;

import com.refind.exception.DatabaseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseTestConfigurationTest {

    @AfterEach
    void clearStartupProperty() {
        System.clearProperty("refind.autoInitializeSchema");
    }

    @Test
    void acceptsClearlyNamedDedicatedTestDatabase() {
        assertDoesNotThrow(() -> DatabaseConnection.validateIntegrationTestDatabase(
                "jdbc:mysql://db.example.test:3306/refind_test?sslMode=REQUIRED"));
    }

    @Test
    void rejectsApplicationDatabaseAndDisabledTls() {
        assertThrows(DatabaseException.class, () -> DatabaseConnection.validateIntegrationTestDatabase(
                "jdbc:mysql://db.example.test:3306/defaultdb?sslMode=REQUIRED"));
        assertThrows(DatabaseException.class, () -> DatabaseConnection.validateIntegrationTestDatabase(
                "jdbc:mysql://db.example.test:3306/refind_test?sslMode=DISABLED"));
    }

    @Test
    void schemaCreationRequiresExplicitOptIn() {
        System.setProperty("refind.autoInitializeSchema", "false");
        assertFalse(DatabaseConnection.isAutoSchemaInitializationEnabled());

        System.setProperty("refind.autoInitializeSchema", "true");
        assertTrue(DatabaseConnection.isAutoSchemaInitializationEnabled());
    }
}
