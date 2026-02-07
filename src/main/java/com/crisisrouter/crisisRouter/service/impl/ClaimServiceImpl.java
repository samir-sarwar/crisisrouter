package com.crisisrouter.crisisRouter.service.impl;

import com.crisisrouter.crisisRouter.model.entity.Claim;
import com.crisisrouter.crisisRouter.model.entity.RequestStatus;
import com.crisisrouter.crisisRouter.model.entity.ResourceRequest;
import com.crisisrouter.crisisRouter.model.entity.User;
import com.crisisrouter.crisisRouter.repository.ClaimRepository;
import com.crisisrouter.crisisRouter.repository.ResourceRequestRepository;
import com.crisisrouter.crisisRouter.repository.UserRepository;
import com.crisisrouter.crisisRouter.service.ClaimService;
import com.crisisrouter.crisisRouter.service.dto.ClaimDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClaimServiceImpl implements ClaimService {

    private final ClaimRepository claimRepository;
    private final ResourceRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public ClaimDTO claimRequest(UUID requestId, UUID volunteerId) {
        // 1. Find the request
        ResourceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        // 2. Validate that it is actually OPEN
        if (request.getStatus() != RequestStatus.OPEN) {
            throw new IllegalStateException("Request is not OPEN. Current status: " + request.getStatus());
        }

        // 3. Find the volunteer
        User volunteer = userRepository.findById(volunteerId)
                .orElseThrow(() -> new RuntimeException("Volunteer not found"));

        // 4. Create the Claim record
        Claim claim = new Claim();
        claim.setRequest(request);    // Matches 'private ResourceRequest request' in Claim entity
        claim.setUser(volunteer);     // Matches 'private User user' in Claim entity
        claim.setClaimedAt(LocalDateTime.now());
        claim.setStatus("ACTIVE");    // This requires the String field we added to Claim.java

        // 5. Update the Request status to CLAIMED (Enum)
        request.setStatus(RequestStatus.CLAIMED);

        // 6. Save changes
        requestRepository.save(request);
        Claim savedClaim = claimRepository.save(claim);

        messagingTemplate.convertAndSend("/topic/claim" + requestId, savedClaim);
        // 7. Return the DTO
        return new ClaimDTO(
                savedClaim.getId(),
                request.getId(),
                volunteer.getId(),
                savedClaim.getClaimedAt(),
                savedClaim.getStatus()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimDTO> getVolunteerClaims(UUID volunteerId) {
        List<Claim> claims = claimRepository.findByUserId(volunteerId);

        return claims.stream()
                .map(claim -> new ClaimDTO(
                        claim.getId(),
                        claim.getRequest().getId(),
                        claim.getUser().getId(),
                        claim.getClaimedAt(),
                        claim.getStatus()
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void completeClaim(UUID claimId) {
        // 1. Find the claim
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found"));

        // 2. Update Claim Status (String)
        claim.setStatus("FULFILLED");

        // 3. Update Request Status (Enum) - Matches your RequestStatus.java file
        ResourceRequest request = claim.getRequest();
        request.setStatus(RequestStatus.FULFILLED);

        // 4. Save both
        claimRepository.save(claim);
        requestRepository.save(request);
    }
}