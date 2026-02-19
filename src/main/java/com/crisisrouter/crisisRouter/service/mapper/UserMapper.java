package com.crisisrouter.crisisRouter.service.mapper;

import com.crisisrouter.crisisRouter.model.entity.User;
import com.crisisrouter.crisisRouter.model.entity.UserRole;
import com.crisisrouter.crisisRouter.service.dto.UserDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = {UserRole.class})
public interface UserMapper {

    @Mapping(target = "role", expression = "java(user.getRole() != null ? user.getRole().name() : null)")
    UserDTO toDto(User user);

    @Mapping(target = "role", expression = "java(dto.getRole() != null ? UserRole.valueOf(dto.getRole()) : null)")
    User toEntity(UserDTO dto);
}