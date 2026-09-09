package com.refind.model;

import com.refind.model.enums.ClaimStatus;

import java.time.LocalDateTime;

public class Claim {

    private Long id;
    private User claimant;
    private FoundItem foundItem;
    private String justification;
    private ClaimStatus status;
    private LocalDateTime createdAt;

    public Claim() {
    }

    public Claim(User claimant,
                 FoundItem foundItem,
                 String justification,
                 ClaimStatus status,
                 LocalDateTime createdAt) {
        this.claimant = claimant;
        this.foundItem = foundItem;
        this.justification = justification;
        this.status = status;
        this.createdAt = createdAt;
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

    public FoundItem getFoundItem() {
        return foundItem;
    }

    public void setFoundItem(FoundItem foundItem) {
        this.foundItem = foundItem;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public void setStatus(ClaimStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
