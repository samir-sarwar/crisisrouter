package com.crisisrouter.crisisRouter.service;

import com.crisisrouter.crisisRouter.service.dto.ClaimDTO;
import java.util.List;
import java.util.UUID;

public interface ClaimService {
    ClaimDTO claimRequest(UUID requestId, UUID volunteerId);
    List<ClaimDTO> getVolunteerClaims(UUID volunteerId);
    void completeClaim(UUID claimId);
}