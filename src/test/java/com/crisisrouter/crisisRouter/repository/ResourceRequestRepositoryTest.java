package com.crisisrouter.crisisRouter.repository;

import com.crisisrouter.crisisRouter.model.entity.*;
import com.crisisrouter.crisisRouter.testconfig.TestcontainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ResourceRequestRepositoryTest {

    @Autowired
    private ResourceRequestRepository resourceRequestRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    private User user;
    private Category category;

    @BeforeEach
    void setUp() {
        user = userRepository.saveAndFlush(User.builder()
                .email("test-" + UUID.randomUUID() + "@test.com")
                .firstName("Test").lastName("User")
                .role(UserRole.VOLUNTEER).build());

        category = categoryRepository.saveAndFlush(Category.builder()
                .name("Food").description("Food supplies").build());
    }

    private ResourceRequest createAndSaveRequest(String title, double lng, double lat, RequestStatus status) {
        Point location = geometryFactory.createPoint(new Coordinate(lng, lat));
        return resourceRequestRepository.saveAndFlush(ResourceRequest.builder()
                .user(user).category(category)
                .title(title).description("Description")
                .address("123 St").severityLevel(3)
                .location(location).status(status).build());
    }

    @Test
    void findByUserId_returnsRequests() {
        createAndSaveRequest("Request 1", -80.5449, 43.4723, RequestStatus.OPEN);

        List<ResourceRequest> result = resourceRequestRepository.findByUserId(user.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Request 1");
    }

    @Test
    void findByUserId_noRequests_returnsEmptyList() {
        List<ResourceRequest> result = resourceRequestRepository.findByUserId(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    @Test
    void findNearbyRequests_withinRadius_returnsResults() {
        // University of Waterloo coordinates
        createAndSaveRequest("Nearby Request", -80.5449, 43.4723, RequestStatus.OPEN);

        // Search from a point ~1km away
        Point searchPoint = geometryFactory.createPoint(new Coordinate(-80.5400, 43.4700));

        List<ResourceRequest> result = resourceRequestRepository.findNearbyRequests(searchPoint, 5000.0);

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getTitle()).isEqualTo("Nearby Request");
    }

    @Test
    void findNearbyRequests_outsideRadius_returnsEmpty() {
        // University of Waterloo
        createAndSaveRequest("Far Request", -80.5449, 43.4723, RequestStatus.OPEN);

        // Search from a very distant point (London, UK ~5500km away)
        // ST_DWithin with geometry type uses degrees, so use a tiny radius
        Point searchPoint = geometryFactory.createPoint(new Coordinate(-0.1276, 51.5074));

        List<ResourceRequest> result = resourceRequestRepository.findNearbyRequests(searchPoint, 0.001);

        assertThat(result).isEmpty();
    }

    @Test
    void findNearbyRequests_orderedByDistance() {
        // Closer request
        createAndSaveRequest("Close Request", -80.5400, 43.4700, RequestStatus.OPEN);
        // Farther request
        createAndSaveRequest("Far Request", -80.5600, 43.4900, RequestStatus.OPEN);

        Point searchPoint = geometryFactory.createPoint(new Coordinate(-80.5390, 43.4695));

        List<ResourceRequest> result = resourceRequestRepository.findNearbyRequests(searchPoint, 50000.0);

        assertThat(result).hasSizeGreaterThanOrEqualTo(2);
        assertThat(result.get(0).getTitle()).isEqualTo("Close Request");
        assertThat(result.get(1).getTitle()).isEqualTo("Far Request");
    }

    @Test
    void findByStatus_returnsMatchingRequests() {
        createAndSaveRequest("Open Request", -80.5449, 43.4723, RequestStatus.OPEN);
        createAndSaveRequest("Claimed Request", -80.5400, 43.4700, RequestStatus.CLAIMED);

        List<ResourceRequest> openResults = resourceRequestRepository.findByStatus(RequestStatus.OPEN);

        assertThat(openResults).extracting(ResourceRequest::getTitle).contains("Open Request");
    }
}
