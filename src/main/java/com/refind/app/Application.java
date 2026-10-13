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

public final class Application {
    private Application() {}

    public static void main(String[] args) {
        if (!GraphicsEnvironment.isHeadless()) {
            System.out.println("Launching ReFind Lost Item Management screens...");
            LostItemApp.main(args);
        } else {
            try (var connection = DatabaseConnection.getConnection()) {
                DatabaseConnection.prepareApplicationDatabase(connection);
                System.out.println("Connected to ReFind database: " + connection.getCatalog());
                System.out.println("DAO layer ready: " + JdbcUserDAO.class.getSimpleName() + ", "
                        + JdbcCategoryDAO.class.getSimpleName() + ", " + JdbcLocationDAO.class.getSimpleName() + ", "
                        + JdbcItemDAO.class.getSimpleName() + ", " + JdbcClaimDAO.class.getSimpleName() + ", "
                        + JdbcModerationDAO.class.getSimpleName() + ", " + JdbcNotificationDAO.class.getSimpleName());
            } catch (Exception ex) {
                System.err.println("Database startup check failed: " + ex.getMessage());
            }
            com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
        }
    }
}
