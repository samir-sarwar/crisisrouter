package com.crisisrouter.crisisRouter.service;

import com.crisisrouter.crisisRouter.model.entity.*;
import com.crisisrouter.crisisRouter.repository.CategoryRepository;
import com.crisisrouter.crisisRouter.repository.ResourceRequestRepository;
import com.crisisrouter.crisisRouter.repository.UserRepository;
import com.crisisrouter.crisisRouter.service.dto.ResourceRequestDTO;
import com.crisisrouter.crisisRouter.service.impl.ResourceRequestServiceImpl;
import com.crisisrouter.crisisRouter.service.mapper.ResourceRequestMapper;
import com.crisisrouter.crisisRouter.testutil.SecurityTestUtil;
import com.crisisrouter.crisisRouter.testutil.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceRequestServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ResourceRequestRepository requestRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ResourceRequestMapper mapper;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ResourceRequestServiceImpl service;

    @AfterEach
    void tearDown() {
        SecurityTestUtil.clearSecurityContext();
    }

    // ── createRequest ──

    @Test
    void createRequest_validRequest_savesAndReturnsDTO() {
        ResourceRequestDTO inputDto = TestDataFactory.createResourceRequestDTO();
        Category category = TestDataFactory.createCategory();
        User user = TestDataFactory.createUser();
        ResourceRequest entity = TestDataFactory.createResourceRequest(user, category);
        ResourceRequestDTO outputDto = TestDataFactory.createResourceRequestDTO();

        SecurityTestUtil.mockSecurityContext("test@example.com");
        when(categoryRepository.findById(inputDto.getCategoryId())).thenReturn(Optional.of(category));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(mapper.toEntity(inputDto)).thenReturn(entity);
        when(requestRepository.save(any(ResourceRequest.class))).thenReturn(entity);
        when(mapper.toDTO(entity)).thenReturn(outputDto);

        ResourceRequestDTO result = service.createRequest(inputDto, null);

        assertThat(result).isNotNull();
        verify(requestRepository).save(any(ResourceRequest.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/requests"), any(ResourceRequestDTO.class));
    }

    @Test
    void createRequest_withImage_uploadsAndSetsImageUrl() {
        ResourceRequestDTO inputDto = TestDataFactory.createResourceRequestDTO();
        Category category = TestDataFactory.createCategory();
        User user = TestDataFactory.createUser();
        ResourceRequest entity = TestDataFactory.createResourceRequest(user, category);
        MultipartFile image = mock(MultipartFile.class);

        SecurityTestUtil.mockSecurityContext("test@example.com");
        when(categoryRepository.findById(inputDto.getCategoryId())).thenReturn(Optional.of(category));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(mapper.toEntity(inputDto)).thenReturn(entity);
        when(image.isEmpty()).thenReturn(false);
        when(fileStorageService.storeFile(image)).thenReturn("https://s3.com/img.jpg");
        when(requestRepository.save(any(ResourceRequest.class))).thenReturn(entity);
        when(mapper.toDTO(entity)).thenReturn(TestDataFactory.createResourceRequestDTO());

        service.createRequest(inputDto, image);

        verify(fileStorageService).storeFile(image);
        assertThat(entity.getImageUrl()).isEqualTo("https://s3.com/img.jpg");
    }

    @Test
    void createRequest_withoutImage_noUpload() {
        ResourceRequestDTO inputDto = TestDataFactory.createResourceRequestDTO();
        Category category = TestDataFactory.createCategory();
        User user = TestDataFactory.createUser();
        ResourceRequest entity = TestDataFactory.createResourceRequest(user, category);

        SecurityTestUtil.mockSecurityContext("test@example.com");
        when(categoryRepository.findById(inputDto.getCategoryId())).thenReturn(Optional.of(category));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(mapper.toEntity(inputDto)).thenReturn(entity);
        when(requestRepository.save(any(ResourceRequest.class))).thenReturn(entity);
        when(mapper.toDTO(entity)).thenReturn(TestDataFactory.createResourceRequestDTO());

        service.createRequest(inputDto, null);

        verify(fileStorageService, never()).storeFile(any());
    }

    @Test
    void createRequest_categoryNotFound_throwsRuntimeException() {
        ResourceRequestDTO dto = TestDataFactory.createResourceRequestDTO();
        when(categoryRepository.findById(dto.getCategoryId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createRequest(dto, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    void createRequest_userNotAuthenticated_throwsRuntimeException() {
        ResourceRequestDTO dto = TestDataFactory.createResourceRequestDTO();
        Category category = TestDataFactory.createCategory();

        SecurityTestUtil.mockSecurityContextNonOidc();
        when(categoryRepository.findById(dto.getCategoryId())).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> service.createRequest(dto, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("authenticated");
    }

    @Test
    void createRequest_userNotFoundByEmail_throwsRuntimeException() {
        ResourceRequestDTO dto = TestDataFactory.createResourceRequestDTO();
        Category category = TestDataFactory.createCategory();

        SecurityTestUtil.mockSecurityContext("missing@example.com");
        when(categoryRepository.findById(dto.getCategoryId())).thenReturn(Optional.of(category));
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createRequest(dto, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void createRequest_otherCategoryWithoutCustomCategory_throwsIllegalArgumentException() {
        ResourceRequestDTO dto = TestDataFactory.createResourceRequestDTO();
        dto.setCustomCategory(null);
        Category otherCategory = TestDataFactory.createCategory("Other", "Other category");
        User user = TestDataFactory.createUser();

        SecurityTestUtil.mockSecurityContext("test@example.com");
        when(categoryRepository.findById(dto.getCategoryId())).thenReturn(Optional.of(otherCategory));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.createRequest(dto, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("custom category description");
    }

    @Test
    void createRequest_otherCategoryWithBlankCustomCategory_throwsIllegalArgumentException() {
        ResourceRequestDTO dto = TestDataFactory.createResourceRequestDTO();
        dto.setCustomCategory("   ");
        Category otherCategory = TestDataFactory.createCategory("Other", "Other category");
        User user = TestDataFactory.createUser();

        SecurityTestUtil.mockSecurityContext("test@example.com");
        when(categoryRepository.findById(dto.getCategoryId())).thenReturn(Optional.of(otherCategory));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.createRequest(dto, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("custom category description");
    }

    @Test
    void createRequest_otherCategoryWithCustomCategory_succeeds() {
        ResourceRequestDTO dto = TestDataFactory.createResourceRequestDTO();
        dto.setCustomCategory("Water filters");
        Category otherCategory = TestDataFactory.createCategory("Other", "Other category");
        User user = TestDataFactory.createUser();
        ResourceRequest entity = TestDataFactory.createResourceRequest(user, otherCategory);

        SecurityTestUtil.mockSecurityContext("test@example.com");
        when(categoryRepository.findById(dto.getCategoryId())).thenReturn(Optional.of(otherCategory));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(requestRepository.save(any(ResourceRequest.class))).thenReturn(entity);
        when(mapper.toDTO(entity)).thenReturn(dto);

        ResourceRequestDTO result = service.createRequest(dto, null);

        assertThat(result).isNotNull();
    }

    @Test
    void createRequest_setsStatusToOpen() {
        ResourceRequestDTO dto = TestDataFactory.createResourceRequestDTO();
        Category category = TestDataFactory.createCategory();
        User user = TestDataFactory.createUser();
        ResourceRequest entity = TestDataFactory.createResourceRequest(user, category);
        entity.setStatus(null); // Ensure it's not pre-set

        SecurityTestUtil.mockSecurityContext("test@example.com");
        when(categoryRepository.findById(dto.getCategoryId())).thenReturn(Optional.of(category));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(requestRepository.save(any(ResourceRequest.class))).thenReturn(entity);
        when(mapper.toDTO(entity)).thenReturn(dto);

        service.createRequest(dto, null);

        assertThat(entity.getStatus()).isEqualTo(RequestStatus.OPEN);
    }

    // ── findNearby ──

    @Test
    void findNearby_returnsListOfDTOs() {
        User user = TestDataFactory.createUser();
        Category category = TestDataFactory.createCategory();
        ResourceRequest entity = TestDataFactory.createResourceRequest(user, category);
        ResourceRequestDTO dto = TestDataFactory.createResourceRequestDTO();

        when(requestRepository.findNearbyRequests(any(), eq(5000.0))).thenReturn(List.of(entity));
        when(mapper.toDTO(entity)).thenReturn(dto);

        List<ResourceRequestDTO> result = service.findNearby(-80.5, 43.5, 5000.0);

        assertThat(result).hasSize(1);
        verify(requestRepository).findNearbyRequests(any(), eq(5000.0));
    }

    // ── getById ──

    @Test
    void getById_existingId_returnsDTO() {
        UUID id = UUID.randomUUID();
        User user = TestDataFactory.createUser();
        Category category = TestDataFactory.createCategory();
        ResourceRequest entity = TestDataFactory.createResourceRequest(user, category);
        ResourceRequestDTO dto = TestDataFactory.createResourceRequestDTO();

        when(requestRepository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toDTO(entity)).thenReturn(dto);

        ResourceRequestDTO result = service.getById(id);

        assertThat(result).isNotNull();
    }

    @Test
    void getById_nonExistentId_throwsRuntimeException() {
        UUID id = UUID.randomUUID();
        when(requestRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Request not found");
    }

    // ── updateStatus ──

    @Test
    void updateStatus_validStatus_updatesAndReturns() {
        UUID id = UUID.randomUUID();
        User user = TestDataFactory.createUser();
        Category category = TestDataFactory.createCategory();
        ResourceRequest entity = TestDataFactory.createResourceRequest(user, category);
        ResourceRequestDTO dto = TestDataFactory.createResourceRequestDTO();

        when(requestRepository.findById(id)).thenReturn(Optional.of(entity));
        when(requestRepository.save(entity)).thenReturn(entity);
        when(mapper.toDTO(entity)).thenReturn(dto);

        service.updateStatus(id, "CLAIMED");

        assertThat(entity.getStatus()).isEqualTo(RequestStatus.CLAIMED);
    }

    @Test
    void updateStatus_invalidStatusString_throwsIllegalArgumentException() {
        UUID id = UUID.randomUUID();
        User user = TestDataFactory.createUser();
        Category category = TestDataFactory.createCategory();
        ResourceRequest entity = TestDataFactory.createResourceRequest(user, category);

        when(requestRepository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.updateStatus(id, "INVALID_STATUS"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid status");
    }

    @Test
    void updateStatus_requestNotFound_throwsRuntimeException() {
        UUID id = UUID.randomUUID();
        when(requestRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus(id, "OPEN"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Request not found");
    }

    // ── getMyRequests ──

    @Test
    void getMyRequests_authenticated_returnsUserRequests() {
        User user = TestDataFactory.createUser();
        Category category = TestDataFactory.createCategory();
        ResourceRequest entity = TestDataFactory.createResourceRequest(user, category);
        ResourceRequestDTO dto = TestDataFactory.createResourceRequestDTO();

        SecurityTestUtil.mockSecurityContext("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(requestRepository.findByUserId(user.getId())).thenReturn(List.of(entity));
        when(mapper.toDTO(entity)).thenReturn(dto);

        List<ResourceRequestDTO> result = service.getMyRequests();

        assertThat(result).hasSize(1);
    }

    @Test
    void getMyRequests_notAuthenticated_throwsRuntimeException() {
        SecurityTestUtil.mockSecurityContextNonOidc();

        assertThatThrownBy(() -> service.getMyRequests())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("authenticated");
    }

    @Test
    void getMyRequests_userNotInDb_throwsRuntimeException() {
        SecurityTestUtil.mockSecurityContext("missing@example.com");
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyRequests())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    // ── updateRequest ──

    @Test
    void updateRequest_openRequest_updatesFields() {
        UUID id = UUID.randomUUID();
        User user = TestDataFactory.createUser();
        Category category = TestDataFactory.createCategory();
        ResourceRequest entity = TestDataFactory.createResourceRequest(user, category, RequestStatus.OPEN);
        ResourceRequestDTO updateDto = new ResourceRequestDTO();
        updateDto.setTitle("Updated Title");
        updateDto.setDescription("Updated Description");
        updateDto.setAddress("456 New St");
        updateDto.setSeverityLevel(4);

        when(requestRepository.findById(id)).thenReturn(Optional.of(entity));
        when(requestRepository.save(entity)).thenReturn(entity);
        when(mapper.toDTO(entity)).thenReturn(TestDataFactory.createResourceRequestDTO());

        service.updateRequest(id, updateDto);

        assertThat(entity.getTitle()).isEqualTo("Updated Title");
        assertThat(entity.getDescription()).isEqualTo("Updated Description");
        assertThat(entity.getAddress()).isEqualTo("456 New St");
        assertThat(entity.getSeverityLevel()).isEqualTo(4);
    }

    @Test
    void updateRequest_claimedRequest_throwsIllegalStateException() {
        UUID id = UUID.randomUUID();
        User user = TestDataFactory.createUser();
        Category category = TestDataFactory.createCategory();
        ResourceRequest entity = TestDataFactory.createResourceRequest(user, category, RequestStatus.CLAIMED);

        when(requestRepository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.updateRequest(id, new ResourceRequestDTO()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot edit");
    }

    @Test
    void updateRequest_requestNotFound_throwsRuntimeException() {
        UUID id = UUID.randomUUID();
        when(requestRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateRequest(id, new ResourceRequestDTO()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Request not found");
    }

    @Test
    void updateRequest_updatesLocation_whenLatLngProvided() {
        UUID id = UUID.randomUUID();
        User user = TestDataFactory.createUser();
        Category category = TestDataFactory.createCategory();
        ResourceRequest entity = TestDataFactory.createResourceRequest(user, category, RequestStatus.OPEN);
        ResourceRequestDTO updateDto = new ResourceRequestDTO();
        updateDto.setLatitude(44.0);
        updateDto.setLongitude(-79.0);

        when(requestRepository.findById(id)).thenReturn(Optional.of(entity));
        when(requestRepository.save(entity)).thenReturn(entity);
        when(mapper.toDTO(entity)).thenReturn(TestDataFactory.createResourceRequestDTO());

        service.updateRequest(id, updateDto);

        assertThat(entity.getLocation()).isNotNull();
        assertThat(entity.getLocation().getX()).isEqualTo(-79.0);
        assertThat(entity.getLocation().getY()).isEqualTo(44.0);
    }

    @Test
    void updateRequest_invalidSeverity_doesNotUpdate() {
        UUID id = UUID.randomUUID();
        User user = TestDataFactory.createUser();
        Category category = TestDataFactory.createCategory();
        ResourceRequest entity = TestDataFactory.createResourceRequest(user, category, RequestStatus.OPEN);
        entity.setSeverityLevel(3);

        ResourceRequestDTO updateDto = new ResourceRequestDTO();
        updateDto.setSeverityLevel(0); // Invalid: below 1

        when(requestRepository.findById(id)).thenReturn(Optional.of(entity));
        when(requestRepository.save(entity)).thenReturn(entity);
        when(mapper.toDTO(entity)).thenReturn(TestDataFactory.createResourceRequestDTO());

        service.updateRequest(id, updateDto);

        assertThat(entity.getSeverityLevel()).isEqualTo(3); // Unchanged
    }
}
