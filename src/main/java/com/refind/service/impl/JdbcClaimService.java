package com.refind.service.impl;

import com.refind.dao.ClaimDAO;
import com.refind.exception.ValidationException;
import com.refind.model.Claim;
import com.refind.model.enums.ClaimStatus;
import com.refind.service.ClaimService;

import java.util.List;
import java.util.Optional;

public class JdbcClaimService implements ClaimService {

    private final ClaimDAO dao;

    public JdbcClaimService(ClaimDAO dao) {
        this.dao = dao;
    }

    @Override
    public Claim submitClaim(Claim c) {
        if (c == null || c.getClaimant() == null || c.getClaimant().getId() == null) {
            throw new ValidationException("Claimant is required.");
        }
        if (c.getItem() == null || c.getItem().getId() == null) {
            throw new ValidationException("Item is required.");
        }
        if (c.getStatus() == null) {
            c.setStatus(ClaimStatus.PENDING);
        }
        return dao.save(c);
    }

    @Override
    public Optional<Claim> getClaimById(Long id) {
        return dao.findById(id);
    }

    @Override
    public List<Claim> getAllClaims() {
        return dao.findAll();
    }

    @Override
    public List<Claim> getClaimsByClaimant(Long id) {
        return dao.findByClaimant(id);
    }

    @Override
    public List<Claim> getClaimsByItem(Long id) {
        return dao.findByItem(id);
    }

    @Override
    public List<Claim> getClaimsByStatus(ClaimStatus s) {
        return dao.findByStatus(s);
    }

    @Override
    public Claim updateClaim(Claim c) {
        if (c.getId() == null) {
            throw new ValidationException("Claim id is required for update.");
        }
        return dao.update(c);
    }

    @Override
    public boolean deleteClaim(Long id) {
        return dao.deleteById(id);
    }
}
