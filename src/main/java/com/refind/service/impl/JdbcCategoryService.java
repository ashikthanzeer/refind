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

    @Override
    public Category createCategory(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Category name is required.");
        }
        Category category = new Category(name.trim());
        return dao.save(category);
    }

    @Override
    public Optional<Category> getCategoryById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return dao.findById(id);
    }

    @Override
    public List<Category> getAllCategories() {
        return dao.findAll();
    }

    @Override
    public Category updateCategory(Category category) {
        if (category == null || category.getId() == null) {
            throw new ValidationException("Category ID is required for update.");
        }
        if (category.getName() == null || category.getName().isBlank()) {
            throw new ValidationException("Category name is required.");
        }
        return dao.update(category);
    }

    @Override
    public boolean deleteCategory(Long id) {
        if (id == null) {
            throw new ValidationException("Category ID is required for deletion.");
        }
        return dao.deleteById(id);
    }
}
