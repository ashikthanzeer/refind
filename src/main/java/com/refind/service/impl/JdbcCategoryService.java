package com.refind.service.impl;

import com.refind.dao.CategoryDAO;
import com.refind.exception.ValidationException;
import com.refind.model.Category;
import com.refind.service.CategoryService;

import java.util.List;
import java.util.Optional;

public class JdbcCategoryService implements CategoryService {
    private final CategoryDAO dao;

    public JdbcCategoryService(CategoryDAO dao) {
        this.dao = dao;
    }

    public Category createCategory(Category category) {
        if (category == null || category.getName() == null || category.getName().isBlank()) {
            throw new ValidationException("Category name is required.");
        }
        return dao.save(category);
    }

    public Optional<Category> getCategoryById(Long id) {
        return dao.findById(id);
    }

    public List<Category> getAllCategories() {
        return dao.findAll();
    }

    public Category updateCategory(Category category) {
        if (category == null || category.getId() == null) {
            throw new ValidationException("Category id is required for update.");
        }
        if (category.getName() == null || category.getName().isBlank()) {
            throw new ValidationException("Category name is required.");
        }
        return dao.update(category);
    }

    public boolean deleteCategory(Long id) {
        return dao.deleteById(id);
    }
}
