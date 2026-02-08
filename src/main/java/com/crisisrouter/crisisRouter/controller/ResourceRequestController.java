package com.crisisrouter.crisisRouter.controller;

import com.crisisrouter.crisisRouter.service.ResourceRequestService;
import com.crisisrouter.crisisRouter.service.dto.ResourceRequestDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/requests")
public class ResourceRequestController {

    private final ResourceRequestService resourceRequestService;

    // POST /api/requests
    // Create a new crisis request
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResourceRequestDTO> createReq(
            @RequestPart("request") @Valid ResourceRequestDTO requestDTO,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        // Now we pass BOTH arguments to your updated Service
        ResourceRequestDTO created = resourceRequestService.createRequest(requestDTO, image);

        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }
    //GET /api/requests/nearby
    // Find based on location

    @GetMapping("/nearby")
    public ResponseEntity<List<ResourceRequestDTO>> getNearbyRequests(
            @RequestParam Double longitude,
            @RequestParam Double latitude,
            @RequestParam(defaultValue = "10000.0") Double radiusInMeters){
        List<ResourceRequestDTO> nearby = resourceRequestService.findNearby(longitude, latitude, radiusInMeters);
        return ResponseEntity.ok(nearby);
    }

    // GET /api/requests/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ResourceRequestDTO> getById(@PathVariable UUID id){
        ResourceRequestDTO request = resourceRequestService.getById(id);
        return ResponseEntity.ok(request);

    }

    // PATCH /api/requests/{id}/status
    @PatchMapping("/{id}")
    public ResponseEntity<ResourceRequestDTO> updateStatus(
            @PathVariable UUID id,
            @RequestParam String status){
        ResourceRequestDTO updated = resourceRequestService.updateStatus(id, status);
        return ResponseEntity.ok(updated);
    }

}
