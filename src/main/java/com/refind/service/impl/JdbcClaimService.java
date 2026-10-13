package com.refind.service.impl;

import com.refind.dao.ClaimDAO;
import com.refind.dao.ItemDAO;
import com.refind.dao.NotificationDAO;
import com.refind.database.DatabaseConnection;
import com.refind.exception.ValidationException;
import com.refind.model.Claim;
import com.refind.model.Item;
import com.refind.model.Notification;
import com.refind.model.User;
import com.refind.model.enums.ClaimStatus;
import com.refind.model.enums.ItemStatus;
import com.refind.service.ClaimService;

import java.time.LocalDateTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public class JdbcClaimService implements ClaimService {

    private final ClaimDAO dao;
    private final ItemDAO itemDAO;
    private final NotificationDAO notificationDAO;

    public JdbcClaimService(ClaimDAO dao) {
        this(dao, null, null);
    }

    public JdbcClaimService(ClaimDAO dao, ItemDAO itemDAO, NotificationDAO notificationDAO) {
        this.dao = dao;
        this.itemDAO = itemDAO;
        this.notificationDAO = notificationDAO;
    }

    @Override
    public Claim submitClaim(Claim c) {
        if (c == null) {
            throw new ValidationException("Claim data cannot be null.");
        }
        if (c.getClaimant() == null || c.getClaimant().getId() == null) {
            throw new ValidationException("Claimant account is required to submit a claim.");
        }
        if (c.getItem() == null || c.getItem().getId() == null) {
            throw new ValidationException("Item reference is required to submit a claim.");
        }
        if (c.getMessage() == null || c.getMessage().isBlank()) {
            throw new ValidationException("Please provide details or proof of ownership to support your claim.");
        }
        if (c.getMessage().trim().length() < 5) {
            throw new ValidationException("Claim message must contain at least 5 characters explaining ownership.");
        }

        if (itemDAO != null) {
            return submitClaimWithItemLock(c);
        }

        // This constructor remains useful for isolated non-JDBC service tests.
        // Application paths supply an item DAO and use the transaction above.
        List<Claim> existingClaims = dao.findByClaimant(c.getClaimant().getId());
        for (Claim ec : existingClaims) {
            if (ec.getItem() != null && ec.getItem().getId() != null
                    && ec.getItem().getId().equals(c.getItem().getId())
                    && ec.getStatus() == ClaimStatus.PENDING) {
                throw new ValidationException("You already have an active pending claim for this item. Please wait for moderation.");
            }
        }

        if (c.getStatus() == null) {
            c.setStatus(ClaimStatus.PENDING);
        }
        if (c.getSubmittedAt() == null) {
            c.setSubmittedAt(LocalDateTime.now());
        }

        Claim saved = dao.save(c);

        // Notify claimant of successful submission
        if (notificationDAO != null) {
            try {
                String notifText = "Your claim #" + saved.getId() + " has been submitted and is pending administrator review.";
                notificationDAO.save(new Notification(c.getClaimant(), notifText, false, LocalDateTime.now()));
            } catch (Exception ignored) {}
        }

        return saved;
    }

    /**
     * Serializes submission with moderation on the item row. It prevents a new
     * pending claim from being inserted after an approval has claimed the item,
     * and keeps the claim and its submission notification atomic.
     */
    private Claim submitClaimWithItemLock(Claim claim) {
        Long itemId = claim.getItem().getId();
        Long claimantId = claim.getClaimant().getId();
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement itemStatement = connection.prepareStatement(
                        "SELECT reporter_id, status FROM items WHERE id = ? FOR UPDATE")) {
                    itemStatement.setLong(1, itemId);
                    try (ResultSet result = itemStatement.executeQuery()) {
                        if (!result.next()) {
                            throw new ValidationException("Claimed item #" + itemId + " does not exist.");
                        }
                        if (claimantId.equals(result.getLong("reporter_id"))) {
                            throw new ValidationException("You cannot submit an ownership claim for an item you reported.");
                        }
                        ItemStatus status = ItemStatus.valueOf(result.getString("status"));
                        if (status == ItemStatus.CLAIMED || status == ItemStatus.RETURNED || status == ItemStatus.CLOSED) {
                            throw new ValidationException("This item has already been " + status + " and is no longer eligible for new claims.");
                        }
                    }
                }

                try (PreparedStatement duplicateStatement = connection.prepareStatement(
                        "SELECT id FROM claims WHERE user_id = ? AND item_id = ? AND status = 'PENDING' FOR UPDATE")) {
                    duplicateStatement.setLong(1, claimantId);
                    duplicateStatement.setLong(2, itemId);
                    try (ResultSet result = duplicateStatement.executeQuery()) {
                        if (result.next()) {
                            throw new ValidationException("You already have an active pending claim for this item. Please wait for moderation.");
                        }
                    }
                }

                LocalDateTime submittedAt = claim.getSubmittedAt() == null ? LocalDateTime.now() : claim.getSubmittedAt();
                claim.setSubmittedAt(submittedAt);
                claim.setStatus(ClaimStatus.PENDING);
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO claims(user_id, item_id, message, status, submitted_at) VALUES (?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    insert.setLong(1, claimantId);
                    insert.setLong(2, itemId);
                    insert.setString(3, claim.getMessage());
                    insert.setString(4, ClaimStatus.PENDING.name());
                    insert.setTimestamp(5, Timestamp.valueOf(submittedAt));
                    insert.executeUpdate();
                    try (ResultSet keys = insert.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new ValidationException("Claim submission did not return an identifier.");
                        }
                        claim.setId(keys.getLong(1));
                    }
                }

                if (notificationDAO != null) {
                    try (PreparedStatement notification = connection.prepareStatement(
                            "INSERT INTO notifications (user_id, text, read_flag, created_at) VALUES (?, ?, false, ?)")) {
                        notification.setLong(1, claimantId);
                        notification.setString(2, "Your claim #" + claim.getId() + " has been submitted and is pending administrator review.");
                        notification.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                        notification.executeUpdate();
                    }
                }
                connection.commit();
                return claim;
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw new ValidationException("Failed to submit claim: " + ex.getMessage());
        }
    }

    @Override
    public Optional<Claim> getClaimById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return dao.findById(id);
    }

    @Override
    public List<Claim> getAllClaims() {
        return dao.findAll();
    }

    @Override
    public List<Claim> getClaimsByClaimant(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return dao.findByClaimant(userId);
    }

    @Override
    public List<Claim> getClaimsByItem(Long itemId) {
        if (itemId == null) {
            return List.of();
        }
        return dao.findByItem(itemId);
    }

    @Override
    public List<Claim> getClaimsByStatus(ClaimStatus status) {
        if (status == null) {
            return dao.findAll();
        }
        return dao.findByStatus(status);
    }

    @Override
    public boolean withdrawClaim(Long claimId, User currentUser) {
        if (claimId == null) {
            throw new ValidationException("Claim id is required.");
        }
        if (currentUser == null || currentUser.getId() == null) {
            throw new ValidationException("Authentication required to withdraw a claim.");
        }

        Claim existing = dao.findById(claimId)
                .orElseThrow(() -> new ValidationException("Claim not found: #" + claimId));

        if (!currentUser.getId().equals(existing.getClaimant().getId())) {
            throw new ValidationException("Permission denied: You can only withdraw your own claims.");
        }

        if (existing.getStatus() != ClaimStatus.PENDING) {
            throw new ValidationException("Only pending claims can be withdrawn. This claim is already " + existing.getStatus() + ".");
        }

        return dao.deleteById(claimId);
    }

    /**
     * Unauthenticated claim deletion is intentionally not supported. Claims may
     * only be withdrawn through withdrawClaim, which verifies ownership.
     */
    @Override
    public boolean deleteClaim(Long id) {
        throw new ValidationException("Claims must be withdrawn by their claimant through an authenticated session.");
    }
}
