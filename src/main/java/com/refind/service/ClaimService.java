package com.refind.service;

import com.refind.model.Claim;
import com.refind.model.enums.ClaimStatus;
import java.util.List;
import java.util.Optional;

public interface ClaimService {
    Claim submitClaim(Claim claim);
    Optional<Claim> getClaimById(Long id);
    List<Claim> getAllClaims();
    List<Claim> getClaimsByClaimant(Long userId);
    List<Claim> getClaimsByItem(Long itemId);
    List<Claim> getClaimsByStatus(ClaimStatus status);
    Claim updateClaim(Claim claim);
    boolean deleteClaim(Long id);
}
