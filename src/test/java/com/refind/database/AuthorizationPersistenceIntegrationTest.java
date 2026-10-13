package com.refind.database;

import com.refind.dao.CategoryDAO;
import com.refind.dao.ClaimDAO;
import com.refind.dao.ItemDAO;
import com.refind.dao.LocationDAO;
import com.refind.dao.ModerationDAO;
import com.refind.dao.NotificationDAO;
import com.refind.dao.UserDAO;
import com.refind.dao.impl.JdbcCategoryDAO;
import com.refind.dao.impl.JdbcClaimDAO;
import com.refind.dao.impl.JdbcItemDAO;
import com.refind.dao.impl.JdbcLocationDAO;
import com.refind.dao.impl.JdbcModerationDAO;
import com.refind.dao.impl.JdbcNotificationDAO;
import com.refind.dao.impl.JdbcUserDAO;
import com.refind.exception.ValidationException;
import com.refind.model.Category;
import com.refind.model.Claim;
import com.refind.model.Item;
import com.refind.model.Location;
import com.refind.model.User;
import com.refind.model.enums.ClaimStatus;
import com.refind.model.enums.ItemStatus;
import com.refind.model.enums.ItemType;
import com.refind.model.enums.Role;
import com.refind.service.impl.JdbcClaimService;
import com.refind.service.impl.JdbcItemService;
import com.refind.service.impl.JdbcModerationService;
import com.refind.service.impl.JdbcNotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Database-backed authorization and consistency tests. The integration Maven
 * profile refuses normal application configuration and requires REFIND_TEST_DB_*
 * variables, so every write here targets an explicitly named test database.
 */
@Tag("integration")
class AuthorizationPersistenceIntegrationTest {
    private final List<Long> userIds = new ArrayList<>();
    private final List<Long> categoryIds = new ArrayList<>();
    private final List<Long> locationIds = new ArrayList<>();
    private final List<Long> itemIds = new ArrayList<>();
    private final List<Long> claimIds = new ArrayList<>();

    private User owner;
    private User otherUser;
    private User administrator;
    private User secondAdministrator;
    private Category category;
    private Location location;
    private Item item;
    private UserDAO users;
    private ItemDAO items;
    private ClaimDAO claims;
    private NotificationDAO notifications;

    @BeforeEach
    void createIsolatedFixture() throws Exception {
        try (Connection connection = DatabaseConnection.getConnection()) {
            DatabaseConnection.ensureSchemaInitialized(connection);
        }
        users = new JdbcUserDAO();
        items = new JdbcItemDAO();
        claims = new JdbcClaimDAO();
        notifications = new JdbcNotificationDAO();

        owner = createUser(Role.USER);
        otherUser = createUser(Role.USER);
        administrator = createUser(Role.ADMIN);
        secondAdministrator = createUser(Role.ADMIN);
        category = new JdbcCategoryDAO().save(new Category("integration-category-" + UUID.randomUUID()));
        categoryIds.add(category.getId());
        location = new JdbcLocationDAO().save(new Location("integration-campus", "building", "room"));
        locationIds.add(location.getId());
        item = createItem(owner);
    }

    @AfterEach
    void removeOnlyThisTestsRecords() throws Exception {
        // Foreign-key order matters. Each statement is restricted to ids recorded
        // from this test's own inserts; no table-wide cleanup is performed.
        try (Connection connection = DatabaseConnection.getConnection()) {
            deleteByIds(connection, "DELETE FROM moderation WHERE claim_id IN ", claimIds);
            deleteByIds(connection, "DELETE FROM notifications WHERE user_id IN ", userIds);
            deleteByIds(connection, "DELETE FROM claims WHERE id IN ", claimIds);
            deleteByIds(connection, "DELETE FROM items WHERE id IN ", itemIds);
            deleteByIds(connection, "DELETE FROM users WHERE id IN ", userIds);
            deleteByIds(connection, "DELETE FROM categories WHERE id IN ", categoryIds);
            deleteByIds(connection, "DELETE FROM locations WHERE id IN ", locationIds);
        }
    }

    @Test
    void itemUpdatesUsePersistedAuthorizationAndPreserveReporter() {
        JdbcItemService service = new JdbcItemService(items, users);
        Item ownerEdit = items.findById(item.getId()).orElseThrow();
        ownerEdit.setTitle("owner edit");
        ownerEdit.setReportedBy(otherUser); // forged mutable input
        service.updateItem(ownerEdit, owner);
        assertEquals(owner.getId(), items.findById(item.getId()).orElseThrow().getReportedBy().getId());

        Item otherEdit = items.findById(item.getId()).orElseThrow();
        assertThrows(ValidationException.class, () -> service.updateItem(otherEdit, otherUser));
        assertThrows(ValidationException.class, () -> service.updateItem(otherEdit, null));

        // The database role, not the role attached to the caller's mutable User,
        // authorizes administration.
        User staleSessionUser = new User();
        staleSessionUser.setId(administrator.getId());
        staleSessionUser.setRole(Role.USER);
        service.updateItem(items.findById(item.getId()).orElseThrow(), staleSessionUser);
    }

    @Test
    void claimSubmissionWithdrawalAndNotificationRecipientAuthorizationPersist() {
        JdbcClaimService claimService = new JdbcClaimService(claims, items, notifications);
        Claim claim = submitClaim(claimService, otherUser, item);
        assertEquals(ClaimStatus.PENDING, claims.findById(claim.getId()).orElseThrow().getStatus());
        assertFalse(notifications.findByUser(otherUser.getId()).isEmpty());

        assertThrows(ValidationException.class, () -> claimService.withdrawClaim(claim.getId(), owner));
        assertTrue(claimService.withdrawClaim(claim.getId(), otherUser));
        assertTrue(claims.findById(claim.getId()).isEmpty());

        JdbcNotificationService notificationService = new JdbcNotificationService(notifications);
        Claim secondClaim = submitClaim(claimService, otherUser, item);
        Long notificationId = notifications.findByUser(otherUser.getId()).get(0).getId();
        assertFalse(notificationService.markAsRead(notificationId, owner));
        assertTrue(notificationService.markAsRead(notificationId, otherUser));
        assertEquals(ClaimStatus.PENDING, claims.findById(secondClaim.getId()).orElseThrow().getStatus());
    }

    @Test
    void moderationApprovesOneClaimRejectsCompetitorsAndProtectsReviewAuthorization() {
        JdbcClaimService claimService = new JdbcClaimService(claims, items, notifications);
        JdbcModerationService moderationService = moderationService();
        Claim first = submitClaim(claimService, otherUser, item);
        User secondClaimant = createUser(Role.USER);
        Claim competing = submitClaim(claimService, secondClaimant, item);
        Claim selfReviewedClaim = submitClaim(claimService, administrator, item);

        assertThrows(ValidationException.class, () -> moderationService.rejectClaim(first.getId(), owner.getId(), "not allowed"));
        assertThrows(ValidationException.class, () -> moderationService.approveClaim(selfReviewedClaim.getId(), administrator.getId()));
        moderationService.approveClaim(first.getId(), administrator.getId());

        assertEquals(ClaimStatus.APPROVED, claims.findById(first.getId()).orElseThrow().getStatus());
        assertEquals(ClaimStatus.REJECTED, claims.findById(competing.getId()).orElseThrow().getStatus());
        assertEquals(ClaimStatus.REJECTED, claims.findById(selfReviewedClaim.getId()).orElseThrow().getStatus());
        assertEquals(ItemStatus.CLAIMED, items.findById(item.getId()).orElseThrow().getStatus());
        assertFalse(notifications.findByUser(otherUser.getId()).isEmpty());
        assertFalse(notifications.findByUser(secondClaimant.getId()).isEmpty());

        // A rejected claim does not claim or otherwise mutate its item.
        Item separatelyRejectedItem = createItem(owner);
        Claim rejected = submitClaim(claimService, otherUser, separatelyRejectedItem);
        moderationService.rejectClaim(rejected.getId(), administrator.getId(), "Insufficient proof for integration test");
        assertEquals(ClaimStatus.REJECTED, claims.findById(rejected.getId()).orElseThrow().getStatus());
        assertEquals(ItemStatus.OPEN, items.findById(separatelyRejectedItem.getId()).orElseThrow().getStatus());

        // A later decision fails without changing the committed outcome.
        assertThrows(ValidationException.class, () -> moderationService.approveClaim(first.getId(), secondAdministrator.getId()));
        assertEquals(ClaimStatus.APPROVED, claims.findById(first.getId()).orElseThrow().getStatus());
        assertEquals(ItemStatus.CLAIMED, items.findById(item.getId()).orElseThrow().getStatus());
    }

    @Test
    void concurrentApprovalsCannotProduceTwoApprovedClaimsForOneItem() throws Exception {
        JdbcClaimService claimService = new JdbcClaimService(claims, items, notifications);
        Claim first = submitClaim(claimService, otherUser, item);
        User secondClaimant = createUser(Role.USER);
        Claim second = submitClaim(claimService, secondClaimant, item);

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> firstResult = executor.submit(() -> approveAfterStart(start, first.getId(), administrator.getId()));
            Future<Boolean> secondResult = executor.submit(() -> approveAfterStart(start, second.getId(), secondAdministrator.getId()));
            start.countDown();
            int approvedCalls = (firstResult.get() ? 1 : 0) + (secondResult.get() ? 1 : 0);
            assertEquals(1, approvedCalls);
        } finally {
            executor.shutdownNow();
        }

        long approvedClaims = claims.findByItem(item.getId()).stream()
                .filter(claim -> claim.getStatus() == ClaimStatus.APPROVED)
                .count();
        assertEquals(1, approvedClaims);
        assertEquals(ItemStatus.CLAIMED, items.findById(item.getId()).orElseThrow().getStatus());
    }

    @Test
    void concurrentOwnerUpdatesCannotChangeItemOwnership() throws Exception {
        JdbcItemService service = new JdbcItemService(items, users);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> updateAfterStart(start, service, "first concurrent edit"));
            Future<?> second = executor.submit(() -> updateAfterStart(start, service, "second concurrent edit"));
            start.countDown();
            first.get();
            second.get();
        } finally {
            executor.shutdownNow();
        }
        assertEquals(owner.getId(), items.findById(item.getId()).orElseThrow().getReportedBy().getId());
    }

    private boolean approveAfterStart(CountDownLatch start, Long claimId, Long adminId) throws Exception {
        start.await();
        try {
            moderationService().approveClaim(claimId, adminId);
            return true;
        } catch (ValidationException expectedConcurrentOutcome) {
            return false;
        }
    }

    private JdbcModerationService moderationService() {
        ModerationDAO moderation = new JdbcModerationDAO();
        return new JdbcModerationService(claims, moderation, items, notifications, users);
    }

    private void updateAfterStart(CountDownLatch start, JdbcItemService service, String title) {
        try {
            start.await();
            Item edit = items.findById(item.getId()).orElseThrow();
            edit.setTitle(title);
            edit.setReportedBy(otherUser);
            service.updateItem(edit, owner);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Concurrent update interrupted", interrupted);
        }
    }

    private User createUser(Role role) {
        String suffix = UUID.randomUUID().toString();
        User user = users.save(new User("integration-" + suffix, suffix + "@example.test", "test-hash", role, LocalDateTime.now()));
        userIds.add(user.getId());
        return user;
    }

    private Item createItem(User reporter) {
        Item created = new Item("integration-item-" + UUID.randomUUID(), "test item", ItemType.LOST, ItemStatus.OPEN,
                category, location, LocalDateTime.now(), null, reporter);
        created = items.save(created);
        itemIds.add(created.getId());
        return created;
    }

    private Claim submitClaim(JdbcClaimService service, User claimant, Item claimedItem) {
        Claim claim = new Claim();
        claim.setClaimant(claimant);
        claim.setItem(claimedItem);
        claim.setMessage("Proof for integration test");
        Claim saved = service.submitClaim(claim);
        claimIds.add(saved.getId());
        return saved;
    }

    private void deleteByIds(Connection connection, String prefix, List<Long> ids) throws Exception {
        if (ids.isEmpty()) return;
        String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        try (PreparedStatement statement = connection.prepareStatement(prefix + "(" + placeholders + ")")) {
            for (int i = 0; i < ids.size(); i++) statement.setLong(i + 1, ids.get(i));
            statement.executeUpdate();
        }
    }
}
