package com.refind.dao;

import com.refind.model.User;

import java.util.List;
import java.util.Optional;

public interface UserDAO {

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    List<User> findAll();

    User update(User user);

    boolean deleteById(Long id);
}
