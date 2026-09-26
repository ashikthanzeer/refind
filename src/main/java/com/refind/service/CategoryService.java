package com.refind.service;

import com.refind.model.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryService {
    Category createCategory(String name);
    Optional<Category> getCategoryById(Long id);
    List<Category> getAllCategories();
    Category updateCategory(Category category);
    boolean deleteCategory(Long id);
}
