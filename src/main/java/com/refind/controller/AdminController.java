package com.refind.controller;

import com.refind.exception.ValidationException;
import com.refind.model.Category;
import com.refind.model.Claim;
import com.refind.model.Item;
import com.refind.model.Location;
import com.refind.model.User;
import com.refind.model.enums.ClaimStatus;
import com.refind.model.enums.ItemType;
import com.refind.model.enums.Role;
import com.refind.service.CategoryService;
import com.refind.service.ClaimService;
import com.refind.service.ItemService;
import com.refind.service.LocationService;
import com.refind.service.ModerationService;
import com.refind.service.UserService;

import java.util.Collections;
import java.util.List;

/**
 * Controller for administrator actions.
 * Keeps role checks and UI-independent admin operations outside the Swing view.
 */
public class AdminController {

    public record DashboardMetrics(
            long totalLostItems,
            long totalFoundItems,
            long pendingClaims,
            long totalUsers,
            long totalClaims
    ) {}

    private final UserService userService;
    private final CategoryService categoryService;
    private final LocationService locationService;
    private final ItemService itemService;
    private final ClaimService claimService;
    private final ModerationService moderationService;

    public AdminController(UserService userService,
                           CategoryService categoryService,
                           LocationService locationService) {
        this(userService, categoryService, locationService, null, null, null);
    }

    public AdminController(UserService userService,
                           CategoryService categoryService,
                           LocationService locationService,
                           ItemService itemService,
                           ClaimService claimService,
                           ModerationService moderationService) {
        this.userService = userService;
        this.categoryService = categoryService;
        this.locationService = locationService;
        this.itemService = itemService;
        this.claimService = claimService;
        this.moderationService = moderationService;
    }

    public void requireAdmin(User currentUser) {
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
            throw new ValidationException("Administrator access is required.");
        }
    }

    public DashboardMetrics getDashboardMetrics(User currentUser) {
        requireAdmin(currentUser);

        long lostCount = 0;
        long foundCount = 0;
        if (itemService != null) {
            try {
                lostCount = itemService.getItemsByType(ItemType.LOST).size();
                foundCount = itemService.getItemsByType(ItemType.FOUND).size();
            } catch (Exception ignored) {}
        }

        long pendingClaimsCount = 0;
        long totalClaimsCount = 0;
        if (claimService != null) {
            try {
                pendingClaimsCount = claimService.getClaimsByStatus(ClaimStatus.PENDING).size();
                totalClaimsCount = claimService.getAllClaims().size();
            } catch (Exception ignored) {}
        }

        long usersCount = 0;
        if (userService != null) {
            try {
                usersCount = userService.getAllUsers().size();
            } catch (Exception ignored) {}
        }

        return new DashboardMetrics(lostCount, foundCount, pendingClaimsCount, usersCount, totalClaimsCount);
    }

    public List<User> getUsers(User currentUser) {
        requireAdmin(currentUser);
        return userService != null ? userService.getAllUsers() : Collections.emptyList();
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
        return categoryService != null ? categoryService.getAllCategories() : Collections.emptyList();
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
        return locationService != null ? locationService.getAllLocations() : Collections.emptyList();
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

    public List<Claim> getClaims(User currentUser) {
        requireAdmin(currentUser);
        return claimService != null ? claimService.getAllClaims() : Collections.emptyList();
    }

    public List<Claim> getPendingClaims(User currentUser) {
        requireAdmin(currentUser);
        if (moderationService != null) {
            return moderationService.getPendingClaims();
        }
        if (claimService != null) {
            return claimService.getClaimsByStatus(ClaimStatus.PENDING);
        }
        return Collections.emptyList();
    }

    public Claim approveClaim(User currentUser, Long claimId, String comment) {
        requireAdmin(currentUser);
        if (moderationService == null) {
            throw new ValidationException("Moderation service unavailable.");
        }
        return moderationService.approveClaim(claimId, currentUser.getId(), comment);
    }

    public Claim rejectClaim(User currentUser, Long claimId, String comment) {
        requireAdmin(currentUser);
        if (moderationService == null) {
            throw new ValidationException("Moderation service unavailable.");
        }
        return moderationService.rejectClaim(claimId, currentUser.getId(), comment);
    }

    public List<Item> getAllItems(User currentUser) {
        requireAdmin(currentUser);
        return itemService != null ? itemService.getAllItems() : Collections.emptyList();
    }

    public boolean deleteItem(User currentUser, Long itemId) {
        requireAdmin(currentUser);
        if (itemService == null) {
            throw new ValidationException("Item service unavailable.");
        }
        return itemService.deleteItem(itemId, currentUser);
    }
}
