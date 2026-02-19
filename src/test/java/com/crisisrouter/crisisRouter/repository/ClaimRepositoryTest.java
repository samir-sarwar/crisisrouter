package com.crisisrouter.crisisRouter.repository;

import com.crisisrouter.crisisRouter.model.entity.*;
import com.crisisrouter.crisisRouter.testconfig.TestcontainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ClaimRepositoryTest {

    @Autowired
    private ClaimRepository claimRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ResourceRequestRepository resourceRequestRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    private User volunteer;
    private ResourceRequest request;

    @BeforeEach
    void setUp() {
        User creator = userRepository.saveAndFlush(User.builder()
                .email("creator-" + UUID.randomUUID() + "@test.com")
                .firstName("Creator").lastName("User")
                .role(UserRole.VOLUNTEER).build());

        volunteer = userRepository.saveAndFlush(User.builder()
                .email("volunteer-" + UUID.randomUUID() + "@test.com")
                .firstName("Vol").lastName("Teer")
                .role(UserRole.VOLUNTEER).build());

        Category category = categoryRepository.saveAndFlush(Category.builder()
                .name("Food").description("Food supplies").build());

        request = resourceRequestRepository.saveAndFlush(ResourceRequest.builder()
                .user(creator).category(category)
                .title("Help").description("Need help")
                .address("123 St").severityLevel(3)
                .status(RequestStatus.OPEN).build());
    }

    @Test
    void findByUserId_existingVolunteer_returnsClaims() {
        Claim claim = Claim.builder()
                .request(request).user(volunteer)
                .claimedAt(LocalDateTime.now()).status("ACTIVE").build();
        claimRepository.saveAndFlush(claim);

        List<Claim> result = claimRepository.findByUserId(volunteer.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUser().getId()).isEqualTo(volunteer.getId());
    }

    @Test
    void findByUserId_noClaimsForUser_returnsEmptyList() {
        List<Claim> result = claimRepository.findByUserId(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    @Test
    void findByRequestId_existingRequest_returnsClaims() {
        Claim claim = Claim.builder()
                .request(request).user(volunteer)
                .claimedAt(LocalDateTime.now()).status("ACTIVE").build();
        claimRepository.saveAndFlush(claim);

        List<Claim> result = claimRepository.findByRequestId(request.getId());

        assertThat(result).hasSize(1);
    }

    @Test
    void findByRequestId_noClaimsForRequest_returnsEmptyList() {
        List<Claim> result = claimRepository.findByRequestId(UUID.randomUUID());

        assertThat(result).isEmpty();
    }
}
