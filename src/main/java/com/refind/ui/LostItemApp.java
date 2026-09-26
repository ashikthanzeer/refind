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
import com.refind.model.enums.ItemStatus;
import com.refind.model.enums.ItemType;
import com.refind.model.enums.Role;
import com.refind.service.CategoryService;
import com.refind.service.ItemService;
import com.refind.service.LocationService;
import com.refind.service.UserService;
import com.refind.service.impl.JdbcCategoryService;
import com.refind.service.impl.JdbcItemService;
import com.refind.service.impl.JdbcLocationService;
import com.refind.service.impl.JdbcUserService;

import javax.swing.*;
import java.time.LocalDateTime;
import java.util.List;

public class LostItemApp {

    public static void main(String[] args) {
        // Set System Look & Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            try {
                // Initialize Data Access Objects
                UserDAO userDAO = new JdbcUserDAO();
                CategoryDAO categoryDAO = new JdbcCategoryDAO();
                LocationDAO locationDAO = new JdbcLocationDAO();
                ItemDAO itemDAO = new JdbcItemDAO();

                // Initialize Services
                UserService userService = new JdbcUserService(userDAO);
                CategoryService categoryService = new JdbcCategoryService(categoryDAO);
                LocationService locationService = new JdbcLocationService(locationDAO);
                ItemService itemService = new JdbcItemService(itemDAO);

                // Auto-seed sample test records if database tables are empty
                seedSampleDataIfEmpty(userService, categoryService, locationService, itemService);

                // Initialize Session Context
                SessionContext sessionContext = new SessionContext(userService);

                // Launch Lost Item Window
                LostItemFrame frame = new LostItemFrame(itemService, categoryService, locationService, sessionContext);
                frame.setVisible(true);

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Failed to start ReFind Lost Item Portal:\n" + ex.getMessage() + "\n\n"
                                + "Please check database connection settings in database.properties or environment variables.",
                        "Startup Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static void seedSampleDataIfEmpty(UserService userService, CategoryService categoryService,
                                              LocationService locationService, ItemService itemService) {
        try {
            // Seed Categories if none exist
            List<Category> categories = categoryService.getAllCategories();
            Category electronics = null;
            Category wallets = null;
            Category docs = null;

            if (categories.isEmpty()) {
                electronics = categoryService.createCategory("Electronics");
                wallets = categoryService.createCategory("Wallets & Keys");
                docs = categoryService.createCategory("Documents & Cards");
                categoryService.createCategory("Books & Stationery");
                categoryService.createCategory("Clothing & Bags");
            } else {
                electronics = categories.get(0);
                if (categories.size() > 1) wallets = categories.get(1);
            }

            // Seed Locations if none exist
            List<Location> locations = locationService.getAllLocations();
            Location engBuilding = null;
            Location library = null;
            if (locations.isEmpty()) {
                engBuilding = locationService.createLocation("Main Campus", "Engineering Block", "302");
                library = locationService.createLocation("Main Campus", "Central Library", "Reading Hall");
                locationService.createLocation("North Campus", "Student Cafeteria", "Counter 2");
            } else {
                engBuilding = locations.get(0);
                if (locations.size() > 1) library = locations.get(1);
            }

            // Seed Users if none exist
            List<User> users = userService.getAllUsers();
            User john = null;
            User alice = null;
            User admin = null;
            if (users.isEmpty()) {
                UserDAO userDAO = new JdbcUserDAO();
                john = userDAO.save(new User("John Doe", "john@campus.edu", "sha256hash", Role.USER, LocalDateTime.now()));
                alice = userDAO.save(new User("Alice Smith", "alice@campus.edu", "sha256hash", Role.USER, LocalDateTime.now()));
                admin = userDAO.save(new User("Campus Security Officer", "admin@campus.edu", "sha256hash", Role.ADMIN, LocalDateTime.now()));
            } else {
                john = users.get(0);
                if (users.size() > 1) alice = users.get(1);
            }

            // Seed sample Lost Items if none exist
            List<Item> lostItems = itemService.getItemsByType(ItemType.LOST);
            if (lostItems.isEmpty() && john != null && electronics != null) {
                Item item1 = new Item();
                item1.setTitle("Lost Black Dell Laptop Charger");
                item1.setDescription("Left behind a 65W USB-C Dell laptop power adapter near row 4 desk.");
                item1.setType(ItemType.LOST);
                item1.setStatus(ItemStatus.OPEN);
                item1.setCategory(electronics);
                item1.setLocation(engBuilding);
                item1.setReportedBy(john);
                item1.setReportedAt(LocalDateTime.now().minusHours(3));
                itemService.reportItem(item1);

                if (alice != null && wallets != null) {
                    Item item2 = new Item();
                    item2.setTitle("Brown Leather Wallet");
                    item2.setDescription("Contains university ID card, driving license and small amount of cash.");
                    item2.setType(ItemType.LOST);
                    item2.setStatus(ItemStatus.OPEN);
                    item2.setCategory(wallets);
                    item2.setLocation(library);
                    item2.setReportedBy(alice);
                    item2.setReportedAt(LocalDateTime.now().minusHours(6));
                    itemService.reportItem(item2);
                }
            }
        } catch (Exception ex) {
            // Seeder failure is non-fatal (e.g. database already pre-populated or read-only)
            System.err.println("Database sample seed notice: " + ex.getMessage());
        }
    }
}
