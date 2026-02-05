package com.crisisrouter.crisisRouter.service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {
    private UUID id;

    @NotBlank(message = "Category name is required")
    private String name;

    private String description;

}
