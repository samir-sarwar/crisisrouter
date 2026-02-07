package com.crisisrouter.crisisRouter.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimDTO {
    private UUID id;
    private UUID resourceRequestId;
    private UUID volunteerId;
    private LocalDateTime claimedAt;
    private String status; // e.g., "ACTIVE", "COMPLETED"
}