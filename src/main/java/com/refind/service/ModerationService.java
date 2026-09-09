package com.refind.service;

import com.refind.model.Claim;

import java.util.List;

public interface ModerationService {

    List<Claim> getPendingClaims();

    Claim approveClaim(Long claimId, Long moderatorId);

    Claim rejectClaim(Long claimId, Long moderatorId);
}
