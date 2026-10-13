package com.refind.service.impl;

import com.refind.dao.UserDAO;
import com.refind.exception.ValidationException;
import com.refind.model.User;
import com.refind.model.enums.Role;
import com.refind.security.PasswordEncoder;
import com.refind.service.AuthService;

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
        if (name == null || name.isBlank()) {
            throw new ValidationException("Full name is required.");
        }
        name = name.trim();
        if (name.length() < 2 || name.length() > 120) {
            throw new ValidationException("Full name must be between 2 and 120 characters.");
        }

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

        if (dao.findByEmail(email).isPresent()) {
            throw new ValidationException("An account with this email address is already registered.");
        }

        String hashedPassword = PasswordEncoder.hash(password);
        // Public registration always assigns regular USER role
        User user = new User(name, email, hashedPassword, Role.USER, LocalDateTime.now());
        return dao.save(user);
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
