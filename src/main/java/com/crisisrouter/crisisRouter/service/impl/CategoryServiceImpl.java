package com.crisisrouter.crisisRouter.service.impl;

import com.crisisrouter.crisisRouter.model.entity.Category;
import com.crisisrouter.crisisRouter.repository.ResourceRequestRepository;
import com.crisisrouter.crisisRouter.repository.CategoryRepository;
import com.crisisrouter.crisisRouter.service.CategoryService;
import com.crisisrouter.crisisRouter.service.dto.CategoryDTO;
import com.crisisrouter.crisisRouter.service.mapper.ResourceRequestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public List<CategoryDTO> findAll(){
        return categoryRepository.findAll().stream()
                .map(category -> new CategoryDTO(category.getId(), category.getName(), category.getDescription()))
                .collect(Collectors.toList());
    }


    @Override
    public CategoryDTO findById(UUID id){
        Category category = categoryRepository.findById(id).orElseThrow(()
                -> new RuntimeException ("Category not found"));

        return new CategoryDTO(category.getId(), category.getName(), category.getDescription());

    }

}
