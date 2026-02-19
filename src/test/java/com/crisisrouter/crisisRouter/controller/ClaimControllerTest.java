package com.crisisrouter.crisisRouter.controller;

import com.crisisrouter.crisisRouter.service.ClaimService;
import com.crisisrouter.crisisRouter.service.dto.ClaimDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClaimController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class ClaimControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClaimService claimService;

    private ClaimDTO sampleClaimDTO() {
        ClaimDTO dto = new ClaimDTO();
        dto.setId(UUID.randomUUID());
        dto.setResourceRequestId(UUID.randomUUID());
        dto.setVolunteerId(UUID.randomUUID());
        dto.setClaimedAt(LocalDateTime.now());
        dto.setStatus("ACTIVE");
        return dto;
    }

    @Test
    void claimRequest_returns200() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        ClaimDTO dto = sampleClaimDTO();

        when(claimService.claimRequest(requestId, volunteerId)).thenReturn(dto);

        mockMvc.perform(post("/api/claims/request/{requestId}", requestId)
                        .param("volunteerId", volunteerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void claimRequest_notOpen_returns404() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();

        when(claimService.claimRequest(requestId, volunteerId))
                .thenThrow(new IllegalStateException("Request is not OPEN"));

        mockMvc.perform(post("/api/claims/request/{requestId}", requestId)
                        .param("volunteerId", volunteerId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getVolunteerClaims_returns200() throws Exception {
        UUID volunteerId = UUID.randomUUID();
        when(claimService.getVolunteerClaims(volunteerId)).thenReturn(List.of(sampleClaimDTO()));

        mockMvc.perform(get("/api/claims/volunteer/{volunteerId}", volunteerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void getClaimsByRequest_returns200() throws Exception {
        UUID requestId = UUID.randomUUID();
        when(claimService.getClaimsByRequestId(requestId)).thenReturn(List.of(sampleClaimDTO()));

        mockMvc.perform(get("/api/claims/request/{requestId}", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getMyClaims_returns200() throws Exception {
        when(claimService.getMyClaims()).thenReturn(List.of(sampleClaimDTO()));

        mockMvc.perform(get("/api/claims/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void completeClaim_returns204() throws Exception {
        UUID claimId = UUID.randomUUID();
        doNothing().when(claimService).completeClaim(claimId);

        mockMvc.perform(patch("/api/claims/{claimId}/complete", claimId))
                .andExpect(status().isNoContent());
    }

    @Test
    void completeClaim_notFound_returns404() throws Exception {
        UUID claimId = UUID.randomUUID();
        doThrow(new RuntimeException("Claim not found")).when(claimService).completeClaim(claimId);

        mockMvc.perform(patch("/api/claims/{claimId}/complete", claimId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Claim not found"));
    }

    @Test
    void dropClaim_returns204() throws Exception {
        UUID claimId = UUID.randomUUID();
        doNothing().when(claimService).dropClaim(claimId);

        mockMvc.perform(patch("/api/claims/{claimId}/drop", claimId))
                .andExpect(status().isNoContent());
    }

    @Test
    void dropClaim_notActive_returns404() throws Exception {
        UUID claimId = UUID.randomUUID();
        doThrow(new IllegalStateException("Can only drop ACTIVE")).when(claimService).dropClaim(claimId);

        mockMvc.perform(patch("/api/claims/{claimId}/drop", claimId))
                .andExpect(status().isNotFound());
    }
}
