package com.crisisrouter.crisisRouter.repository;
import com.crisisrouter.crisisRouter.model.entity.*;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest // Use the full context to avoid annotation resolution issues
 // This rolls back the database changes after each test
public class ResourceRequestRepositoryTest {

    @Autowired
    private ResourceRequestRepository requestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired CategoryRepository categoryRepository;

    private final GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);
    @Test
    void testFindNearbyRequests() {
        // 1. Create and save a test user
        User creator = new User();
        creator.setEmail("test@waterloo.ca");
        creator.setPhoneNumber("123456");
        creator.setRole(UserRole.VOLUNTEER);
        userRepository.save(creator);
        Category foodCategory = new Category();
        foodCategory.setName("Food");
        foodCategory.setDescription("Emergency food and water supplies");
        categoryRepository.save(foodCategory);

        // 2. Create a request at a specific point (e.g., University of Waterloo)
        Point uWaterloo = factory.createPoint(new Coordinate(-80.5449, 43.4723));
        ResourceRequest request = new ResourceRequest();
        request.setTitle("Emergency Food");
        request.setDescription("Immediate assistance required."); // Must be a String
        request.setAddress("200 University Ave W");            // Must be a String
        request.setSeverityLevel(5);                            // Must be an Integer
        request.setCategory(foodCategory); // Must be a Category object
        request.setCustomCategory("Standard Delivery");         // Must be a String
        request.setLocation(uWaterloo);                         // Must be a JTS Point
        request.setUser(creator);                            // Must be a User object
        request.setStatus(RequestStatus.OPEN);                  // Must be a RequestStatus enum

        requestRepository.save(request);
        // 3. Search from a point 1km away
        Point nearbyPoint = factory.createPoint(new Coordinate(-80.5500, 43.4700));
        List<ResourceRequest> found = requestRepository.findNearbyRequests(nearbyPoint, 5000); // 5km radius

        // 4. Assert
        assertThat(found).isNotEmpty();
        assertThat(found.get(0).getTitle()).isEqualTo("Emergency Food");
    }
}