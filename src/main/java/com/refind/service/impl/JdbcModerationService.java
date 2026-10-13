package com.refind.service.impl;

import com.refind.dao.ClaimDAO;
import com.refind.dao.ItemDAO;
import com.refind.dao.ModerationDAO;
import com.refind.dao.NotificationDAO;
import com.refind.dao.UserDAO;
import com.refind.dao.impl.JdbcUserDAO;
import com.refind.database.DatabaseConnection;
import com.refind.exception.ValidationException;
import com.refind.model.Claim;
import com.refind.model.Item;
import com.refind.model.Notification;
import com.refind.model.User;
import com.refind.model.enums.ClaimStatus;
import com.refind.model.enums.ItemStatus;
import com.refind.model.enums.ModerationDecision;
import com.refind.model.enums.Role;
import com.refind.service.ModerationService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JdbcModerationService implements ModerationService {

    private final ClaimDAO claims;
    private final ModerationDAO moderation;
    private final ItemDAO items;
    private final NotificationDAO notifications;
    private final UserDAO users;

    public JdbcModerationService(ClaimDAO claims, ModerationDAO moderation, ItemDAO items, NotificationDAO notifications) {
        this(claims, moderation, items, notifications, new JdbcUserDAO());
    }

    public JdbcModerationService(ClaimDAO claims, ModerationDAO moderation, ItemDAO items,
                                NotificationDAO notifications, UserDAO users) {
        this.claims = claims;
        this.moderation = moderation;
        this.items = items;
        this.notifications = notifications;
        this.users = users;
    }

    @Override
    public List<Claim> getPendingClaims() {
        return claims.findByStatus(ClaimStatus.PENDING);
    }

    @Override
    public Claim approveClaim(Long claimId, Long moderatorId) {
        return approveClaim(claimId, moderatorId, "Claim verified and approved by administrator.");
    }

    @Override
    public Claim approveClaim(Long claimId, Long moderatorId, String comment) {
        return decide(claimId, moderatorId, ModerationDecision.APPROVED,
                comment != null && !comment.isBlank() ? comment : "Claim verified and approved.");
    }

    @Override
    public Claim rejectClaim(Long claimId, Long moderatorId) {
        return rejectClaim(claimId, moderatorId, "Proof of ownership insufficient or invalid.");
    }

    @Override
    public Claim rejectClaim(Long claimId, Long moderatorId, String comment) {
        if (comment == null || comment.isBlank()) {
            throw new ValidationException("An explanation comment is required when rejecting a claim.");
        }
        return decide(claimId, moderatorId, ModerationDecision.REJECTED, comment);
    }

    private void requireAdminReviewer(Long moderatorId) {
        if (moderatorId == null) {
            throw new ValidationException("Moderator identifier is required.");
        }
        if (users == null) {
            return;
        }
        User moderator = users.findById(moderatorId)
                .orElseThrow(() -> new ValidationException("Moderator not found: #" + moderatorId));
        if (moderator.getRole() != Role.ADMIN) {
            throw new ValidationException("Administrator access is required to review claims.");
        }
    }

    private Claim decide(Long claimId, Long moderatorId, ModerationDecision decision, String comment) {
        if (claimId == null) {
            throw new ValidationException("Claim identifier is required.");
        }

        requireAdminReviewer(moderatorId);

        LocalDateTime now = LocalDateTime.now();

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                LockedClaim claim = lockClaimAndItem(conn, claimId);
                if (claim.status != ClaimStatus.PENDING) {
                    throw new ValidationException("This claim has already been decided with status: " + claim.status + ".");
                }
                if (moderatorId.equals(claim.claimantId)) {
                    throw new ValidationException("Independent review required: Reviewers cannot decide claims on their own submitted claims.");
                }
                if (decision == ModerationDecision.APPROVED && isFinalItemStatus(claim.itemStatus)) {
                    throw new ValidationException("The claimed item is already " + claim.itemStatus + " and cannot be approved again.");
                }

                // Row locks make the check and state transition indivisible for this claim/item.
                // The conditional update is a second guard if another transaction changed it first.
                ClaimStatus newStatus = decision == ModerationDecision.APPROVED ? ClaimStatus.APPROVED : ClaimStatus.REJECTED;
                try (PreparedStatement claimStmt = conn.prepareStatement(
                        "UPDATE claims SET status = ?, decided_at = ? WHERE id = ? AND status = 'PENDING'")) {
                    claimStmt.setString(1, newStatus.name());
                    claimStmt.setTimestamp(2, Timestamp.valueOf(now));
                    claimStmt.setLong(3, claimId);
                    if (claimStmt.executeUpdate() != 1) {
                        throw new ValidationException("This claim is no longer pending and cannot be decided.");
                    }
                }

                try (PreparedStatement moderationStmt = conn.prepareStatement(
                        "INSERT INTO moderation (claim_id, moderator_id, decision, comment, decided_at) VALUES (?, ?, ?, ?, ?)")) {
                    moderationStmt.setLong(1, claimId);
                    moderationStmt.setLong(2, moderatorId);
                    moderationStmt.setString(3, decision.name());
                    moderationStmt.setString(4, comment);
                    moderationStmt.setTimestamp(5, Timestamp.valueOf(now));
                    moderationStmt.executeUpdate();
                }

                if (decision == ModerationDecision.APPROVED) {
                    try (PreparedStatement itemStmt = conn.prepareStatement(
                            "UPDATE items SET status = ? WHERE id = ? AND status NOT IN ('CLAIMED', 'RETURNED', 'CLOSED')")) {
                        itemStmt.setString(1, ItemStatus.CLAIMED.name());
                        itemStmt.setLong(2, claim.itemId);
                        if (itemStmt.executeUpdate() != 1) {
                            throw new ValidationException("The claimed item is no longer eligible for approval.");
                        }
                    }

                    try (PreparedStatement competingClaims = conn.prepareStatement(
                            "SELECT id, user_id FROM claims WHERE item_id = ? AND id <> ? AND status = 'PENDING' FOR UPDATE")) {
                        competingClaims.setLong(1, claim.itemId);
                        competingClaims.setLong(2, claimId);
                        try (ResultSet rs = competingClaims.executeQuery()) {
                            List<Long> rejectedClaimIds = new ArrayList<>();
                            while (rs.next()) {
                                rejectedClaimIds.add(rs.getLong("id"));
                            }
                            for (Long rejectedClaimId : rejectedClaimIds) {
                                try (PreparedStatement rejectStmt = conn.prepareStatement(
                                        "UPDATE claims SET status = ?, decided_at = ? WHERE id = ? AND status = 'PENDING'")) {
                                    rejectStmt.setString(1, ClaimStatus.REJECTED.name());
                                    rejectStmt.setTimestamp(2, Timestamp.valueOf(now));
                                    rejectStmt.setLong(3, rejectedClaimId);
                                    rejectStmt.executeUpdate();
                                }
                                try (PreparedStatement notifyStmt = conn.prepareStatement(
                                        "SELECT user_id FROM claims WHERE id = ?")) {
                                    notifyStmt.setLong(1, rejectedClaimId);
                                    try (ResultSet claimantRs = notifyStmt.executeQuery()) {
                                        if (claimantRs.next()) {
                                            long claimantId = claimantRs.getLong("user_id");
                                            try (PreparedStatement insertNotification = conn.prepareStatement(
                                                    "INSERT INTO notifications (user_id, text, read_flag, created_at) VALUES (?, ?, false, ?)")) {
                                                insertNotification.setLong(1, claimantId);
                                                insertNotification.setString(2, "Your claim #" + rejectedClaimId + " was rejected because another claim was verified and approved.");
                                                insertNotification.setTimestamp(3, Timestamp.valueOf(now));
                                                insertNotification.executeUpdate();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (claim.claimantId != null) {
                    try (PreparedStatement insertNotification = conn.prepareStatement(
                            "INSERT INTO notifications (user_id, text, read_flag, created_at) VALUES (?, ?, false, ?)")) {
                        insertNotification.setLong(1, claim.claimantId);
                        String outcome = decision == ModerationDecision.APPROVED ? "APPROVED" : "REJECTED";
                        insertNotification.setString(2, "Your claim #" + claimId + " has been " + outcome + ". Note: " + comment);
                        insertNotification.setTimestamp(3, Timestamp.valueOf(now));
                        insertNotification.executeUpdate();
                    }
                }

                conn.commit();
                return claims.findById(claimId)
                        .orElseThrow(() -> new ValidationException("Claim not found after moderation: #" + claimId));
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            throw new ValidationException("Failed to update moderation decision: " + e.getMessage());
        }
    }

    private LockedClaim lockClaimAndItem(Connection conn, Long claimId) throws SQLException {
        Long itemId;
        // Discover the item first, then always lock the shared item before an
        // individual claim. This consistent lock order serializes competing
        // approvals without creating a claim-row/item-row deadlock cycle.
        try (PreparedStatement claimStmt = conn.prepareStatement(
                "SELECT item_id FROM claims WHERE id = ?")) {
            claimStmt.setLong(1, claimId);
            try (ResultSet rs = claimStmt.executeQuery()) {
                if (!rs.next()) {
                    throw new ValidationException("Claim not found: #" + claimId);
                }
                itemId = rs.getLong("item_id");
            }
        }

        ItemStatus itemStatus;
        try (PreparedStatement itemStmt = conn.prepareStatement(
                "SELECT status FROM items WHERE id = ? FOR UPDATE")) {
            itemStmt.setLong(1, itemId);
            try (ResultSet rs = itemStmt.executeQuery()) {
                if (!rs.next()) {
                    throw new ValidationException("Item not found for claim #" + claimId);
                }
                itemStatus = ItemStatus.valueOf(rs.getString("status"));
            }
        }

        try (PreparedStatement claimStmt = conn.prepareStatement(
                "SELECT user_id, item_id, status FROM claims WHERE id = ? FOR UPDATE")) {
            claimStmt.setLong(1, claimId);
            try (ResultSet rs = claimStmt.executeQuery()) {
                if (!rs.next()) {
                    throw new ValidationException("Claim not found: #" + claimId);
                }
                Long lockedItemId = rs.getLong("item_id");
                if (!itemId.equals(lockedItemId)) {
                    throw new ValidationException("Claim item changed while preparing moderation; please retry.");
                }
                return new LockedClaim(rs.getLong("user_id"), lockedItemId,
                        ClaimStatus.valueOf(rs.getString("status")), itemStatus);
            }
        }
    }

    private boolean isFinalItemStatus(ItemStatus status) {
        return status == ItemStatus.CLAIMED || status == ItemStatus.RETURNED || status == ItemStatus.CLOSED;
    }

    private static final class LockedClaim {
        private final Long claimantId;
        private final Long itemId;
        private final ClaimStatus status;
        private final ItemStatus itemStatus;

        private LockedClaim(Long claimantId, Long itemId, ClaimStatus status, ItemStatus itemStatus) {
            this.claimantId = claimantId;
            this.itemId = itemId;
            this.status = status;
            this.itemStatus = itemStatus;
        }
    }
}
