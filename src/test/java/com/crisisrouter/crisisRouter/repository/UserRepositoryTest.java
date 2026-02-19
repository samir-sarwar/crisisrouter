package com.crisisrouter.crisisRouter.repository;

import com.crisisrouter.crisisRouter.model.entity.User;
import com.crisisrouter.crisisRouter.model.entity.UserRole;
import com.crisisrouter.crisisRouter.testconfig.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User persistUser(String email) {
        User user = User.builder()
                .email(email)
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.VOLUNTEER)
                .build();
        return userRepository.saveAndFlush(user);
    }

    @Test
    void findByEmail_existingUser_returnsUser() {
        persistUser("findme@test.com");

        Optional<User> result = userRepository.findByEmail("findme@test.com");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("findme@test.com");
    }

    @Test
    void findByEmail_nonExistent_returnsEmpty() {
        Optional<User> result = userRepository.findByEmail("doesnotexist@test.com");

        assertThat(result).isEmpty();
    }

    @Test
    void existsByEmail_existingUser_returnsTrue() {
        persistUser("exists@test.com");

        assertThat(userRepository.existsByEmail("exists@test.com")).isTrue();
    }

    @Test
    void existsByEmail_nonExistent_returnsFalse() {
        assertThat(userRepository.existsByEmail("nope@test.com")).isFalse();
    }
}
