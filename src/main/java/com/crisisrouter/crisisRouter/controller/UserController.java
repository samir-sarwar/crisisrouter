package com.crisisrouter.crisisRouter.controller;

import com.crisisrouter.crisisRouter.service.UserService;
import com.crisisrouter.crisisRouter.service.dto.UserDTO;
import com.crisisrouter.crisisRouter.service.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(@AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        String email = principal.getEmail();

        // 1. Call Service -> 2. Map Entity to DTO -> 3. Return JSON
        return userService.getUserByEmail(email)
                .map(userMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserDTO> updateUser(
            @AuthenticationPrincipal OidcUser principal,
            @RequestPart("user") UserDTO userDTO,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        String email = principal.getEmail();
        UserDTO updatedUser = userService.updateUser(email, userDTO, image);
        return ResponseEntity.ok(updatedUser);
    }
}