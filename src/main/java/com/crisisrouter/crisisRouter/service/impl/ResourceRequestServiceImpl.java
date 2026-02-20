package com.crisisrouter.crisisRouter.service.impl;

import com.crisisrouter.crisisRouter.model.entity.Category;
import com.crisisrouter.crisisRouter.model.entity.ResourceRequest;
import com.crisisrouter.crisisRouter.model.entity.RequestStatus;
import com.crisisrouter.crisisRouter.repository.CategoryRepository;
import com.crisisrouter.crisisRouter.repository.ResourceRequestRepository;
import com.crisisrouter.crisisRouter.repository.UserRepository;
import com.crisisrouter.crisisRouter.service.FileStorageService;
import com.crisisrouter.crisisRouter.service.ResourceRequestService;
import com.crisisrouter.crisisRouter.service.dto.ResourceRequestDTO;
import com.crisisrouter.crisisRouter.service.mapper.ResourceRequestMapper;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.crisisrouter.crisisRouter.model.entity.User;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceRequestServiceImpl implements ResourceRequestService {

    private final UserRepository userRepository;
    private final ResourceRequestRepository requestRepository;
    private final CategoryRepository categoryRepository;
    private final ResourceRequestMapper mapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final FileStorageService fileStorageService;

    // GeometryFactory for spatial math (SRID 4326 = GPS coordinates)
    private final GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);

    public String getCurrentUserEmail() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof OidcUser) {
            return ((OidcUser) principal).getEmail();
        }
        return null;
    }

    @Override
    @Transactional
    public ResourceRequestDTO createRequest(ResourceRequestDTO requestDTO, MultipartFile image) {
        // 1. Fetch the official Category from the DB
        Category category = categoryRepository.findById(requestDTO.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with ID: " + requestDTO.getCategoryId()));

        String email = getCurrentUserEmail();
        if (email == null) {
            throw new RuntimeException("User must be authenticated to create a request");
        }

        User creator = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found for email: " + email));

        // 2. ENFORCER LOGIC: Check the "Other" rule
        if (category.getName().equalsIgnoreCase("Other")) {
            if (requestDTO.getCustomCategory() == null || requestDTO.getCustomCategory().isBlank()) {
                throw new IllegalArgumentException(
                        "A custom category description must be provided when 'Other' is selected.");
            }
        }

        // 3. Map DTO to Entity and link the resolved category
        ResourceRequest entity = mapper.toEntity(requestDTO);
        entity.setCategory(category);
        entity.setUser(creator);

        if (image != null && !image.isEmpty()) {
            String imageUrl = fileStorageService.storeFile(image);
            entity.setImageUrl(imageUrl);
        }

        // Build the PostGIS Point from the DTO's lat/lng
        // (The mapper ignores 'location' so we must set it manually)
        if (requestDTO.getLatitude() != null && requestDTO.getLongitude() != null) {
            Point point = factory.createPoint(
                    new Coordinate(requestDTO.getLongitude(), requestDTO.getLatitude()));
            entity.setLocation(point);
        }

        // Default new requests to OPEN status
        entity.setStatus(RequestStatus.OPEN);

        // 4. Save and return as DTO
        ResourceRequest saved = requestRepository.save(entity);

        ResourceRequestDTO savedDTO = mapper.toDTO(saved);

        messagingTemplate.convertAndSend("/topic/requests", savedDTO);
        return savedDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResourceRequestDTO> findNearby(Double longitude, Double latitude, Double radiusInMeters) {
        // Convert the doubles from the frontend into a JTS Point for PostGIS
        Point searchPoint = factory.createPoint(new Coordinate(longitude, latitude));

        // Call our specialized Repository method
        List<ResourceRequest> entities = requestRepository.findNearbyRequests(searchPoint, radiusInMeters);

        // Use Java Streams to translate every entity in the list to a DTO
        return entities.stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceRequestDTO getById(UUID id) {
        return requestRepository.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new RuntimeException("Request not found"));
    }

    @Override
    @Transactional
    public ResourceRequestDTO updateStatus(UUID id, String statusStr) {
        // 1. Find the existing request
        ResourceRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        // 2. Convert string to Enum safely
        try {
            RequestStatus newStatus = RequestStatus.valueOf(statusStr.toUpperCase());
            request.setStatus(newStatus);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + statusStr);
        }

        // 3. Save the update
        ResourceRequest updated = requestRepository.save(request);

        // (This is where you will trigger your AuditLog later!)

        return mapper.toDTO(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResourceRequestDTO> getMyRequests() {
        String email = getCurrentUserEmail();
        if (email == null) {
            throw new RuntimeException("User must be authenticated");
        }

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found for email: " + email));

        return requestRepository.findByUserId(currentUser.getId())
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ResourceRequestDTO updateRequest(UUID id, ResourceRequestDTO dto) {
        ResourceRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found with ID: " + id));

        // Only allow editing if the request is still OPEN
        if (request.getStatus() != RequestStatus.OPEN) {
            throw new IllegalStateException("Cannot edit a request that is " + request.getStatus());
        }

        // Update editable fields
        if (dto.getTitle() != null)
            request.setTitle(dto.getTitle());
        if (dto.getDescription() != null)
            request.setDescription(dto.getDescription());
        if (dto.getAddress() != null)
            request.setAddress(dto.getAddress());
        if (dto.getSeverityLevel() >= 1 && dto.getSeverityLevel() <= 5) {
            request.setSeverityLevel(dto.getSeverityLevel());
        }

        // Re-set location if coordinates changed
        if (dto.getLatitude() != null && dto.getLongitude() != null) {
            Point point = factory.createPoint(
                    new Coordinate(dto.getLongitude(), dto.getLatitude()));
            request.setLocation(point);
        }

        ResourceRequest saved = requestRepository.save(request);
        return mapper.toDTO(saved);
    }
}