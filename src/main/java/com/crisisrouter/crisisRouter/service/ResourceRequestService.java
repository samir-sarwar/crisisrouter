package com.crisisrouter.crisisRouter.service;

import com.crisisrouter.crisisRouter.service.dto.ResourceRequestDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ResourceRequestService {
    // Saves a new request from a user
    ResourceRequestDTO createRequest(ResourceRequestDTO requestDTO, MultipartFile image);

    // The "PostGIS" logic will live inside this one
    List<ResourceRequestDTO> findNearby(Double longitude, Double latitude, Double radiusInMeters);

    // Gets a single request for a "Details" page
    ResourceRequestDTO getById(UUID id);

    // Updates status (e.g., OPEN to CLAIMED)
    ResourceRequestDTO updateStatus(UUID id, String status);

    // Fetches all requests created by the currently authenticated user
    List<ResourceRequestDTO> getMyRequests();
}