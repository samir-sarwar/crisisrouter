package com.crisisrouter.crisisRouter.service;

import com.crisisrouter.crisisRouter.model.entity.User;
import com.crisisrouter.crisisRouter.repository.UserRepository;
import com.crisisrouter.crisisRouter.service.dto.UserDTO;
import com.crisisrouter.crisisRouter.service.impl.UserServiceImpl;
import com.crisisrouter.crisisRouter.service.mapper.UserMapper;
import com.crisisrouter.crisisRouter.testutil.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void getUserByEmail_existingEmail_returnsUser() {
        User user = TestDataFactory.createUser();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        Optional<User> result = userService.getUserByEmail("test@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void getUserByEmail_nonExistent_returnsEmpty() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        Optional<User> result = userService.getUserByEmail("unknown@example.com");

        assertThat(result).isEmpty();
    }

    @Test
    void updateUser_validData_updatesFieldsAndReturnsDTO() {
        User user = TestDataFactory.createUser();
        UserDTO dto = TestDataFactory.createUserDTO();
        dto.setFirstName("Updated");
        dto.setLastName("Name");
        dto.setPhoneNumber("999-8888");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(any(User.class))).thenReturn(dto);

        UserDTO result = userService.updateUser("test@example.com", dto, null);

        assertThat(result.getFirstName()).isEqualTo("Updated");
        verify(userRepository).save(user);
        assertThat(user.getFirstName()).isEqualTo("Updated");
        assertThat(user.getLastName()).isEqualTo("Name");
        assertThat(user.getPhoneNumber()).isEqualTo("999-8888");
    }

    @Test
    void updateUser_withImage_uploadsToS3AndSetsUrl() {
        User user = TestDataFactory.createUser();
        UserDTO dto = TestDataFactory.createUserDTO();
        MultipartFile image = mock(MultipartFile.class);

        when(image.isEmpty()).thenReturn(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(fileStorageService.storeFile(image)).thenReturn("https://s3.com/pic.jpg");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(any(User.class))).thenReturn(dto);

        userService.updateUser("test@example.com", dto, image);

        verify(fileStorageService).storeFile(image);
        assertThat(user.getProfilePictureUrl()).isEqualTo("https://s3.com/pic.jpg");
    }

    @Test
    void updateUser_nullImage_doesNotUpload() {
        User user = TestDataFactory.createUser();
        UserDTO dto = TestDataFactory.createUserDTO();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(any(User.class))).thenReturn(dto);

        userService.updateUser("test@example.com", dto, null);

        verify(fileStorageService, never()).storeFile(any());
    }

    @Test
    void updateUser_emailNotFound_throwsRuntimeException() {
        UserDTO dto = TestDataFactory.createUserDTO();
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser("unknown@example.com", dto, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void updateUser_nullDtoFields_doesNotOverwriteExisting() {
        User user = TestDataFactory.createUser();
        user.setFirstName("Original");
        user.setPhoneNumber("111-2222");

        UserDTO dto = new UserDTO();
        // All fields null

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(any(User.class))).thenReturn(new UserDTO());

        userService.updateUser("test@example.com", dto, null);

        assertThat(user.getFirstName()).isEqualTo("Original");
        assertThat(user.getPhoneNumber()).isEqualTo("111-2222");
    }

    @Test
    void updateUser_emptyImage_doesNotUpload() {
        User user = TestDataFactory.createUser();
        UserDTO dto = TestDataFactory.createUserDTO();
        MultipartFile image = mock(MultipartFile.class);
        when(image.isEmpty()).thenReturn(true);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(any(User.class))).thenReturn(dto);

        userService.updateUser("test@example.com", dto, image);

        verify(fileStorageService, never()).storeFile(any());
    }
}
