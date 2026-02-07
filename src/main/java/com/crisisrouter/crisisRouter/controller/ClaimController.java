package com.crisisrouter.crisisRouter.controller;

import com.crisisrouter.crisisRouter.service.ClaimService;
import com.crisisrouter.crisisRouter.service.dto.ClaimDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

    /**
     * POST /api/claims/request/{requestId}?volunteerId={uuid}
     * Allows a volunteer to accept a specific help request.
     */
    @PostMapping("/request/{requestId}")
    public ResponseEntity<ClaimDTO> claimRequest(
            @PathVariable UUID requestId,
            @RequestParam UUID volunteerId) {
        // Note: volunteerId is a param for now; later it will come from the JWT token
        return ResponseEntity.ok(claimService.claimRequest(requestId, volunteerId));
    }

    /**
     * GET /api/claims/volunteer/{volunteerId}
     * Shows a volunteer all the tasks they have currently claimed or finished.
     */
    @GetMapping("/volunteer/{volunteerId}")
    public ResponseEntity<List<ClaimDTO>> getVolunteerClaims(@PathVariable UUID volunteerId) {
        return ResponseEntity.ok(claimService.getVolunteerClaims(volunteerId));
    }

    /**
     * PATCH /api/claims/{claimId}/complete
     * Marks a task as fully done.
     * This updates both the Claim status AND the original Request status.
     */
    @PatchMapping("/{claimId}/complete")
    public ResponseEntity<Void> completeClaim(@PathVariable UUID claimId) {
        claimService.completeClaim(claimId);
        return ResponseEntity.noContent().build(); // Returns 204 No Content (Standard for void actions)
    }
}