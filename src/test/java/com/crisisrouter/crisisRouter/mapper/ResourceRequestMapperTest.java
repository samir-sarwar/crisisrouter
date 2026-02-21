package com.crisisrouter.crisisRouter.mapper;

import com.crisisrouter.crisisRouter.model.entity.Category;
import com.crisisrouter.crisisRouter.model.entity.RequestStatus;
import com.crisisrouter.crisisRouter.model.entity.ResourceRequest;
import com.crisisrouter.crisisRouter.model.entity.User;
import com.crisisrouter.crisisRouter.service.dto.ResourceRequestDTO;
import com.crisisrouter.crisisRouter.service.mapper.ResourceRequestMapper;
import com.crisisrouter.crisisRouter.testutil.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceRequestMapperTest {

    private ResourceRequestMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(ResourceRequestMapper.class);
    }

    @Test
    void toDTO_mapsScalarFields() {
        User user = TestDataFactory.createUser();
        Category category = TestDataFactory.createCategory();
        ResourceRequest entity = TestDataFactory.createResourceRequest(user, category);

        ResourceRequestDTO dto = mapper.toDTO(entity);

        assertThat(dto.getId()).isEqualTo(entity.getId());
        assertThat(dto.getTitle()).isEqualTo(entity.getTitle());
        assertThat(dto.getDescription()).isEqualTo(entity.getDescription());
        assertThat(dto.getAddress()).isEqualTo(entity.getAddress());
        assertThat(dto.getSeverityLevel()).isEqualTo(entity.getSeverityLevel());
        assertThat(dto.getStatus()).isEqualTo("OPEN");
        assertThat(dto.getImageUrl()).isEqualTo(entity.getImageUrl());
    }

    @Test
    void toDTO_mapsCreatorNames() {
        User user = TestDataFactory.createUser("test@email.com", "Jane", "Smith");
        Category category = TestDataFactory.createCategory();
        ResourceRequest entity = TestDataFactory.createResourceRequest(user, category);

        ResourceRequestDTO dto = mapper.toDTO(entity);

        assertThat(dto.getCreatorFirstName()).isEqualTo("Jane");
        assertThat(dto.getCreatorLastName()).isEqualTo("Smith");
    }

    @Test
    void toDTO_mapsCategoryId() {
        User user = TestDataFactory.createUser();
        Category category = TestDataFactory.createCategory();
        ResourceRequest entity = TestDataFactory.createResourceRequest(user, category);

        ResourceRequestDTO dto = mapper.toDTO(entity);

        assertThat(dto.getCategoryId()).isEqualTo(category.getId());
    }

    @Test
    void toDTO_mapsPointToLatLng() {
        User user = TestDataFactory.createUser();
        Category category = TestDataFactory.createCategory();
        ResourceRequest entity = TestDataFactory.createResourceRequest(user, category);
        // Location is set to (-80.5449, 43.4723) by TestDataFactory

        ResourceRequestDTO dto = mapper.toDTO(entity);

        assertThat(dto.getLongitude()).isEqualTo(-80.5449);
        assertThat(dto.getLatitude()).isEqualTo(43.4723);
    }

    @Test
    void toDTO_nullLocation_returnsNullLatLng() {
        User user = TestDataFactory.createUser();
        Category category = TestDataFactory.createCategory();
        ResourceRequest entity = TestDataFactory.createResourceRequest(user, category);
        entity.setLatitude(null);
        entity.setLongitude(null);

        ResourceRequestDTO dto = mapper.toDTO(entity);

        assertThat(dto.getLatitude()).isNull();
        assertThat(dto.getLongitude()).isNull();
    }

    @Test
    void toDTO_nullEntity_returnsNull() {
        assertThat(mapper.toDTO(null)).isNull();
    }

    @Test
    void toDTO_nullUser_returnsNullCreatorNames() {
        ResourceRequest entity = TestDataFactory.createResourceRequest(
                TestDataFactory.createUser(), TestDataFactory.createCategory());
        entity.setUser(null);

        ResourceRequestDTO dto = mapper.toDTO(entity);

        assertThat(dto.getCreatorFirstName()).isNull();
        assertThat(dto.getCreatorLastName()).isNull();
    }

    @Test
    void toDTO_nullCategory_returnsNullCategoryId() {
        ResourceRequest entity = TestDataFactory.createResourceRequest(
                TestDataFactory.createUser(), TestDataFactory.createCategory());
        entity.setCategory(null);

        ResourceRequestDTO dto = mapper.toDTO(entity);

        assertThat(dto.getCategoryId()).isNull();
    }

    @Test
    void toEntity_mapsScalarFields() {
        ResourceRequestDTO dto = TestDataFactory.createResourceRequestDTO();

        ResourceRequest entity = mapper.toEntity(dto);

        assertThat(entity.getTitle()).isEqualTo(dto.getTitle());
        assertThat(entity.getDescription()).isEqualTo(dto.getDescription());
        assertThat(entity.getAddress()).isEqualTo(dto.getAddress());
        assertThat(entity.getSeverityLevel()).isEqualTo(dto.getSeverityLevel());
    }

    @Test
    void toEntity_ignoresUserAndCategory() {
        ResourceRequestDTO dto = TestDataFactory.createResourceRequestDTO();

        ResourceRequest entity = mapper.toEntity(dto);

        assertThat(entity.getUser()).isNull();
        assertThat(entity.getCategory()).isNull();
    }

    @Test
    void toEntity_statusProvided_parsesEnum() {
        ResourceRequestDTO dto = TestDataFactory.createResourceRequestDTO();
        dto.setStatus("CLAIMED");

        ResourceRequest entity = mapper.toEntity(dto);

        assertThat(entity.getStatus()).isEqualTo(RequestStatus.CLAIMED);
    }

    @Test
    void toEntity_nullStatus_defaultsToOpen() {
        ResourceRequestDTO dto = TestDataFactory.createResourceRequestDTO();
        dto.setStatus(null);

        ResourceRequest entity = mapper.toEntity(dto);

        assertThat(entity.getStatus()).isEqualTo(RequestStatus.OPEN);
    }

    @Test
    void toEntity_nullDTO_returnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }
}
