package com.crisisrouter.crisisRouter.service.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class UserDTO {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
}