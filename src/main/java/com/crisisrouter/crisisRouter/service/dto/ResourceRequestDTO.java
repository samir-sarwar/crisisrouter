package com.crisisrouter.crisisRouter.service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class ResourceRequestDTO {

    private UUID id;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Address is required")
    private String address;

    @Min(1) @Max(5)
    private int severityLevel;

    @NotNull(message = "Category ID is required")
    private UUID categoryId;

    // This stays optional here, but our Service Layer will enforce it
    // if the category is "Other"
    private String customCategory;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    private String status;
}
