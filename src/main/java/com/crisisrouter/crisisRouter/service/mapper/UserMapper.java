package com.crisisrouter.crisisRouter.service.mapper;

import com.crisisrouter.crisisRouter.model.entity.User;
import com.crisisrouter.crisisRouter.service.dto.UserDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDTO toDto(User user);

    User toEntity(UserDTO userDTO);
}