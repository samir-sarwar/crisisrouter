package com.crisisrouter.crisisRouter.service.impl;

import com.crisisrouter.crisisRouter.model.entity.User;
import com.crisisrouter.crisisRouter.repository.UserRepository;
import com.crisisrouter.crisisRouter.service.FileStorageService;
import com.crisisrouter.crisisRouter.service.UserService;
import com.crisisrouter.crisisRouter.service.dto.UserDTO;
import com.crisisrouter.crisisRouter.service.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    private final FileStorageService fileStorageService;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserDTO updateUser(String email, UserDTO userDTO, MultipartFile image) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (userDTO.getFirstName() != null)
            user.setFirstName(userDTO.getFirstName());
        if (userDTO.getLastName() != null)
            user.setLastName(userDTO.getLastName());
        if (userDTO.getPhoneNumber() != null)
            user.setPhoneNumber(userDTO.getPhoneNumber());
        if (userDTO.getDescription() != null)
            user.setDescription(userDTO.getDescription());
        if (userDTO.getAddress() != null)
            user.setAddress(userDTO.getAddress());
        if (userDTO.getLatitude() != null)
            user.setLatitude(userDTO.getLatitude());
        if (userDTO.getLongitude() != null)
            user.setLongitude(userDTO.getLongitude());

        if (image != null && !image.isEmpty()) {
            String imageUrl = fileStorageService.storeFile(image);
            user.setProfilePictureUrl(imageUrl);
        }

        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }
}