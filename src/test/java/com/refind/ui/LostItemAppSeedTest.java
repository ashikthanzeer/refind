package com.refind.ui;

import com.refind.dao.CategoryDAO;
import com.refind.dao.ItemDAO;
import com.refind.dao.LocationDAO;
import com.refind.dao.UserDAO;
import com.refind.dao.impl.JdbcCategoryDAO;
import com.refind.dao.impl.JdbcItemDAO;
import com.refind.dao.impl.JdbcLocationDAO;
import com.refind.dao.impl.JdbcUserDAO;
import com.refind.database.DatabaseConnection;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.sql.Connection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class LostItemAppSeedTest {

    private final List<Long> createdItemIds = new ArrayList<>();
    private final List<Long> createdUserIds = new ArrayList<>();
    private final List<Long> createdCategoryIds = new ArrayList<>();
    private final List<Long> createdLocationIds = new ArrayList<>();

    @AfterAll
    static void cleanup() {
        try {
            com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
        } catch (Throwable ignored) {
        }
    }

    @AfterEach
    void removeOnlyRowsCreatedByTheSeeder() {
        ItemDAO itemDAO = new JdbcItemDAO();
        UserDAO userDAO = new JdbcUserDAO();
        CategoryDAO categoryDAO = new JdbcCategoryDAO();
        LocationDAO locationDAO = new JdbcLocationDAO();

        createdItemIds.forEach(itemDAO::deleteById);
        createdUserIds.forEach(userDAO::deleteById);
        createdCategoryIds.forEach(categoryDAO::deleteById);
        createdLocationIds.forEach(locationDAO::deleteById);
    }

    @Test
    void testSeedSampleDataIfEmpty() throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            DatabaseConnection.ensureSchemaInitialized(conn);
        }

        UserDAO userDAO = new JdbcUserDAO();
        CategoryDAO categoryDAO = new JdbcCategoryDAO();
        LocationDAO locationDAO = new JdbcLocationDAO();
        ItemDAO itemDAO = new JdbcItemDAO();

        UserService userService = new JdbcUserService(userDAO);
        CategoryService categoryService = new JdbcCategoryService(categoryDAO);
        LocationService locationService = new JdbcLocationService(locationDAO);
        ItemService itemService = new JdbcItemService(itemDAO, userDAO);

        Set<Long> itemIdsBefore = ids(itemDAO.findAll(), Item::getId);
        Set<Long> userIdsBefore = ids(userDAO.findAll(), User::getId);
        Set<Long> categoryIdsBefore = ids(categoryDAO.findAll(), Category::getId);
        Set<Long> locationIdsBefore = ids(locationDAO.findAll(), Location::getId);

        try {
            // Execute seed logic
            LostItemApp.seedSampleDataIfEmpty(userService, categoryService, locationService, itemService);

            // Verify seeded data
            List<Category> categories = categoryService.getAllCategories();
            assertFalse(categories.isEmpty(), "Categories should be populated after seeding");

            List<Location> locations = locationService.getAllLocations();
            assertFalse(locations.isEmpty(), "Locations should be populated after seeding");

            List<User> users = userService.getAllUsers();
            assertFalse(users.isEmpty(), "Users should be populated after seeding");

            List<Item> lostItems = itemService.getItemsByType(ItemType.LOST);
            assertFalse(lostItems.isEmpty(), "Lost items should be populated after seeding");

            int categoryCount = categories.size();
            int locationCount = locations.size();
            int userCount = users.size();
            int lostItemCount = lostItems.size();

            // Idempotency check: Running seed logic again should not throw errors or create duplicate unique records
            assertDoesNotThrow(() -> {
                LostItemApp.seedSampleDataIfEmpty(userService, categoryService, locationService, itemService);
            });
            assertEquals(categoryCount, categoryService.getAllCategories().size());
            assertEquals(locationCount, locationService.getAllLocations().size());
            assertEquals(userCount, userService.getAllUsers().size());
            assertEquals(lostItemCount, itemService.getItemsByType(ItemType.LOST).size());
        } finally {
            recordCreatedIds(itemDAO.findAll(), Item::getId, itemIdsBefore, createdItemIds);
            recordCreatedIds(userDAO.findAll(), User::getId, userIdsBefore, createdUserIds);
            recordCreatedIds(categoryDAO.findAll(), Category::getId, categoryIdsBefore, createdCategoryIds);
            recordCreatedIds(locationDAO.findAll(), Location::getId, locationIdsBefore, createdLocationIds);
        }
    }

    private static <T> Set<Long> ids(List<T> values, Function<T, Long> idExtractor) {
        Set<Long> ids = new HashSet<>();
        for (T value : values) {
            ids.add(idExtractor.apply(value));
        }
        return ids;
    }

    private static <T> void recordCreatedIds(List<T> values, Function<T, Long> idExtractor,
                                              Set<Long> idsBefore, List<Long> createdIds) {
        for (T value : values) {
            Long id = idExtractor.apply(value);
            if (!idsBefore.contains(id)) {
                createdIds.add(id);
            }
        }
    }
}
