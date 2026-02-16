package com.crisisrouter.crisisRouter.service;

import com.crisisrouter.crisisRouter.model.entity.User;
import com.crisisrouter.crisisRouter.service.dto.UserDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
public interface UserService {
    Optional<User> getUserByEmail(String email);

    // Updates user profile
    UserDTO updateUser(String email, UserDTO userDTO, MultipartFile image);

    // Future expansion:
    // User updateUser(UserDTO userDTO);
    // void deleteUser(UUID id);
}