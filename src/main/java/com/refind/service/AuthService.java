package com.refind.service;

import com.refind.model.User;

import java.util.Optional;

public interface AuthService {

    User register(String name, String email, String password);

    User register(String name, String email, String password, String confirmPassword);

    Optional<User> login(String email, String password);

    boolean emailExists(String email);

    User updateProfile(Long userId, String newName, String currentPassword, String newPassword);

    void logout();
}
