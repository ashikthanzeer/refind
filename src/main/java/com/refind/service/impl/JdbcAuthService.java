package com.refind.service.impl;

import com.refind.dao.UserDAO;
import com.refind.exception.ValidationException;
import com.refind.model.User;
import com.refind.model.enums.Role;
import com.refind.service.AuthService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;

public class JdbcAuthService implements AuthService {

    private final UserDAO dao;

    public JdbcAuthService(UserDAO dao) {
        this.dao = dao;
    }

    @Override
    public User register(String name, String email, String password) {
        if (name == null || name.isBlank() || email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new ValidationException("Name, email and password are required.");
        }
        String cleanEmail = email.trim().toLowerCase();
        if (dao.findByEmail(cleanEmail).isPresent()) {
            throw new ValidationException("Email is already registered.");
        }
        User user = new User(name.trim(), cleanEmail, hash(password), Role.USER, LocalDateTime.now());
        return dao.save(user);
    }

    @Override
    public Optional<User> login(String email, String password) {
        if (email == null || password == null || email.isBlank() || password.isBlank()) {
            return Optional.empty();
        }
        String cleanEmail = email.trim().toLowerCase();
        return dao.findByEmail(cleanEmail).filter(u ->
                u.getPasswordHash() != null && MessageDigest.isEqual(
                        u.getPasswordHash().getBytes(StandardCharsets.UTF_8),
                        hash(password).getBytes(StandardCharsets.UTF_8)
                )
        );
    }

    @Override
    public boolean emailExists(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return dao.findByEmail(email.trim().toLowerCase()).isPresent();
    }

    @Override
    public void logout() {
        // Stateless service; session logout managed in SessionContext
    }

    public static String hash(String s) {
        if (s == null) {
            return "";
        }
        try {
            byte[] b = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder x = new StringBuilder();
            for (byte v : b) {
                x.append(String.format("%02x", v));
            }
            return x.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
