package com.refind.dao;

import com.refind.model.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryDAO {

    Category save(Category category);

    Optional<Category> findById(Long id);

    List<Category> findAll();

    Category update(Category category);

    boolean deleteById(Long id);
}
