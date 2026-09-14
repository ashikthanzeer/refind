package com.refind.model;

import com.refind.model.enums.ModerationDecision;
import java.time.LocalDateTime;

public class Moderation {
    private Long id;
    private Claim claim;
    private User moderator;
    private ModerationDecision decision;
    private String comment;
    private LocalDateTime decidedAt;

    public Moderation() {}
    public Moderation(Claim claim, User moderator, ModerationDecision decision, String comment, LocalDateTime decidedAt) {
        this.claim = claim;
        this.moderator = moderator;
        this.decision = decision;
        this.comment = comment;
        this.decidedAt = decidedAt;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Claim getClaim() { return claim; }
    public void setClaim(Claim claim) { this.claim = claim; }
    public User getModerator() { return moderator; }
    public void setModerator(User moderator) { this.moderator = moderator; }
    public ModerationDecision getDecision() { return decision; }
    public void setDecision(ModerationDecision decision) { this.decision = decision; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }
}
