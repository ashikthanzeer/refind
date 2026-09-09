package com.refind.dao;

import com.refind.model.Claim;
import com.refind.model.enums.ClaimStatus;

import java.util.List;
import java.util.Optional;

public interface ClaimDAO {

    Claim save(Claim claim);

    Optional<Claim> findById(Long id);

    List<Claim> findAll();

    List<Claim> findByClaimant(Long userId);

    List<Claim> findByFoundItem(Long foundItemId);

    List<Claim> findByStatus(ClaimStatus status);

    Claim update(Claim claim);

    boolean deleteById(Long id);
}
