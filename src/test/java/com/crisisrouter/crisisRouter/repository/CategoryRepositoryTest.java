package com.crisisrouter.crisisRouter.repository;

import com.crisisrouter.crisisRouter.model.entity.Category;
import com.crisisrouter.crisisRouter.testconfig.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void findByName_existingCategory_returnsCategory() {
        String uniqueName = "TestCategory-" + UUID.randomUUID();
        Category category = Category.builder()
                .name(uniqueName)
                .description("Test category")
                .build();
        categoryRepository.saveAndFlush(category);

        Optional<Category> result = categoryRepository.findByName(uniqueName);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo(uniqueName);
    }

    @Test
    void findByName_nonExistent_returnsEmpty() {
        Optional<Category> result = categoryRepository.findByName("NonExistent-" + UUID.randomUUID());

        assertThat(result).isEmpty();
    }
}
