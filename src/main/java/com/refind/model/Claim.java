package com.refind.model;

import com.refind.model.enums.ClaimStatus;

import java.time.LocalDateTime;

public class Claim {

    private Long id;
    private User claimant;
    private Item item;
    private String message;
    private ClaimStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime decidedAt;

    public Claim() {
    }

    public Claim(User claimant,
                 Item item,
                 String message,
                 ClaimStatus status,
                 LocalDateTime submittedAt,
                 LocalDateTime decidedAt) {
        this.claimant = claimant;
        this.item = item;
        this.message = message;
        this.status = status;
        this.submittedAt = submittedAt;
        this.decidedAt = decidedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getClaimant() {
        return claimant;
    }

    public void setClaimant(User claimant) {
        this.claimant = claimant;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public void setStatus(ClaimStatus status) {
        this.status = status;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt=decidedAt;
    }

}
