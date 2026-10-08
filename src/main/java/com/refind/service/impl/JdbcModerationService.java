package com.refind.service.impl;

import com.refind.dao.ClaimDAO;
import com.refind.dao.ItemDAO;
import com.refind.dao.ModerationDAO;
import com.refind.dao.NotificationDAO;
import com.refind.exception.ValidationException;
import com.refind.model.Claim;
import com.refind.model.Item;
import com.refind.model.Moderation;
import com.refind.model.Notification;
import com.refind.model.User;
import com.refind.model.enums.ClaimStatus;
import com.refind.model.enums.ItemStatus;
import com.refind.model.enums.ModerationDecision;
import com.refind.service.ModerationService;

import java.time.LocalDateTime;
import java.util.List;

public class JdbcModerationService implements ModerationService {

    private final ClaimDAO claims;
    private final ModerationDAO moderation;
    private final ItemDAO items;
    private final NotificationDAO notifications;

    public JdbcModerationService(ClaimDAO claims, ModerationDAO moderation, ItemDAO items, NotificationDAO notifications) {
        this.claims = claims;
        this.moderation = moderation;
        this.items = items;
        this.notifications = notifications;
    }

    @Override
    public List<Claim> getPendingClaims() {
        return claims.findByStatus(ClaimStatus.PENDING);
    }

    @Override
    public Claim approveClaim(Long claimId, Long moderatorId) {
        return decide(claimId, moderatorId, ModerationDecision.APPROVED, null);
    }

    @Override
    public Claim approveClaim(Long claimId, Long moderatorId, String comment) {
        return decide(claimId, moderatorId, ModerationDecision.APPROVED, comment);
    }

    @Override
    public Claim rejectClaim(Long claimId, Long moderatorId) {
        return decide(claimId, moderatorId, ModerationDecision.REJECTED, null);
    }

    @Override
    public Claim rejectClaim(Long claimId, Long moderatorId, String comment) {
        return decide(claimId, moderatorId, ModerationDecision.REJECTED, comment);
    }

    private Claim decide(Long id, Long moderatorId, ModerationDecision decision, String comment) {
        Claim claim = claims.findById(id).orElseThrow(() -> new ValidationException("Claim not found: " + id));
        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new ValidationException("Claim is already decided.");
        }

        User mod = new User();
        mod.setId(moderatorId);

        Moderation modRecord = new Moderation(claim, mod, decision, comment, LocalDateTime.now());
        moderation.save(modRecord);

        claim.setStatus(decision == ModerationDecision.APPROVED ? ClaimStatus.APPROVED : ClaimStatus.REJECTED);
        claim.setDecidedAt(LocalDateTime.now());
        Claim updated = claims.update(claim);

        if (decision == ModerationDecision.APPROVED && claim.getItem() != null && claim.getItem().getId() != null) {
            items.findById(claim.getItem().getId()).ifPresent(item -> {
                item.setStatus(ItemStatus.CLAIMED);
                items.update(item);
            });
        }

        User claimant = claim.getClaimant();
        if (claimant != null && claimant.getId() != null) {
            String noteText = comment != null && !comment.isBlank() ? " Note: " + comment.trim() : "";
            Notification notification = new Notification(
                    claimant,
                    "Your claim #" + id + " was " + decision.name().toLowerCase() + "." + noteText,
                    false,
                    LocalDateTime.now()
            );
            notifications.save(notification);
        }

        return updated;
    }
}
