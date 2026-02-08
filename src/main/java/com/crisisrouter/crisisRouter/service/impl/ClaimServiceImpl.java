package com.crisisrouter.crisisRouter.service.impl;

import com.crisisrouter.crisisRouter.model.entity.Claim;
import com.crisisrouter.crisisRouter.model.entity.RequestStatus;
import com.crisisrouter.crisisRouter.model.entity.ResourceRequest;
import com.crisisrouter.crisisRouter.model.entity.User;
import com.crisisrouter.crisisRouter.repository.ClaimRepository;
import com.crisisrouter.crisisRouter.repository.ResourceRequestRepository;
import com.crisisrouter.crisisRouter.repository.UserRepository;
import com.crisisrouter.crisisRouter.service.AuditService;
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
    private final AuditService auditService;

    @Override
    @Transactional
    public ClaimDTO claimRequest(UUID requestId, UUID volunteerId) {
        // 1. Find the request
        ResourceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        RequestStatus currentStatus = request.getStatus();
        String oldStatus = currentStatus.toString();

        // 2. Validate that it is actually OPEN
        if (request.getStatus() != RequestStatus.OPEN) {
            throw new IllegalStateException("Request is not OPEN. Current status: " + request.getStatus());
        }

        // 3. Find the volunteer
        User volunteer = userRepository.findById(volunteerId)
                .orElseThrow(() -> new RuntimeException("Volunteer not found"));

        // 4. Create the Claim record
        Claim claim = new Claim();
        claim.setRequest(request);
        claim.setUser(volunteer);
        claim.setClaimedAt(LocalDateTime.now());
        claim.setStatus("ACTIVE");

        // 5. Update the Request status
        request.setStatus(RequestStatus.CLAIMED);

        // 6. Save changes
        requestRepository.save(request);
        Claim savedClaim = claimRepository.save(claim);

        // 7. PREPARE DTO (Moved up so we can use it in WebSocket)
        ClaimDTO claimDTO = new ClaimDTO(
                savedClaim.getId(),
                request.getId(),
                volunteer.getId(),
                savedClaim.getClaimedAt(),
                savedClaim.getStatus()
        );

        // 8. BROADCAST SAFE DTO (Fixes Recursion & PostGIS error)
        messagingTemplate.convertAndSend("/topic/claim/" + requestId, claimDTO);

        String oldStatusJson = "{\"status\": \"" + oldStatus + "\"}";
        String newStatusJson = "{\"status\": \"CLAIMED\"}";

        // 10. Audit Log
        auditService.logAction(
                "CLAIM_REQUEST",
                "ResourceRequest",
                request.getId(),
                oldStatusJson,             // Now it passes valid JSON
                newStatusJson,             // Now it passes valid JSON
                volunteer
        );

        return claimDTO;
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

        // 2. Update Claim Status
        claim.setStatus("FULFILLED");

        // 3. Update Request Status
        ResourceRequest request = claim.getRequest();
        request.setStatus(RequestStatus.FULFILLED);

        // 4. Save both
        claimRepository.save(claim);
        requestRepository.save(request);

        // 5. PREPARE DTO for Broadcast
        ClaimDTO claimDTO = new ClaimDTO(
                claim.getId(),
                request.getId(),
                claim.getUser().getId(),
                claim.getClaimedAt(),
                claim.getStatus()
        );

        // 6. BROADCAST COMPLETION (So the map updates instantly for everyone)
        messagingTemplate.convertAndSend("/topic/claim/" + request.getId(), claimDTO);
    }
}