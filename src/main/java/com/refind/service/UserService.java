package com.refind.service;

import com.refind.model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    Optional<User> getUserById(Long id);

    Optional<User> getUserByEmail(String email);

    List<User> getAllUsers();

    User updateUser(User user);

    boolean deleteUser(Long id);
}
