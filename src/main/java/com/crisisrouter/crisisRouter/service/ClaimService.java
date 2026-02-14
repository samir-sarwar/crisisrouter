package com.crisisrouter.crisisRouter.service;

import com.crisisrouter.crisisRouter.service.dto.ClaimDTO;
import java.util.List;
import java.util.UUID;

public interface ClaimService {
    ClaimDTO claimRequest(UUID requestId, UUID volunteerId);

    List<ClaimDTO> getVolunteerClaims(UUID volunteerId);

    List<ClaimDTO> getClaimsByRequestId(UUID requestId);

    List<ClaimDTO> getMyClaims();

    void completeClaim(UUID claimId);

    void dropClaim(UUID claimId);
}