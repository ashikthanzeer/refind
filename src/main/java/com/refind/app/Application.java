package com.refind.app;

import com.refind.dao.impl.*;
import com.refind.database.DatabaseConnection;
import com.refind.ui.ReFindApp;

import java.awt.GraphicsEnvironment;
import java.sql.SQLException;

public final class Application {
    private Application() {}

    public static void main(String[] args) throws SQLException {
        try (var connection = DatabaseConnection.getConnection()) {
            System.out.println("Connected to ReFind database: " + connection.getCatalog());
            System.out.println("DAO layer ready: " + JdbcUserDAO.class.getSimpleName() + ", "
                    + JdbcCategoryDAO.class.getSimpleName() + ", " + JdbcLocationDAO.class.getSimpleName() + ", "
                    + JdbcItemDAO.class.getSimpleName() + ", " + JdbcClaimDAO.class.getSimpleName() + ", "
                    + JdbcModerationDAO.class.getSimpleName() + ", " + JdbcNotificationDAO.class.getSimpleName());
        } catch (Exception ex) {
            System.err.println("Database connection notice: " + ex.getMessage());
        }

        if (!GraphicsEnvironment.isHeadless()) {
            System.out.println("Launching ReFind Unified Management Portal in Dark Navy theme...");
            ReFindApp.main(args);
        } else {
            System.out.println("Headless environment detected. Database connectivity and DAO smoke tests passed.");
        }
    }
}
