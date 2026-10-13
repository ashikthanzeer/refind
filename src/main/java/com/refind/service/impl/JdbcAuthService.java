package com.refind.service.impl;

import com.refind.dao.UserDAO;
import com.refind.dao.impl.JdbcUserDAO;
import com.refind.database.DatabaseConnection;
import com.refind.exception.DatabaseException;
import com.refind.exception.ValidationException;
import com.refind.model.User;
import com.refind.model.enums.Role;
import com.refind.security.PasswordEncoder;
import com.refind.service.AuthService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

public class JdbcAuthService implements AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UserDAO dao;

    public JdbcAuthService(UserDAO dao) {
        this.dao = dao;
    }

    @Override
    public User register(String name, String email, String password) {
        return register(name, email, password, password);
    }

    @Override
    public User register(String name, String email, String password, String confirmPassword) {
        String cleanedName = normalizeName(name);
        String cleanedEmail = normalizeEmail(email);
        validatePassword(password, confirmPassword);
        if (dao.findByEmail(cleanedEmail).isPresent()) {
            throw new ValidationException("An account with this email address is already registered.");
        }

        User user = new User(cleanedName, cleanedEmail, PasswordEncoder.hash(password), Role.USER, LocalDateTime.now());
        return dao.save(user);
    }

    @Override
    public User bootstrapFirstAdmin(String name, String email, String password) {
        return bootstrapFirstAdmin(name, email, password, password);
    }

    @Override
    public User bootstrapFirstAdmin(String name, String email, String password, String confirmPassword) {
        String cleanedName = normalizeName(name);
        String cleanedEmail = normalizeEmail(email);
        validatePassword(password, confirmPassword);

        if (!(dao instanceof JdbcUserDAO)) {
            if (hasAdministrator()) {
                throw new ValidationException("An administrator already exists. Bootstrap is only available when no administrator has been created.");
            }
            if (dao.findByEmail(cleanedEmail).isPresent()) {
                throw new ValidationException("An account with this email address is already registered.");
            }
            User user = new User(cleanedName, cleanedEmail, PasswordEncoder.hash(password), Role.ADMIN, LocalDateTime.now());
            return dao.save(user);
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            ensureBootstrapLockTable(conn);
            conn.setAutoCommit(false);
            try {
                lockBootstrapRow(conn);
                if (hasAdministrator(conn)) {
                    throw new ValidationException("An administrator already exists. Bootstrap is only available when no administrator has been created.");
                }
                if (dao.findByEmail(cleanedEmail).isPresent()) {
                    throw new ValidationException("An account with this email address is already registered.");
                }

                User user = new User(cleanedName, cleanedEmail, PasswordEncoder.hash(password), Role.ADMIN, LocalDateTime.now());
                User saved = dao.save(user);
                conn.commit();
                return saved;
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to create the first administrator: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean hasAdministrator() {
        return dao.findAll().stream().anyMatch(user -> user != null && user.getRole() == Role.ADMIN);
    }

    private boolean hasAdministrator(Connection conn) throws SQLException {
        String sql = "SELECT id FROM users WHERE role = 'ADMIN' LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void ensureBootstrapLockTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS admin_bootstrap_lock (id INT PRIMARY KEY, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            stmt.execute("INSERT IGNORE INTO admin_bootstrap_lock (id, created_at) VALUES (1, NOW())");
        }
    }

    private void lockBootstrapRow(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT id FROM admin_bootstrap_lock WHERE id = 1 FOR UPDATE")) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new ValidationException("Administrator bootstrap lock is unavailable.");
                }
            }
        }
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Full name is required.");
        }
        name = name.trim();
        if (name.length() < 2 || name.length() > 120) {
            throw new ValidationException("Full name must be between 2 and 120 characters.");
        }
        return name;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ValidationException("Email address is required.");
        }
        email = email.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException("Please provide a valid email address.");
        }
        if (email.length() > 255) {
            throw new ValidationException("Email address cannot exceed 255 characters.");
        }
        return email;
    }

    private void validatePassword(String password, String confirmPassword) {
        if (password == null || password.isBlank()) {
            throw new ValidationException("Password is required.");
        }
        if (password.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters long.");
        }
        if (password.length() > 100) {
            throw new ValidationException("Password cannot exceed 100 characters.");
        }
        if (confirmPassword != null && !password.equals(confirmPassword)) {
            throw new ValidationException("Password and confirmation password do not match.");
        }
    }

    @Override
    public Optional<User> login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }

        String cleanEmail = email.trim().toLowerCase();
        Optional<User> userOpt = dao.findByEmail(cleanEmail);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();
        if (!PasswordEncoder.verify(password, user.getPasswordHash())) {
            return Optional.empty();
        }

        // Seamlessly upgrade legacy/plaintext hashes to BCrypt on successful login
        if (PasswordEncoder.isLegacyHash(user.getPasswordHash())) {
            try {
                user.setPasswordHash(PasswordEncoder.hash(password));
                dao.update(user);
            } catch (Exception ignored) {
                // Non-fatal if hash upgrade fails
            }
        }

        return Optional.of(user);
    }

    @Override
    public boolean emailExists(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return dao.findByEmail(email.trim().toLowerCase()).isPresent();
    }

    @Override
    public User updateProfile(Long userId, String newName, String currentPassword, String newPassword) {
        if (userId == null) {
            throw new ValidationException("User identifier is required.");
        }

        User user = dao.findById(userId)
                .orElseThrow(() -> new ValidationException("User account not found: #" + userId));

        if (newName != null && !newName.isBlank()) {
            String cleanName = newName.trim();
            if (cleanName.length() < 2 || cleanName.length() > 120) {
                throw new ValidationException("Full name must be between 2 and 120 characters.");
            }
            user.setName(cleanName);
        }

        if (newPassword != null && !newPassword.isBlank()) {
            if (currentPassword == null || !PasswordEncoder.verify(currentPassword, user.getPasswordHash())) {
                throw new ValidationException("Current password verification failed.");
            }
            if (newPassword.length() < 6) {
                throw new ValidationException("New password must be at least 6 characters long.");
            }
            if (newPassword.length() > 100) {
                throw new ValidationException("New password cannot exceed 100 characters.");
            }
            user.setPasswordHash(PasswordEncoder.hash(newPassword));
        }

        // Role and email are strictly preserved and cannot be tampered via profile update
        return dao.update(user);
    }

    @Override
    public void logout() {
        // UI layer clears session context
    }
}
