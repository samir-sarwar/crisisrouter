package com.crisisrouter.crisisRouter.service.impl;

import com.crisisrouter.crisisRouter.model.entity.User;
import com.crisisrouter.crisisRouter.repository.UserRepository;
import com.crisisrouter.crisisRouter.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
}