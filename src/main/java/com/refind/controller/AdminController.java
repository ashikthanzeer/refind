package com.refind.controller;

import com.refind.exception.ValidationException;
import com.refind.model.Category;
import com.refind.model.Location;
import com.refind.model.User;
import com.refind.model.enums.Role;
import com.refind.service.CategoryService;
import com.refind.service.LocationService;
import com.refind.service.UserService;

import java.util.List;

/**
 * Controller for administrator actions.
 * Keeps role checks and UI-independent admin operations outside the Swing view.
 */
public class AdminController {

    private final UserService userService;
    private final CategoryService categoryService;
    private final LocationService locationService;

    public AdminController(UserService userService,
                           CategoryService categoryService,
                           LocationService locationService) {
        this.userService = userService;
        this.categoryService = categoryService;
        this.locationService = locationService;
    }

    public void requireAdmin(User currentUser) {
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
            throw new ValidationException("Administrator access is required.");
        }
    }

    public List<User> getUsers(User currentUser) {
        requireAdmin(currentUser);
        return userService.getAllUsers();
    }

    public User updateUser(User currentUser, User user) {
        requireAdmin(currentUser);
        return userService.updateUser(user);
    }

    public boolean deleteUser(User currentUser, Long id) {
        requireAdmin(currentUser);
        if (id != null && id.equals(currentUser.getId())) {
            throw new ValidationException("You cannot delete the currently logged-in administrator.");
        }
        return userService.deleteUser(id);
    }

    public List<Category> getCategories(User currentUser) {
        requireAdmin(currentUser);
        return categoryService.getAllCategories();
    }

    public Category createCategory(User currentUser, String name) {
        requireAdmin(currentUser);
        return categoryService.createCategory(name);
    }

    public Category updateCategory(User currentUser, Category category) {
        requireAdmin(currentUser);
        return categoryService.updateCategory(category);
    }

    public boolean deleteCategory(User currentUser, Long id) {
        requireAdmin(currentUser);
        return categoryService.deleteCategory(id);
    }

    public List<Location> getLocations(User currentUser) {
        requireAdmin(currentUser);
        return locationService.getAllLocations();
    }

    public Location createLocation(User currentUser, String campus, String building, String room) {
        requireAdmin(currentUser);
        return locationService.createLocation(campus, building, room);
    }

    public Location updateLocation(User currentUser, Location location) {
        requireAdmin(currentUser);
        return locationService.updateLocation(location);
    }

    public boolean deleteLocation(User currentUser, Long id) {
        requireAdmin(currentUser);
        return locationService.deleteLocation(id);
    }
}
