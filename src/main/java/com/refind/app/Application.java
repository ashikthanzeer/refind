package com.refind.app;

import com.refind.dao.impl.JdbcCategoryDAO;
import com.refind.dao.impl.JdbcClaimDAO;
import com.refind.dao.impl.JdbcItemDAO;
import com.refind.dao.impl.JdbcLocationDAO;
import com.refind.dao.impl.JdbcModerationDAO;
import com.refind.dao.impl.JdbcNotificationDAO;
import com.refind.dao.impl.JdbcUserDAO;
import com.refind.database.DatabaseConnection;
import com.refind.ui.LostItemApp;

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
            System.err.println("Database connection warning: " + ex.getMessage());
        }

        if (!GraphicsEnvironment.isHeadless()) {
            System.out.println("Launching ReFind Lost Item Management screens...");
            LostItemApp.main(args);
        }
    }
}
