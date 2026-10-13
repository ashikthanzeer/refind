package com.refind.database;

import com.refind.exception.DatabaseException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DatabaseConnection {
    private static final String CONFIG_FILE = "database.properties";
    private static final String INTEGRATION_TEST_MODE = "refind.integration";
    private static final String AUTO_SCHEMA_PROPERTY = "refind.autoInitializeSchema";
    private static final String AUTO_SCHEMA_ENV = "REFIND_AUTO_INITIALIZE_SCHEMA";
    private static final Properties PROPERTIES = loadProperties();

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ignored) {
            // Driver loaded via ServiceLoader if present on classpath
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
            } catch (Throwable ignored) {
            }
        }));
    }

    private DatabaseConnection() {}

    private static String getEnvAny(String... keys) {
        for (String key : keys) {
            String val = System.getenv(key);
            if (val != null && !val.isBlank()) {
                return val.trim();
            }
        }
        return null;
    }

    static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = DatabaseConnection.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            throw new DatabaseException("Failed to load database configuration.", e);
        }

        if (Boolean.getBoolean(INTEGRATION_TEST_MODE)) {
            configureIntegrationTestDatabase(properties);
            normalizeAndConfigureUrl(properties);
            validateIntegrationTestDatabase(properties.getProperty("db.url"));
            if (properties.getProperty("db.username") == null || properties.getProperty("db.username").isBlank()
                    || properties.getProperty("db.password") == null || properties.getProperty("db.password").isBlank()) {
                throw new DatabaseException("Integration tests require REFIND_TEST_DB_USERNAME and REFIND_TEST_DB_PASSWORD, "
                        + "unless both are embedded in REFIND_TEST_DB_URL.");
            }
            return properties;
        }

        // Support standard environment variables with fallbacks
        String envUrl = getEnvAny("REFIND_DB_URL", "DATABASE_URL", "MYSQL_URL", "DB_URL");
        String envUsername = getEnvAny("REFIND_DB_USERNAME", "DB_USERNAME", "DB_USER");
        String envPassword = getEnvAny("REFIND_DB_PASSWORD", "DB_PASSWORD");
        String envHost = getEnvAny("REFIND_DB_HOST", "DB_HOST");
        String envPort = getEnvAny("REFIND_DB_PORT", "DB_PORT");
        String envDatabase = getEnvAny("REFIND_DB_NAME", "DB_NAME", "MYSQL_DATABASE");

        if (envUrl != null) {
            properties.setProperty("db.url", envUrl);
        } else if (properties.getProperty("db.url") == null && envHost != null) {
            String port = envPort != null ? envPort : "3306";
            String db = envDatabase != null ? envDatabase : "defaultdb";
            properties.setProperty("db.url", "jdbc:mysql://" + envHost + ":" + port + "/" + db + "?sslMode=REQUIRED");
        }

        if (envUsername != null) {
            properties.setProperty("db.username", envUsername);
        }
        if (envPassword != null) {
            properties.setProperty("db.password", envPassword);
        }

        // Normalize URL and extract embedded credentials if provided in standard URI format
        normalizeAndConfigureUrl(properties);

        if (properties.getProperty("db.url") == null) {
            properties.setProperty("db.url", "jdbc:mysql://localhost:3306/lostfound");
        }
        if (properties.getProperty("db.username") == null) {
            properties.setProperty("db.username", "root");
        }
        if (properties.getProperty("db.password") == null) {
            properties.setProperty("db.password", "");
        }

        return properties;
    }

    /**
     * Integration tests must opt in to a separate database.  They never inherit
     * the application's normal database.properties or REFIND_DB_* settings.
     */
    private static void configureIntegrationTestDatabase(Properties properties) {
        properties.remove("db.url");
        properties.remove("db.username");
        properties.remove("db.password");

        String url = getEnvAny("REFIND_TEST_DB_URL");
        if (url == null) {
            throw new DatabaseException("Integration tests require REFIND_TEST_DB_URL. "
                    + "Refusing to use the application database configuration.");
        }
        properties.setProperty("db.url", url);

        String username = getEnvAny("REFIND_TEST_DB_USERNAME");
        String password = getEnvAny("REFIND_TEST_DB_PASSWORD");
        if (username != null) {
            properties.setProperty("db.username", username);
        }
        if (password != null) {
            properties.setProperty("db.password", password);
        }
    }

    /** Package-visible for unit tests; does not connect to a database. */
    static void validateIntegrationTestDatabase(String url) {
        if (url == null || !url.startsWith("jdbc:mysql://")) {
            throw new DatabaseException("REFIND_TEST_DB_URL must be a jdbc:mysql URL for a dedicated test database.");
        }

        String withoutQuery = url.substring("jdbc:mysql://".length()).split("\\?", 2)[0];
        int firstSlash = withoutQuery.indexOf('/');
        String database = firstSlash >= 0 ? withoutQuery.substring(firstSlash + 1) : "";
        if (!database.matches("(?i)(test|test_[a-z0-9_]+|[a-z0-9_]+_test)")) {
            throw new DatabaseException("REFIND_TEST_DB_URL must target a dedicated database named test, test_*, or *_test; refusing unsafe target.");
        }
        if (url.toLowerCase().contains("sslmode=disabled") || url.toLowerCase().contains("ssl-mode=disabled")) {
            throw new DatabaseException("REFIND_TEST_DB_URL must not disable TLS.");
        }
    }

    /**
     * Normalizes standard URIs (such as mysql://user:pass@host:port/db?ssl-mode=REQUIRED)
     * into standard JDBC URLs (jdbc:mysql://host:port/db?sslMode=REQUIRED), extracting
     * embedded credentials if not already provided separately.
     */
    static void normalizeAndConfigureUrl(Properties properties) {
        String rawUrl = properties.getProperty("db.url");
        if (rawUrl == null || rawUrl.isBlank()) {
            return;
        }
        rawUrl = rawUrl.trim();

        if (rawUrl.startsWith("mysql://")) {
            try {
                URI uri = URI.create(rawUrl);
                String userInfo = uri.getUserInfo();
                if (userInfo != null && !userInfo.isEmpty()) {
                    String[] parts = userInfo.split(":", 2);
                    if (properties.getProperty("db.username") == null || properties.getProperty("db.username").isBlank()) {
                        properties.setProperty("db.username", URLDecoder.decode(parts[0], StandardCharsets.UTF_8));
                    }
                    if (parts.length > 1 && (properties.getProperty("db.password") == null || properties.getProperty("db.password").isBlank())) {
                        properties.setProperty("db.password", URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
                    }
                }

                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 3306;
                String path = uri.getPath();
                if (path == null || path.isEmpty() || path.equals("/")) {
                    path = "/defaultdb";
                }

                String query = uri.getQuery();
                if (query != null) {
                    query = query.replace("ssl-mode=", "sslMode=");
                }

                String normalizedJdbcUrl = "jdbc:mysql://" + host + ":" + port + path + (query != null ? "?" + query : "");
                properties.setProperty("db.url", normalizedJdbcUrl);
            } catch (Exception ignored) {
                // Fall back to replacing prefix and ssl parameter
                String normalized = "jdbc:" + rawUrl.replace("ssl-mode=", "sslMode=");
                properties.setProperty("db.url", normalized);
            }
        } else if (rawUrl.startsWith("jdbc:mysql://")) {
            String normalized = rawUrl.replace("ssl-mode=", "sslMode=");
            // If credentials were included inside jdbc authority: jdbc:mysql://user:pass@host:port/db
            Matcher matcher = Pattern.compile("^jdbc:mysql://([^:]+):([^@]+)@(.+)$").matcher(normalized);
            if (matcher.matches()) {
                if (properties.getProperty("db.username") == null || properties.getProperty("db.username").isBlank()) {
                    properties.setProperty("db.username", URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8));
                }
                if (properties.getProperty("db.password") == null || properties.getProperty("db.password").isBlank()) {
                    properties.setProperty("db.password", URLDecoder.decode(matcher.group(2), StandardCharsets.UTF_8));
                }
                normalized = "jdbc:mysql://" + matcher.group(3);
            }
            properties.setProperty("db.url", normalized);
        }
    }

    public static Connection getConnection() {
        String url = PROPERTIES.getProperty("db.url");
        String username = PROPERTIES.getProperty("db.username");
        String password = PROPERTIES.getProperty("db.password");
        try {
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            throw new DatabaseException(buildDiagnosticMessage(e, url), e);
        }
    }

    /**
     * Validates the configured schema before application services are created.
     * Schema creation is deliberately opt-in so normal startup cannot change a
     * live database merely by connecting to it.
     */
    public static void prepareApplicationDatabase(Connection connection) {
        if (isAutoSchemaInitializationEnabled()) {
            ensureSchemaInitialized(connection);
        }
        validateRequiredSchema(connection);
    }

    static boolean isAutoSchemaInitializationEnabled() {
        String configured = System.getProperty(AUTO_SCHEMA_PROPERTY);
        if (configured == null) {
            configured = System.getenv(AUTO_SCHEMA_ENV);
        }
        return isExplicitlyEnabled(configured);
    }

    /** Package-visible for unit tests. */
    static boolean isExplicitlyEnabled(String value) {
        return "true".equalsIgnoreCase(value == null ? "" : value.trim());
    }

    /**
     * Sanitizes a connection URL by masking embedded passwords.
     */
    public static String sanitizeUrl(String url) {
        if (url == null) return "null";
        // Mask user:password@
        String sanitized = url.replaceAll("://([^:]+):([^@]+)@", "://$1:***@");
        // Mask password query param
        sanitized = sanitized.replaceAll("(?i)(password=)[^&;]*", "$1***");
        return sanitized;
    }

    /**
     * Produces a clear, diagnostic message distinguishing DNS failures, network timeouts,
     * authentication errors, TLS failures, and schema issues without leaking credentials.
     */
    public static String buildDiagnosticMessage(SQLException e, String rawUrl) {
        String sanitized = sanitizeUrl(rawUrl);
        String message = e.getMessage() != null ? e.getMessage() : "";
        String sqlState = e.getSQLState() != null ? e.getSQLState() : "";
        int errorCode = e.getErrorCode();

        String lowerMessage = message.toLowerCase();
        String category;

        if (e.getCause() instanceof java.net.UnknownHostException || lowerMessage.contains("unknown host")) {
            category = "DNS resolution failure";
        } else if (lowerMessage.contains("communications link failure") || lowerMessage.contains("connect") || lowerMessage.contains("timed out") || lowerMessage.contains("timeout")) {
            category = "Network connectivity or timeout failure";
        } else if (errorCode == 1045 || "28000".equals(sqlState) || lowerMessage.contains("access denied")) {
            category = "Authentication failure (invalid username or password)";
        } else if (errorCode == 1049 || "42000".equals(sqlState) || lowerMessage.contains("unknown database")) {
            category = "Database not found";
        } else if (lowerMessage.contains("ssl") || lowerMessage.contains("tls") || lowerMessage.contains("handshake") || lowerMessage.contains("certificate") || lowerMessage.contains("pkix")) {
            category = "TLS / SSL configuration failure";
        } else if (lowerMessage.contains("no suitable driver")) {
            category = "JDBC driver or URL format failure (ensure URL starts with jdbc:mysql://)";
        } else if (errorCode == 1146 || "42S02".equals(sqlState) || lowerMessage.contains("doesn't exist")) {
            category = "Database schema / missing table failure";
        } else {
            category = "Database error (SQLState: " + sqlState + ", Code: " + errorCode + ")";
        }

        // Sanitize the underlying driver message as well
        String sanitizedReason = sanitizeUrl(message);
        return "Failed to establish database connection to " + sanitized + " [" + category + "]: " + sanitizedReason;
    }

    /** Validates that all tables and columns required by the current application are present. */
    public static void validateRequiredSchema(Connection connection) {
        String[][] requiredColumns = {
                {"users", "id", "name", "email", "password_hash", "role", "created_at"},
                {"categories", "id", "name"},
                {"locations", "id", "campus", "building", "room"},
                {"items", "id", "title", "description", "type", "status", "category_id", "location_id", "reporter_id", "reported_at", "image_path"},
                {"claims", "id", "user_id", "item_id", "message", "status", "submitted_at", "decided_at"},
                {"moderation", "id", "claim_id", "moderator_id", "decision", "comment", "decided_at"},
                {"notifications", "id", "user_id", "text", "read_flag", "created_at"}
        };
        try {
            DatabaseMetaData meta = connection.getMetaData();
            java.util.List<String> missing = new java.util.ArrayList<>();
            for (String[] definition : requiredColumns) {
                String table = definition[0];
                boolean tableExists;
                try (ResultSet tables = meta.getTables(connection.getCatalog(), null, table, new String[]{"TABLE"})) {
                    tableExists = tables.next();
                }
                if (!tableExists) {
                    missing.add("table " + table);
                    continue;
                }
                java.util.Set<String> actualColumns = new java.util.HashSet<>();
                try (ResultSet columns = meta.getColumns(connection.getCatalog(), null, table, null)) {
                    while (columns.next()) {
                        actualColumns.add(columns.getString("COLUMN_NAME").toLowerCase());
                    }
                }
                for (int i = 1; i < definition.length; i++) {
                    if (!actualColumns.contains(definition[i])) {
                        missing.add("column " + table + "." + definition[i]);
                    }
                }
            }
            if (!missing.isEmpty()) {
                throw new DatabaseException("Database schema is incomplete or incompatible (missing "
                        + String.join(", ", missing) + "). Apply database/schema.sql, or explicitly set "
                        + AUTO_SCHEMA_ENV + "=true only for a non-production development database.");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to validate database schema: " + e.getMessage(), e);
        }
    }

    /**
     * Creates missing schema objects using non-destructive CREATE IF NOT EXISTS
     * statements. Call only after explicit development/test opt-in.
     */
    public static void ensureSchemaInitialized(Connection connection) {
        try {
            String[] ddl = new String[]{
                    "CREATE TABLE IF NOT EXISTS users (" +
                    "    id INT AUTO_INCREMENT PRIMARY KEY," +
                    "    name VARCHAR(120) NOT NULL," +
                    "    email VARCHAR(255) NOT NULL UNIQUE," +
                    "    password_hash VARCHAR(255) NOT NULL," +
                    "    role VARCHAR(30) NOT NULL DEFAULT 'USER'," +
                    "    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                    ")",
                    "CREATE TABLE IF NOT EXISTS categories (" +
                    "    id INT AUTO_INCREMENT PRIMARY KEY," +
                    "    name VARCHAR(60) NOT NULL UNIQUE" +
                    ")",
                    "CREATE TABLE IF NOT EXISTS locations (" +
                    "    id INT AUTO_INCREMENT PRIMARY KEY," +
                    "    campus VARCHAR(120) NOT NULL," +
                    "    building VARCHAR(120)," +
                    "    room VARCHAR(120)" +
                    ")",
                    "CREATE TABLE IF NOT EXISTS items (" +
                    "    id INT AUTO_INCREMENT PRIMARY KEY," +
                    "    title VARCHAR(120) NOT NULL," +
                    "    description TEXT," +
                    "    type VARCHAR(10) NOT NULL," +
                    "    status VARCHAR(20) NOT NULL DEFAULT 'FOUND'," +
                    "    category_id INT NOT NULL," +
                    "    location_id INT," +
                    "    reporter_id INT NOT NULL," +
                    "    reported_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "    image_path VARCHAR(255)," +
                    "    CONSTRAINT fk_items_category FOREIGN KEY (category_id) REFERENCES categories(id)," +
                    "    CONSTRAINT fk_items_location FOREIGN KEY (location_id) REFERENCES locations(id)," +
                    "    CONSTRAINT fk_items_reporter FOREIGN KEY (reporter_id) REFERENCES users(id)," +
                    "    CONSTRAINT chk_items_type CHECK (type IN ('LOST', 'FOUND'))," +
                    "    CONSTRAINT chk_items_status CHECK (status IN ('OPEN', 'LOST', 'FOUND', 'CLAIMED', 'RETURNED', 'CLOSED'))" +
                    ")",
                    "CREATE TABLE IF NOT EXISTS claims (" +
                    "    id INT AUTO_INCREMENT PRIMARY KEY," +
                    "    user_id INT NOT NULL," +
                    "    item_id INT NOT NULL," +
                    "    message TEXT," +
                    "    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'," +
                    "    submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "    decided_at DATETIME," +
                    "    CONSTRAINT fk_claims_user FOREIGN KEY (user_id) REFERENCES users(id)," +
                    "    CONSTRAINT fk_claims_item FOREIGN KEY (item_id) REFERENCES items(id)" +
                    ")",
                    "CREATE TABLE IF NOT EXISTS moderation (" +
                    "    id INT AUTO_INCREMENT PRIMARY KEY," +
                    "    claim_id INT NOT NULL UNIQUE," +
                    "    moderator_id INT NOT NULL," +
                    "    decision VARCHAR(20) NOT NULL," +
                    "    comment TEXT," +
                    "    decided_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "    CONSTRAINT fk_moderation_claim FOREIGN KEY (claim_id) REFERENCES claims(id)," +
                    "    CONSTRAINT fk_moderation_moderator FOREIGN KEY (moderator_id) REFERENCES users(id)," +
                    "    CONSTRAINT chk_moderation_decision CHECK (decision IN ('APPROVED', 'REJECTED'))" +
                    ")",
                    "CREATE TABLE IF NOT EXISTS notifications (" +
                    "    id INT AUTO_INCREMENT PRIMARY KEY," +
                    "    user_id INT NOT NULL," +
                    "    text TEXT NOT NULL," +
                    "    read_flag BOOLEAN NOT NULL DEFAULT FALSE," +
                    "    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id)" +
                    ")",
                    "CREATE INDEX idx_items_status ON items(status)",
                    "CREATE INDEX idx_items_category ON items(category_id)",
                    "CREATE INDEX idx_claims_item ON claims(item_id)",
                    "CREATE INDEX idx_claims_user ON claims(user_id)",
                    "CREATE INDEX idx_items_status_cat ON items(status, category_id)"
                };

            try (Statement stmt = connection.createStatement()) {
                for (String sql : ddl) {
                    try {
                        stmt.execute(sql);
                    } catch (SQLException sqle) {
                        if (!sqle.getMessage().contains("Duplicate key name")) {
                            throw sqle;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to ensure database schema initialization: " + e.getMessage(), e);
        }
    }
}
