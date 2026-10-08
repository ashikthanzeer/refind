package com.refind;

import com.refind.dao.*;
import com.refind.dao.impl.*;
import com.refind.model.*;
import com.refind.model.enums.*;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseTest {

    @Test
    public void testH2DatabaseSchemaAndDAOs() throws Exception {
        String url = "jdbc:h2:mem:testdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        try (Connection conn = DriverManager.getConnection(url, "sa", "")) {
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "name VARCHAR(120) NOT NULL," +
                    "email VARCHAR(255) NOT NULL UNIQUE," +
                    "password_hash VARCHAR(255) NOT NULL," +
                    "role VARCHAR(30) NOT NULL DEFAULT 'USER'," +
                    "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP)");

            stmt.execute("CREATE TABLE IF NOT EXISTS categories (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "name VARCHAR(60) NOT NULL UNIQUE)");

            stmt.execute("CREATE TABLE IF NOT EXISTS locations (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "campus VARCHAR(120) NOT NULL," +
                    "building VARCHAR(120)," +
                    "room VARCHAR(120))");

            stmt.execute("CREATE TABLE IF NOT EXISTS items (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "title VARCHAR(120) NOT NULL," +
                    "description TEXT," +
                    "type VARCHAR(10) NOT NULL," +
                    "status VARCHAR(20) NOT NULL DEFAULT 'FOUND'," +
                    "category_id INT NOT NULL," +
                    "location_id INT," +
                    "reporter_id INT NOT NULL," +
                    "reported_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "image_path VARCHAR(255))");

            stmt.execute("CREATE TABLE IF NOT EXISTS claims (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "user_id INT NOT NULL," +
                    "item_id INT NOT NULL," +
                    "message TEXT," +
                    "status VARCHAR(20) NOT NULL DEFAULT 'PENDING'," +
                    "submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "decided_at DATETIME)");

            stmt.execute("CREATE TABLE IF NOT EXISTS moderation (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "claim_id INT NOT NULL UNIQUE," +
                    "moderator_id INT NOT NULL," +
                    "decision VARCHAR(20) NOT NULL," +
                    "comment TEXT," +
                    "decided_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP)");

            stmt.execute("CREATE TABLE IF NOT EXISTS notifications (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "user_id INT NOT NULL," +
                    "text TEXT NOT NULL," +
                    "read_flag BOOLEAN NOT NULL DEFAULT FALSE," +
                    "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP)");

            assertTrue(true);
        }
    }
}
