package com.crisisrouter.crisisRouter.controller;
import com.crisisrouter.crisisRouter.model.entity.*;
import com.crisisrouter.crisisRouter.repository.ResourceRequestRepository;
import com.crisisrouter.crisisRouter.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest // Starts the full application context
@AutoConfigureMockMvc // Gives us the 'mockMvc' tool to send requests
@Transactional // Rollback DB changes after each test so they don't overla
@ActiveProfiles("test")
class ClaimControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ResourceRequestRepository requestRepository;
    @Autowired private EntityManager entityManager;

    @Test
    void shouldClaimRequestSuccessfully() throws Exception {
        // 1. PREP DATABASE (We need real data in H2)
        User volunteer = new User();
        volunteer.setEmail("hero@test.com");
        volunteer.setRole(UserRole.VOLUNTEER);
        volunteer.setPhoneNumber("1234567890");
        volunteer.setCreatedAt(LocalDateTime.now());

        userRepository.save(volunteer);

        entityManager.flush();

        ResourceRequest request = new ResourceRequest();
        request.setTitle("Help me");
        request.setDescription("Emergency");
        request.setAddress("123 Main St");
        request.setSeverityLevel(5);
        request.setStatus(RequestStatus.OPEN);
        request.setCreatedAt(LocalDateTime.now());
        request.setCategory(null); // Skipping for simplicity, or mock a category
        request.setUser(volunteer); // Creator
        requestRepository.save(request);

        entityManager.flush();

        // 2. PERFORM REQUEST
        mockMvc.perform(post("/api/claims/request/" + request.getId())
                        .param("volunteerId", volunteer.getId().toString()))

                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.resourceRequestId").value(request.getId().toString()));
    }
}