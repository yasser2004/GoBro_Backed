package com.gobro_backend.user.service;

import com.gobro_backend.user.dto.UpdateUserRequest;
import com.gobro_backend.user.dto.UserResponse;
import com.gobro_backend.user.entity.Role;
import com.gobro_backend.user.entity.User;
import com.gobro_backend.user.repository.RoleRepository;
import com.gobro_backend.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Profile management for the authenticated user, plus administration
 * operations (listing, enabling/disabling accounts, role assignment)
 * reserved to ADMIN via @PreAuthorize in {@code UserController}.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse getByEmail(String email) {
        return toResponse(findUserByEmail(email));
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return toResponse(findUserById(id));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(String keyword, Pageable pageable) {
        Page<User> page = StringUtils.hasText(keyword)
                ? userRepository.search(keyword, pageable)
                : userRepository.findAll(pageable);
        return page.map(this::toResponse);
    }

    @Transactional
    public UserResponse updateProfile(String email, @Valid UpdateUserRequest request) {
        User user = findUserByEmail(email);

        if (StringUtils.hasText(request.fullName())) {
            user.setFullName(request.fullName());
        }
        if (request.bio() != null) {
            user.setBio(request.bio());
        }
        if (request.phoneNumber() != null) {
            user.setPhoneNumber(request.phoneNumber());
        }
        if (StringUtils.hasText(request.avatarUrl())) {
            user.setAvatarUrl(request.avatarUrl());
        }

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = findUserByEmail(email);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mot de passe actuel incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public UserResponse setEnabled(Long userId, boolean enabled) {
        User user = findUserById(userId);
        user.setEnabled(enabled);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse assignRole(Long userId, String roleName) {
        User user = findUserById(userId);
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rôle invalide : " + roleName));
        user.setRole(role);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable");
        }
        userRepository.deleteById(userId);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getPhoneNumber(),
                user.getRole().getName(),
                user.isEnabled(),
                user.isTwoFactorEnabled(),
                user.getXp(),
                user.getLevel(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }
}