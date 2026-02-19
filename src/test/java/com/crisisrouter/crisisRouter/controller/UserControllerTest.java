package com.crisisrouter.crisisRouter.controller;

import com.crisisrouter.crisisRouter.model.entity.User;
import com.crisisrouter.crisisRouter.service.UserService;
import com.crisisrouter.crisisRouter.service.dto.UserDTO;
import com.crisisrouter.crisisRouter.service.mapper.UserMapper;
import com.crisisrouter.crisisRouter.testutil.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserMapper userMapper;

    private OidcUser createOidcUser(String email) {
        OidcIdToken idToken = new OidcIdToken(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("sub", "user-123", "email", email));
        return new DefaultOidcUser(java.util.List.of(), idToken);
    }

    @Test
    void getCurrentUser_authenticated_returns200() throws Exception {
        User user = TestDataFactory.createUser();
        UserDTO dto = TestDataFactory.createUserDTO();

        when(userService.getUserByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(dto);

        mockMvc.perform(get("/api/users/me")
                        .with(oidcLogin().oidcUser(createOidcUser("test@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(dto.getEmail()));
    }

    @Test
    void getCurrentUser_userNotFound_returns404() throws Exception {
        when(userService.getUserByEmail("missing@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/me")
                        .with(oidcLogin().oidcUser(createOidcUser("missing@example.com"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateUser_authenticated_returns200() throws Exception {
        UserDTO dto = TestDataFactory.createUserDTO();
        when(userService.updateUser(eq("test@example.com"), any(UserDTO.class), any())).thenReturn(dto);

        MockMultipartFile userPart = new MockMultipartFile(
                "user", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(dto));
        MockMultipartFile imagePart = new MockMultipartFile(
                "image", "pic.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[]{1, 2});

        mockMvc.perform(multipart("/api/users/me")
                        .file(userPart)
                        .file(imagePart)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .with(csrf())
                        .with(oidcLogin().oidcUser(createOidcUser("test@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value(dto.getFirstName()));
    }

    @Test
    void updateUser_withoutImage_returns200() throws Exception {
        UserDTO dto = TestDataFactory.createUserDTO();
        when(userService.updateUser(eq("test@example.com"), any(UserDTO.class), any())).thenReturn(dto);

        MockMultipartFile userPart = new MockMultipartFile(
                "user", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(dto));

        mockMvc.perform(multipart("/api/users/me")
                        .file(userPart)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .with(csrf())
                        .with(oidcLogin().oidcUser(createOidcUser("test@example.com"))))
                .andExpect(status().isOk());
    }

    @Test
    void getCurrentUser_unauthenticated_returns401() throws Exception {
        // Without oidcLogin, Spring Security will reject the request
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }
}
