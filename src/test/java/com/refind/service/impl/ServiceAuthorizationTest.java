package com.refind.service.impl;

import com.refind.dao.ClaimDAO;
import com.refind.dao.ItemDAO;
import com.refind.dao.UserDAO;
import com.refind.exception.ValidationException;
import com.refind.model.Category;
import com.refind.model.Claim;
import com.refind.model.Item;
import com.refind.model.User;
import com.refind.model.enums.ClaimStatus;
import com.refind.model.enums.ItemStatus;
import com.refind.model.enums.ItemType;
import com.refind.model.enums.Role;
import com.refind.security.PasswordEncoder;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceAuthorizationTest {

    @Test
    void onlyTheClaimantCanWithdrawAPendingClaim() {
        User claimant = user(1L, Role.USER);
        Claim claim = new Claim();
        claim.setId(10L);
        claim.setClaimant(claimant);
        claim.setStatus(ClaimStatus.PENDING);
        InMemoryClaimDao claims = new InMemoryClaimDao(claim);
        JdbcClaimService service = new JdbcClaimService(claims);

        assertThrows(ValidationException.class, () -> service.withdrawClaim(10L, user(2L, Role.USER)));
        assertFalse(claims.deleted);
        assertThrows(ValidationException.class, () -> service.deleteClaim(10L));
    }

    @Test
    void nonAdminCannotApproveClaimAtServiceLayer() {
        User regularUser = user(7L, Role.USER);
        UserDAO users = new UserDAO() {
            @Override public Optional<User> findById(Long id) { return Optional.ofNullable(id.equals(7L) ? regularUser : null); }
            @Override public Optional<User> findByEmail(String email) { return Optional.empty(); }
            @Override public List<User> findAll() { return List.of(); }
            @Override public User save(User user) { return user; }
            @Override public User update(User user) { return user; }
            @Override public boolean deleteById(Long id) { return false; }
        };

        JdbcModerationService service = new JdbcModerationService(new InMemoryClaimDao(null), null, null, null, users);
        assertThrows(ValidationException.class, () -> service.approveClaim(10L, regularUser.getId()));
    }

    @Test
    void ownerCanUpdateOwnItemAndReporterIsPreserved() {
        User owner = user(1L, Role.USER);
        Item persisted = item(20L, owner);
        InMemoryItemDao items = new InMemoryItemDao(persisted);
        JdbcItemService service = new JdbcItemService(items, new InMemoryUserDao(owner));
        Item edited = item(20L, owner);
        edited.setTitle("Updated title");

        Item result = service.updateItem(edited, owner);

        assertSame(edited, result);
        assertTrue(items.updated);
        assertEquals("Updated title", items.stored.getTitle());
        assertEquals(owner.getId(), items.stored.getReportedBy().getId());
    }

    @Test
    void userCannotUpdateAnotherUsersItem() {
        User owner = user(1L, Role.USER);
        User otherUser = user(2L, Role.USER);
        InMemoryItemDao items = new InMemoryItemDao(item(20L, owner));
        JdbcItemService service = new JdbcItemService(items, new InMemoryUserDao(owner, otherUser));

        assertThrows(ValidationException.class, () -> service.updateItem(item(20L, owner), otherUser));
        assertFalse(items.updated);
    }

    @Test
    void ownerCannotForgeReporterIdDuringUpdate() {
        User owner = user(1L, Role.USER);
        User forgedReporter = user(2L, Role.USER);
        InMemoryItemDao items = new InMemoryItemDao(item(20L, owner));
        JdbcItemService service = new JdbcItemService(items, new InMemoryUserDao(owner, forgedReporter));
        Item edited = item(20L, forgedReporter);

        service.updateItem(edited, owner);

        assertEquals(owner.getId(), items.stored.getReportedBy().getId());
    }

    @Test
    void unauthenticatedUserCannotUpdateItem() {
        User owner = user(1L, Role.USER);
        InMemoryItemDao items = new InMemoryItemDao(item(20L, owner));
        JdbcItemService service = new JdbcItemService(items, new InMemoryUserDao(owner));

        assertThrows(ValidationException.class, () -> service.updateItem(item(20L, owner), null));
        assertFalse(items.updated);
    }

    @Test
    void administratorCanUpdateAnyItemUsingStoredRole() {
        User owner = user(1L, Role.USER);
        User storedAdministrator = user(9L, Role.ADMIN);
        User sessionActorWithForgedRole = user(9L, Role.USER);
        InMemoryItemDao items = new InMemoryItemDao(item(20L, owner));
        JdbcItemService service = new JdbcItemService(items, new InMemoryUserDao(owner, storedAdministrator));

        service.updateItem(item(20L, owner), sessionActorWithForgedRole);

        assertTrue(items.updated);
    }

    @Test
    void firstAdminCanBeCreatedWhenNoAdminExists() {
        InMemoryUserDao users = new InMemoryUserDao();
        JdbcAuthService authService = new JdbcAuthService(users);

        User created = authService.bootstrapFirstAdmin("Root Admin", "admin@campus.edu", "StrongPass123");

        assertEquals(Role.ADMIN, created.getRole());
        assertTrue(PasswordEncoder.verify("StrongPass123", created.getPasswordHash()));
        assertNotEquals("StrongPass123", created.getPasswordHash());
        assertTrue(authService.hasAdministrator());
        assertTrue(authService.login("admin@campus.edu", "StrongPass123").isPresent());
    }

    @Test
    void bootstrapRejectsAdminWhenAdministratorAlreadyExists() {
        User existingAdmin = user(100L, Role.ADMIN);
        InMemoryUserDao users = new InMemoryUserDao(existingAdmin);
        JdbcAuthService authService = new JdbcAuthService(users);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> authService.bootstrapFirstAdmin("Another Admin", "another@campus.edu", "StrongPass123"));
        assertTrue(ex.getMessage().contains("already exists"));
        assertEquals(1, users.findAll().size());
    }

    @Test
    void ordinaryRegistrationCannotAssignAdminRole() {
        InMemoryUserDao users = new InMemoryUserDao();
        JdbcAuthService authService = new JdbcAuthService(users);

        User created = authService.register("Regular User", "user@campus.edu", "StrongPass123");

        assertEquals(Role.USER, created.getRole());
        assertFalse(authService.hasAdministrator());
    }

    @Test
    void invalidFirstAdminCredentialsAreRejected() {
        InMemoryUserDao users = new InMemoryUserDao();
        JdbcAuthService authService = new JdbcAuthService(users);

        assertThrows(ValidationException.class,
                () -> authService.bootstrapFirstAdmin("", "admin@campus.edu", "StrongPass123"));
        assertThrows(ValidationException.class,
                () -> authService.bootstrapFirstAdmin("Root Admin", "not-an-email", "StrongPass123"));
        assertThrows(ValidationException.class,
                () -> authService.bootstrapFirstAdmin("Root Admin", "admin@campus.edu", "bad"));
        assertFalse(authService.hasAdministrator());
    }

    @Test
    void failedBootstrapDoesNotLeavePartialAccount() {
        InMemoryUserDao users = new InMemoryUserDao() {
            @Override
            public User save(User value) {
                if (value.getEmail().equals("duplicate@campus.edu")) {
                    throw new RuntimeException("simulated failure");
                }
                return super.save(value);
            }
        };
        JdbcAuthService authService = new JdbcAuthService(users);

        assertThrows(RuntimeException.class,
                () -> authService.bootstrapFirstAdmin("Broken Admin", "duplicate@campus.edu", "StrongPass123"));
        assertTrue(users.findAll().isEmpty());
    }

    @Test
    void reportingWorkflowStillSavesAValidItem() {
        User reporter = user(1L, Role.USER);
        InMemoryItemDao items = new InMemoryItemDao(null);
        JdbcItemService service = new JdbcItemService(items, new InMemoryUserDao(reporter));
        Item newItem = item(null, reporter);

        Item saved = service.reportItem(newItem);

        assertSame(newItem, saved);
        assertSame(newItem, items.stored);
    }

    private static User user(Long id, Role role) {
        User user = new User("Test User", "user" + id + "@example.test", "hash", role, LocalDateTime.now());
        user.setId(id);
        return user;
    }

    private static Item item(Long id, User reporter) {
        Category category = new Category();
        category.setId(3L);
        Item item = new Item();
        item.setId(id);
        item.setTitle("Item " + id);
        item.setType(ItemType.LOST);
        item.setStatus(ItemStatus.OPEN);
        item.setCategory(category);
        item.setReportedBy(reporter);
        item.setReportedAt(LocalDateTime.now().minusDays(1));
        return item;
    }

    private static final class InMemoryClaimDao implements ClaimDAO {
        private final Claim claim;
        private boolean deleted;

        private InMemoryClaimDao(Claim claim) { this.claim = claim; }
        @Override public Claim save(Claim value) { return value; }
        @Override public Optional<Claim> findById(Long id) { return Optional.ofNullable(claim); }
        @Override public List<Claim> findAll() { return claim == null ? List.of() : List.of(claim); }
        @Override public List<Claim> findByClaimant(Long userId) { return List.of(); }
        @Override public List<Claim> findByItem(Long itemId) { return List.of(); }
        @Override public List<Claim> findByStatus(ClaimStatus status) { return List.of(); }
        @Override public Claim update(Claim value) { return value; }
        @Override public boolean deleteById(Long id) { deleted = true; return true; }
    }

    private static class InMemoryUserDao implements UserDAO {
        private final Map<Long, User> users = new HashMap<>();

        private InMemoryUserDao(User... values) {
            for (User user : values) users.put(user.getId(), user);
        }

        @Override public User save(User value) { users.put(value.getId(), value); return value; }
        @Override public Optional<User> findById(Long id) { return Optional.ofNullable(users.get(id)); }
        @Override public Optional<User> findByEmail(String email) { return users.values().stream().filter(u -> email.equals(u.getEmail())).findFirst(); }
        @Override public List<User> findAll() { return List.copyOf(users.values()); }
        @Override public User update(User value) { users.put(value.getId(), value); return value; }
        @Override public boolean deleteById(Long id) { return users.remove(id) != null; }
    }

    private static final class InMemoryItemDao implements ItemDAO {
        private Item stored;
        private boolean updated;

        private InMemoryItemDao(Item stored) { this.stored = stored; }

        @Override public Item save(Item value) { stored = value; return value; }
        @Override public Optional<Item> findById(Long id) { return stored != null && id.equals(stored.getId()) ? Optional.of(stored) : Optional.empty(); }
        @Override public List<Item> findAll() { return stored == null ? List.of() : List.of(stored); }
        @Override public List<Item> findByType(ItemType type) { return findAll(); }
        @Override public List<Item> findByCategory(Long id) { return findAll(); }
        @Override public List<Item> findByCategoryAndType(Long categoryId, ItemType type) { return findAll(); }
        @Override public List<Item> findByStatus(ItemStatus status) { return findAll(); }
        @Override public List<Item> findByUser(Long userId) { return findAll(); }
        @Override public List<Item> search(String keyword) { return findAll(); }
        @Override public List<Item> searchByType(String keyword, ItemType type) { return findAll(); }
        @Override public Item update(Item value) { updated = true; stored = value; return value; }
        @Override public boolean deleteById(Long id) { return false; }
    }
}
