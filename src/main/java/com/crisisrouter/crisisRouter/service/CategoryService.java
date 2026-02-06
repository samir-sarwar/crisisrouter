package com.crisisrouter.crisisRouter.service;

import com.crisisrouter.crisisRouter.service.dto.CategoryDTO;
import java.util.List;
import java.util.UUID;

public interface CategoryService {
    List<CategoryDTO> findAll();
    CategoryDTO findById(UUID id);
}