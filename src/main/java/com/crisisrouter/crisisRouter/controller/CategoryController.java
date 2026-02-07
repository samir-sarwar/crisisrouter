package com.crisisrouter.crisisRouter.controller;

import com.crisisrouter.crisisRouter.service.CategoryService;
import com.crisisrouter.crisisRouter.service.dto.CategoryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
        List<CategoryDTO> categories = categoryService.findAll();
        return ResponseEntity.ok(categories); // sends 200 status code

    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable UUID id) {
        CategoryDTO category = categoryService.findById(id);
        return ResponseEntity.ok(category);
    }
}

// PathVariable maps the id inside URL directly it id param in method