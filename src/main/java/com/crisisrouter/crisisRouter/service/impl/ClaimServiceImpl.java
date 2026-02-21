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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
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

        // ── Auth helper (same pattern as ResourceRequestServiceImpl) ──
        private String getCurrentUserEmail() {
                Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                if (principal instanceof OidcUser) {
                        return ((OidcUser) principal).getEmail();
                }
                return null;
        }

        @Override
        @Transactional
        public ClaimDTO claimRequest(UUID requestId, UUID volunteerId) {
                ResourceRequest request = requestRepository.findById(requestId)
                                .orElseThrow(() -> new RuntimeException("Request not found"));

                RequestStatus currentStatus = request.getStatus();
                String oldStatus = currentStatus.toString();

                if (request.getStatus() != RequestStatus.OPEN) {
                        throw new IllegalStateException("Request is not OPEN. Current status: " + request.getStatus());
                }

                User volunteer = userRepository.findById(volunteerId)
                                .orElseThrow(() -> new RuntimeException("Volunteer not found"));

                Claim claim = new Claim();
                claim.setRequest(request);
                claim.setUser(volunteer);
                claim.setClaimedAt(LocalDateTime.now());
                claim.setStatus("ACTIVE");

                request.setStatus(RequestStatus.CLAIMED);

                requestRepository.save(request);
                Claim savedClaim = claimRepository.save(claim);

                ClaimDTO claimDTO = toClaimDTO(savedClaim);

                messagingTemplate.convertAndSend("/topic/claim/" + requestId, claimDTO);

                String oldStatusJson = "{\"status\": \"" + oldStatus + "\"}";
                String newStatusJson = "{\"status\": \"CLAIMED\"}";

                auditService.logAction(
                                "CLAIM_REQUEST",
                                "ResourceRequest",
                                request.getId(),
                                oldStatusJson,
                                newStatusJson,
                                volunteer);

                return claimDTO;
        }

        @Override
        @Transactional(readOnly = true)
        public List<ClaimDTO> getVolunteerClaims(UUID volunteerId) {
                return claimRepository.findByUserId(volunteerId).stream()
                                .map(this::toClaimDTO)
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional(readOnly = true)
        public List<ClaimDTO> getClaimsByRequestId(UUID requestId) {
                return claimRepository.findByRequestId(requestId).stream()
                                .map(this::toClaimDTO)
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional(readOnly = true)
        public List<ClaimDTO> getMyClaims() {
                String email = getCurrentUserEmail();
                if (email == null) {
                        throw new RuntimeException("User must be authenticated");
                }

                User currentUser = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found for email: " + email));

                return claimRepository.findByUserId(currentUser.getId()).stream()
                                .map(this::toClaimDTO)
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional
        public void completeClaim(UUID claimId) {
                Claim claim = claimRepository.findById(claimId)
                                .orElseThrow(() -> new RuntimeException("Claim not found"));

                claim.setStatus("FULFILLED");

                ResourceRequest request = claim.getRequest();
                request.setStatus(RequestStatus.FULFILLED);

                claimRepository.save(claim);
                requestRepository.save(request);

                ClaimDTO claimDTO = toClaimDTO(claim);
                messagingTemplate.convertAndSend("/topic/claim/" + request.getId(), claimDTO);
        }

        @Override
        @Transactional
        public void dropClaim(UUID claimId) {
                Claim claim = claimRepository.findById(claimId)
                                .orElseThrow(() -> new RuntimeException("Claim not found"));

                if (!"ACTIVE".equals(claim.getStatus())) {
                        throw new IllegalStateException("Can only drop an ACTIVE claim. Current: " + claim.getStatus());
                }

                claim.setStatus("DROPPED");
                claimRepository.save(claim);

                // Revert the request back to OPEN so other volunteers can claim it
                ResourceRequest request = claim.getRequest();
                if (request.getStatus() == RequestStatus.CLAIMED) {
                        request.setStatus(RequestStatus.OPEN);
                        requestRepository.save(request);
                }

                ClaimDTO claimDTO = toClaimDTO(claim);
                messagingTemplate.convertAndSend("/topic/claim/" + request.getId(), claimDTO);
        }

        // ── Helper: Claim entity → ClaimDTO with all related info ──
        private ClaimDTO toClaimDTO(Claim claim) {
                ClaimDTO dto = new ClaimDTO();
                dto.setId(claim.getId());
                dto.setResourceRequestId(claim.getRequest().getId());
                dto.setVolunteerId(claim.getUser().getId());
                dto.setClaimedAt(claim.getClaimedAt());
                dto.setStatus(claim.getStatus());

                // Volunteer info
                User volunteer = claim.getUser();
                dto.setVolunteerFirstName(volunteer.getFirstName());
                dto.setVolunteerLastName(volunteer.getLastName());
                dto.setVolunteerEmail(volunteer.getEmail());
                dto.setVolunteerPhone(volunteer.getPhoneNumber());

                // Request details
                ResourceRequest req = claim.getRequest();
                dto.setRequestTitle(req.getTitle());
                dto.setRequestDescription(req.getDescription());
                dto.setRequestAddress(req.getAddress());
                dto.setRequestSeverityLevel(req.getSeverityLevel());
                dto.setRequestImageUrl(req.getImageUrl());
                dto.setRequestStatus(req.getStatus().name());
                if (req.getLatitude() != null) {
                        dto.setRequestLatitude(req.getLatitude());
                }
                if (req.getLongitude() != null) {
                        dto.setRequestLongitude(req.getLongitude());
                }

                // Requester (request creator) info
                User requester = req.getUser();
                dto.setRequesterFirstName(requester.getFirstName());
                dto.setRequesterLastName(requester.getLastName());
                dto.setRequesterEmail(requester.getEmail());
                dto.setRequesterPhone(requester.getPhoneNumber());

                return dto;
        }
}