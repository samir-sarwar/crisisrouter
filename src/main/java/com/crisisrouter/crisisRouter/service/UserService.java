package com.crisisrouter.crisisRouter.service;

import com.crisisrouter.crisisRouter.model.entity.User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public interface UserService {
    Optional<User> getUserByEmail(String email);

    // Future expansion:
    // User updateUser(UserDTO userDTO);
    // void deleteUser(UUID id);
}