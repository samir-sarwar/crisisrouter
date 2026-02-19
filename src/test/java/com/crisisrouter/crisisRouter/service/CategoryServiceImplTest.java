package com.crisisrouter.crisisRouter.service;

import com.crisisrouter.crisisRouter.model.entity.Category;
import com.crisisrouter.crisisRouter.repository.CategoryRepository;
import com.crisisrouter.crisisRouter.service.dto.CategoryDTO;
import com.crisisrouter.crisisRouter.service.impl.CategoryServiceImpl;
import com.crisisrouter.crisisRouter.testutil.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void findAll_returnsListOfCategoryDTOs() {
        Category category = TestDataFactory.createCategory();
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        List<CategoryDTO> result = categoryService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo(category.getName());
        assertThat(result.get(0).getId()).isEqualTo(category.getId());
    }

    @Test
    void findAll_emptyList_returnsEmptyList() {
        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());

        List<CategoryDTO> result = categoryService.findAll();

        assertThat(result).isEmpty();
    }

    @Test
    void findById_existingId_returnsCategoryDTO() {
        Category category = TestDataFactory.createCategory();
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        CategoryDTO result = categoryService.findById(category.getId());

        assertThat(result.getId()).isEqualTo(category.getId());
        assertThat(result.getName()).isEqualTo(category.getName());
        assertThat(result.getDescription()).isEqualTo(category.getDescription());
    }

    @Test
    void findById_nonExistentId_throwsRuntimeException() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findById(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Category not found");
    }
}
