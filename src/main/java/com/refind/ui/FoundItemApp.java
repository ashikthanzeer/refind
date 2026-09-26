package com.refind.ui;

import com.refind.dao.CategoryDAO;
import com.refind.dao.ItemDAO;
import com.refind.dao.LocationDAO;
import com.refind.dao.UserDAO;
import com.refind.dao.impl.JdbcCategoryDAO;
import com.refind.dao.impl.JdbcItemDAO;
import com.refind.dao.impl.JdbcLocationDAO;
import com.refind.dao.impl.JdbcUserDAO;
import com.refind.service.CategoryService;
import com.refind.service.ItemService;
import com.refind.service.LocationService;
import com.refind.service.UserService;
import com.refind.service.impl.JdbcCategoryService;
import com.refind.service.impl.JdbcItemService;
import com.refind.service.impl.JdbcLocationService;
import com.refind.service.impl.JdbcUserService;

import javax.swing.*;

public class FoundItemApp {

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

                // Initialize Session Context
                SessionContext sessionContext = new SessionContext(userService);

                // Launch Found Item Window
                FoundItemFrame frame = new FoundItemFrame(itemService, categoryService, locationService, sessionContext);
                frame.setVisible(true);

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Failed to start ReFind Found Item Portal:\n" + ex.getMessage() + "\n\n"
                                + "Please check database connection settings in database.properties or environment variables.",
                        "Startup Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
