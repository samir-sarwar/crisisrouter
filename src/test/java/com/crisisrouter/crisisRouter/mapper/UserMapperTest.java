package com.crisisrouter.crisisRouter.mapper;

import com.crisisrouter.crisisRouter.model.entity.User;
import com.crisisrouter.crisisRouter.model.entity.UserRole;
import com.crisisrouter.crisisRouter.service.dto.UserDTO;
import com.crisisrouter.crisisRouter.service.mapper.UserMapper;
import com.crisisrouter.crisisRouter.testutil.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        userMapper = Mappers.getMapper(UserMapper.class);
    }

    @Test
    void toDto_mapsAllFields() {
        User user = TestDataFactory.createUser();
        user.setPhoneNumber("555-1234");
        user.setProfilePictureUrl("https://example.com/pic.jpg");
        user.setDescription("A volunteer");
        user.setAddress("123 Main St");
        user.setLatitude(43.47);
        user.setLongitude(-80.54);

        UserDTO dto = userMapper.toDto(user);

        assertThat(dto.getId()).isEqualTo(user.getId());
        assertThat(dto.getEmail()).isEqualTo(user.getEmail());
        assertThat(dto.getFirstName()).isEqualTo(user.getFirstName());
        assertThat(dto.getLastName()).isEqualTo(user.getLastName());
        assertThat(dto.getPhoneNumber()).isEqualTo("555-1234");
        assertThat(dto.getProfilePictureUrl()).isEqualTo("https://example.com/pic.jpg");
        assertThat(dto.getDescription()).isEqualTo("A volunteer");
        assertThat(dto.getAddress()).isEqualTo("123 Main St");
        assertThat(dto.getLatitude()).isEqualTo(43.47);
        assertThat(dto.getLongitude()).isEqualTo(-80.54);
        assertThat(dto.getRole()).isEqualTo("VOLUNTEER");
    }

    @Test
    void toDto_nullUser_returnsNull() {
        assertThat(userMapper.toDto(null)).isNull();
    }

    @Test
    void toEntity_mapsAllFields() {
        UserDTO dto = TestDataFactory.createUserDTO();

        User user = userMapper.toEntity(dto);

        assertThat(user.getId()).isEqualTo(dto.getId());
        assertThat(user.getEmail()).isEqualTo(dto.getEmail());
        assertThat(user.getFirstName()).isEqualTo(dto.getFirstName());
        assertThat(user.getLastName()).isEqualTo(dto.getLastName());
        assertThat(user.getPhoneNumber()).isEqualTo(dto.getPhoneNumber());
    }

    @Test
    void toEntity_nullDTO_returnsNull() {
        assertThat(userMapper.toEntity(null)).isNull();
    }
}
