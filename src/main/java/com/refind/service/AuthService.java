package com.refind.service;

import com.refind.model.User;

import java.util.Optional;

public interface AuthService {

    User register(String name, String email, String password);

    Optional<User> login(String email, String password);

    boolean emailExists(String email);

    void logout();
}
