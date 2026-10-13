package com.refind.database;

import org.junit.jupiter.api.Test;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseConnectionTest {

    @Test
    void testNormalizeAivenUri() {
        Properties props = new Properties();
        props.setProperty("db.url", "mysql://avnadmin:SecretPass123@refind-db.aivencloud.com:14956/defaultdb?ssl-mode=REQUIRED");

        DatabaseConnection.normalizeAndConfigureUrl(props);

        assertEquals("jdbc:mysql://refind-db.aivencloud.com:14956/defaultdb?sslMode=REQUIRED", props.getProperty("db.url"));
        assertEquals("avnadmin", props.getProperty("db.username"));
        assertEquals("SecretPass123", props.getProperty("db.password"));
    }

    @Test
    void testNormalizeJdbcUrlWithSslModeKebabCase() {
        Properties props = new Properties();
        props.setProperty("db.url", "jdbc:mysql://refind-db.aivencloud.com:14956/defaultdb?ssl-mode=REQUIRED");

        DatabaseConnection.normalizeAndConfigureUrl(props);

        assertEquals("jdbc:mysql://refind-db.aivencloud.com:14956/defaultdb?sslMode=REQUIRED", props.getProperty("db.url"));
    }

    @Test
    void testSanitizeUrlMasksPasswordInAuthority() {
        String url = "mysql://avnadmin:MySuperSecretPassword@host.com:3306/db";
        String sanitized = DatabaseConnection.sanitizeUrl(url);

        assertFalse(sanitized.contains("MySuperSecretPassword"));
        assertTrue(sanitized.contains("***"));
    }

    @Test
    void testSanitizeUrlMasksPasswordInQuery() {
        String url = "jdbc:mysql://host.com:3306/db?password=SecretPass&user=root";
        String sanitized = DatabaseConnection.sanitizeUrl(url);

        assertFalse(sanitized.contains("SecretPass"));
        assertTrue(sanitized.contains("password=***"));
    }

    @Test
    void testDiagnosticsIdentifiesDnsError() {
        java.sql.SQLException ex = new java.sql.SQLException("Unknown host: some-nonexistent-domain.xyz");
        String diag = DatabaseConnection.buildDiagnosticMessage(ex, "jdbc:mysql://some-nonexistent-domain.xyz:3306/db");

        assertTrue(diag.contains("DNS resolution failure"));
    }

    @Test
    void testDiagnosticsIdentifiesAuthError() {
        java.sql.SQLException ex = new java.sql.SQLException("Access denied for user 'avnadmin'@'%'", "28000", 1045);
        String diag = DatabaseConnection.buildDiagnosticMessage(ex, "jdbc:mysql://host:3306/db");

        assertTrue(diag.contains("Authentication failure"));
    }
}
