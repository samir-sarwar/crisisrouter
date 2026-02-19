package com.crisisrouter.crisisRouter.controller;

import com.crisisrouter.crisisRouter.service.ResourceRequestService;
import com.crisisrouter.crisisRouter.service.dto.ResourceRequestDTO;
import com.crisisrouter.crisisRouter.testutil.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ResourceRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class ResourceRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private ResourceRequestService resourceRequestService;

    @Test
    void createRequest_validMultipart_returns201() throws Exception {
        ResourceRequestDTO dto = TestDataFactory.createResourceRequestDTO();
        when(resourceRequestService.createRequest(any(), any())).thenReturn(dto);

        dto.setCreatedAt(null);
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(dto));
        MockMultipartFile imagePart = new MockMultipartFile(
                "image", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/requests")
                        .file(requestPart)
                        .file(imagePart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(dto.getTitle()));
    }

    @Test
    void createRequest_withoutImage_returns201() throws Exception {
        ResourceRequestDTO dto = TestDataFactory.createResourceRequestDTO();
        when(resourceRequestService.createRequest(any(), any())).thenReturn(dto);

        dto.setCreatedAt(null);
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(dto));

        mockMvc.perform(multipart("/api/requests")
                        .file(requestPart))
                .andExpect(status().isCreated());
    }

    @Test
    void createRequest_serviceThrowsIllegalArg_returns400() throws Exception {
        ResourceRequestDTO dto = TestDataFactory.createResourceRequestDTO();
        when(resourceRequestService.createRequest(any(), any()))
                .thenThrow(new IllegalArgumentException("custom category description must be provided"));

        dto.setCreatedAt(null);
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(dto));

        mockMvc.perform(multipart("/api/requests")
                        .file(requestPart))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("custom category description must be provided"));
    }

    @Test
    void getMyRequests_returns200() throws Exception {
        when(resourceRequestService.getMyRequests()).thenReturn(List.of(TestDataFactory.createResourceRequestDTO()));

        mockMvc.perform(get("/api/requests/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").exists());
    }

    @Test
    void getNearbyRequests_returns200() throws Exception {
        when(resourceRequestService.findNearby(-80.5, 43.5, 5000.0))
                .thenReturn(List.of(TestDataFactory.createResourceRequestDTO()));

        mockMvc.perform(get("/api/requests/nearby")
                        .param("longitude", "-80.5")
                        .param("latitude", "43.5")
                        .param("radiusInMeters", "5000.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").exists());
    }

    @Test
    void getById_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        ResourceRequestDTO dto = TestDataFactory.createResourceRequestDTO();
        when(resourceRequestService.getById(id)).thenReturn(dto);

        mockMvc.perform(get("/api/requests/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(dto.getTitle()));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(resourceRequestService.getById(id)).thenThrow(new RuntimeException("Request not found"));

        mockMvc.perform(get("/api/requests/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Request not found"));
    }

    @Test
    void updateStatus_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        ResourceRequestDTO dto = TestDataFactory.createResourceRequestDTO();
        dto.setStatus("CLAIMED");
        when(resourceRequestService.updateStatus(id, "CLAIMED")).thenReturn(dto);

        mockMvc.perform(patch("/api/requests/{id}", id)
                        .param("status", "CLAIMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLAIMED"));
    }

    @Test
    void updateStatus_invalidStatus_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        when(resourceRequestService.updateStatus(id, "INVALID"))
                .thenThrow(new IllegalArgumentException("Invalid status: INVALID"));

        mockMvc.perform(patch("/api/requests/{id}", id)
                        .param("status", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid status: INVALID"));
    }

    @Test
    void updateRequest_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        ResourceRequestDTO dto = TestDataFactory.createResourceRequestDTO();
        when(resourceRequestService.updateRequest(eq(id), any())).thenReturn(dto);

        dto.setCreatedAt(null);
        mockMvc.perform(put("/api/requests/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(dto.getTitle()));
    }
}
