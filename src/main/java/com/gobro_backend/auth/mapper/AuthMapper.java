package com.gobro_backend.auth.mapper;


import com.gobro_backend.auth.dto.AuthResponse;
import com.gobro_backend.auth.dto.RegisterRequest;
import com.gobro_backend.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true) // hashed explicitly in AuthService, never mapped as plain text
    @Mapping(target = "role", ignore = true) // resolved from requestedRole in AuthService
    @Mapping(target = "enabled", constant = "true")
    @Mapping(target = "xp", constant = "0")
    @Mapping(target = "level", constant = "1")
    @Mapping(target = "provider", constant = "LOCAL")
    @Mapping(target = "twoFactorEnabled", constant = "false")
    User toEntity(RegisterRequest request);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "fullName", source = "fullName")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "role", source = "role.name")
    @Mapping(target = "xp", source = "xp")
    @Mapping(target = "level", source = "level")
    AuthResponse.UserSummary toUserSummary(User user);

    @Named("updateFullName")
    default void updateFullName(@MappingTarget User user, String fullName) {
        user.setFullName(fullName);
    }
}