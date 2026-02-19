package com.crisisrouter.crisisRouter.testutil;

import com.crisisrouter.crisisRouter.model.entity.*;
import com.crisisrouter.crisisRouter.service.dto.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.time.LocalDateTime;
import java.util.UUID;

public final class TestDataFactory {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private TestDataFactory() {}

    public static User createUser() {
        return createUser("test@example.com", "John", "Doe");
    }

    public static User createUser(String email, String firstName, String lastName) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(UserRole.VOLUNTEER);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    public static Category createCategory() {
        return createCategory("Food", "Emergency food supplies");
    }

    public static Category createCategory(String name, String description) {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName(name);
        category.setDescription(description);
        return category;
    }

    public static ResourceRequest createResourceRequest(User user, Category category) {
        return createResourceRequest(user, category, RequestStatus.OPEN);
    }

    public static ResourceRequest createResourceRequest(User user, Category category, RequestStatus status) {
        ResourceRequest request = new ResourceRequest();
        request.setId(UUID.randomUUID());
        request.setUser(user);
        request.setCategory(category);
        request.setTitle("Emergency Food Needed");
        request.setDescription("Need food supplies urgently");
        request.setSeverityLevel(3);
        request.setAddress("123 Main St");
        request.setLocation(createPoint(-80.5449, 43.4723));
        request.setStatus(status);
        request.setCreatedAt(LocalDateTime.now());
        return request;
    }

    public static Claim createClaim(ResourceRequest request, User volunteer) {
        return createClaim(request, volunteer, "ACTIVE");
    }

    public static Claim createClaim(ResourceRequest request, User volunteer, String status) {
        Claim claim = new Claim();
        claim.setId(UUID.randomUUID());
        claim.setRequest(request);
        claim.setUser(volunteer);
        claim.setClaimedAt(LocalDateTime.now());
        claim.setStatus(status);
        return claim;
    }

    public static ResourceRequestDTO createResourceRequestDTO() {
        ResourceRequestDTO dto = new ResourceRequestDTO();
        dto.setId(UUID.randomUUID());
        dto.setTitle("Emergency Food Needed");
        dto.setDescription("Need food supplies urgently");
        dto.setAddress("123 Main St");
        dto.setSeverityLevel(3);
        dto.setCategoryId(UUID.randomUUID());
        dto.setLatitude(43.4723);
        dto.setLongitude(-80.5449);
        dto.setStatus("OPEN");
        dto.setCreatorFirstName("John");
        dto.setCreatorLastName("Doe");
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    public static UserDTO createUserDTO() {
        return new UserDTO(
                UUID.randomUUID(),
                "test@example.com",
                "John",
                "Doe",
                "555-1234",
                null,
                "A volunteer",
                "123 Main St",
                43.4723,
                -80.5449,
                "VOLUNTEER"
        );
    }

    public static CategoryDTO createCategoryDTO() {
        return new CategoryDTO(UUID.randomUUID(), "Food", "Emergency food supplies");
    }

    public static Point createPoint(double longitude, double latitude) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
    }
}
