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
    private String status; // e.g., "ACTIVE", "FULFILLED", "DROPPED"

    // Volunteer contact info (populated when returning claims to request owners)
    private String volunteerFirstName;
    private String volunteerLastName;
    private String volunteerEmail;
    private String volunteerPhone;

    // Request details (populated when returning claims to volunteers)
    private String requestTitle;
    private String requestDescription;
    private String requestAddress;
    private Integer requestSeverityLevel;
    private String requestImageUrl;
    private String requestStatus;
    private Double requestLatitude;
    private Double requestLongitude;

    // Requester contact info (populated when returning claims to volunteers)
    private String requesterFirstName;
    private String requesterLastName;
    private String requesterEmail;
    private String requesterPhone;
}