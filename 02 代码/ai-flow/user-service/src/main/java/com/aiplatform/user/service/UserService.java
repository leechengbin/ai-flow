package com.aiplatform.user.service;

import com.aiplatform.user.domain.entity.Organization;
import com.aiplatform.user.domain.entity.Role;
import com.aiplatform.user.domain.entity.User;
import com.aiplatform.user.domain.entity.UserOrganization;
import com.aiplatform.user.domain.enums.UserStatus;
import com.aiplatform.user.dto.request.AssignUserToOrgRequest;
import com.aiplatform.user.dto.request.CreateUserRequest;
import com.aiplatform.user.dto.request.UpdateUserRequest;
import com.aiplatform.user.dto.response.UserDto;
import com.aiplatform.user.exception.DuplicateResourceException;
import com.aiplatform.user.exception.UserNotFoundException;
import com.aiplatform.user.repository.OrganizationRepository;
import com.aiplatform.user.repository.RoleRepository;
import com.aiplatform.user.repository.UserOrganizationRepository;
import com.aiplatform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizationRepository organizationRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + request.getEmail());
        }

        User user = User.builder()
            .username(request.getUsername())
            .password(passwordEncoder.encode(request.getPassword()))
            .email(request.getEmail())
            .phone(request.getPhone())
            .fullName(request.getFullName())
            .status(UserStatus.ACTIVE)
            .build();

        User saved = userRepository.save(user);
        log.info("Created new user: {}", saved.getId());
        return UserDto.from(saved);
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(String id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
        return UserDto.from(user);
    }

    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserDto::from);
    }

    @Transactional
    public UserDto updateUser(String id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("Email already exists: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getStatus() != null) user.setStatus(request.getStatus());

        User updated = userRepository.save(user);
        log.info("Updated user: {}", updated.getId());
        return UserDto.from(updated);
    }

    @Transactional
    public void deleteUser(String id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
        user.setStatus(UserStatus.DELETED);
        userRepository.save(user);
        log.info("Soft deleted user: {}", id);
    }

    @Transactional
    public UserDto assignRoleToUser(String userId, String roleId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));
        user.getRoles().add(role);
        User updated = userRepository.save(user);
        log.info("Assigned role {} to user {}", roleId, userId);
        return UserDto.from(updated);
    }

    @Transactional
    public UserDto removeRoleFromUser(String userId, String roleId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        user.getRoles().removeIf(r -> r.getId().equals(roleId));
        User updated = userRepository.save(user);
        log.info("Removed role {} from user {}", roleId, userId);
        return UserDto.from(updated);
    }

    @Transactional
    public UserDto assignUserToOrganization(String userId, AssignUserToOrgRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        Organization org = organizationRepository.findById(request.getOrganizationId())
            .orElseThrow(() -> new RuntimeException("Organization not found: " + request.getOrganizationId()));

        if (userOrganizationRepository.existsByUserIdAndOrganizationId(userId, request.getOrganizationId())) {
            throw new DuplicateResourceException("User already assigned to this organization");
        }

        UserOrganization userOrg = UserOrganization.builder()
            .user(user)
            .organization(org)
            .roleInOrg(request.getRoleInOrg())
            .userTitle(request.getUserTitle())
            .build();

        userOrganizationRepository.save(userOrg);
        log.info("Assigned user {} to organization {} with role {}", userId, request.getOrganizationId(), request.getRoleInOrg());

        return UserDto.from(userRepository.findById(userId).orElseThrow());
    }
}