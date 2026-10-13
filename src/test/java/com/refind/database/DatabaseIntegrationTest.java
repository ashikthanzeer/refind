package com.refind.database;

import com.refind.dao.CategoryDAO;
import com.refind.dao.ItemDAO;
import com.refind.dao.LocationDAO;
import com.refind.dao.UserDAO;
import com.refind.dao.impl.JdbcCategoryDAO;
import com.refind.dao.impl.JdbcItemDAO;
import com.refind.dao.impl.JdbcLocationDAO;
import com.refind.dao.impl.JdbcUserDAO;
import com.refind.model.Category;
import com.refind.model.Item;
import com.refind.model.Location;
import com.refind.model.User;
import com.refind.model.enums.ItemType;
import com.refind.service.CategoryService;
import com.refind.service.ItemService;
import com.refind.service.LocationService;
import com.refind.service.UserService;
import com.refind.service.impl.JdbcCategoryService;
import com.refind.service.impl.JdbcItemService;
import com.refind.service.impl.JdbcLocationService;
import com.refind.service.impl.JdbcUserService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class DatabaseIntegrationTest {

    @AfterAll
    static void cleanup() {
        try {
            com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
        } catch (Throwable ignored) {
        }
    }

    @Test
    void testRealDatabaseConnectionAndVersionQuery() throws Exception {
        try (Connection connection = DatabaseConnection.getConnection()) {
            assertNotNull(connection, "Connection should not be null");
            assertFalse(connection.isClosed(), "Connection should be open");
            assertTrue(connection.getCatalog().matches("(?i)(test|test_[a-z0-9_]+|[a-z0-9_]+_test)"),
                    "Integration tests must run against the dedicated test database");

            try (Statement statement = connection.createStatement()) {
                try (ResultSet rs = statement.executeQuery("SELECT VERSION() AS version, DATABASE() AS database_name")) {
                    assertTrue(rs.next(), "Query should return at least one row");
                    String version = rs.getString("version");
                    String dbName = rs.getString("database_name");

                    assertNotNull(version, "MySQL version should not be null");
                    assertEquals(connection.getCatalog(), dbName, "Query should run against the configured test database");
                    System.out.println("Integration Test: MySQL Version = " + version + ", Database = " + dbName);
                }
            }
        }
    }

    @Test
    void testSchemaTablesAndDaoQueries() {
        try (Connection connection = DatabaseConnection.getConnection()) {
            DatabaseConnection.ensureSchemaInitialized(connection);
        } catch (Exception e) {
            fail("Could not initialize the dedicated integration-test schema: " + e.getMessage());
        }

        UserDAO userDAO = new JdbcUserDAO();
        CategoryDAO categoryDAO = new JdbcCategoryDAO();
        LocationDAO locationDAO = new JdbcLocationDAO();
        ItemDAO itemDAO = new JdbcItemDAO();

        UserService userService = new JdbcUserService(userDAO);
        CategoryService categoryService = new JdbcCategoryService(categoryDAO);
        LocationService locationService = new JdbcLocationService(locationDAO);
        ItemService itemService = new JdbcItemService(itemDAO, userDAO);

        // Ensure DAOs execute queries against tables in the dedicated test database without error.
        assertDoesNotThrow(() -> {
            List<Category> categories = categoryService.getAllCategories();
            List<Location> locations = locationService.getAllLocations();
            List<User> users = userService.getAllUsers();
            List<Item> lostItems = itemService.getItemsByType(ItemType.LOST);

            System.out.println("Integration Test: Found categories=" + categories.size()
                    + ", locations=" + locations.size()
                    + ", users=" + users.size()
                    + ", lostItems=" + lostItems.size());
        });
    }
}
