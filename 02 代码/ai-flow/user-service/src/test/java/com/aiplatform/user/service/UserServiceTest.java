package com.aiplatform.user.service;

import com.aiplatform.user.domain.entity.User;
import com.aiplatform.user.domain.enums.UserStatus;
import com.aiplatform.user.dto.request.CreateUserRequest;
import com.aiplatform.user.dto.response.UserDto;
import com.aiplatform.user.exception.DuplicateResourceException;
import com.aiplatform.user.exception.UserNotFoundException;
import com.aiplatform.user.repository.OrganizationRepository;
import com.aiplatform.user.repository.RoleRepository;
import com.aiplatform.user.repository.UserOrganizationRepository;
import com.aiplatform.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private UserOrganizationRepository userOrganizationRepository;

    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService(userRepository, roleRepository, organizationRepository,
            userOrganizationRepository, passwordEncoder);
    }

    @Test
    void createUser_shouldCreateNewUser() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        request.setEmail("newuser@example.com");
        request.setFullName("New User");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("user-new");
            return u;
        });

        UserDto result = userService.createUser(request);

        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertEquals("newuser@example.com", result.getEmail());
    }

    @Test
    void createUser_shouldThrowExceptionForDuplicateUsername() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("existinguser");
        request.setPassword("password123");
        request.setEmail("new@example.com");
        request.setFullName("New User");

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> userService.createUser(request));
    }

    @Test
    void getUserById_shouldReturnUser() {
        User user = User.builder()
            .id("user-123")
            .username("testuser")
            .email("test@example.com")
            .status(UserStatus.ACTIVE)
            .roles(new HashSet<>())
            .build();

        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));

        UserDto result = userService.getUserById("user-123");

        assertNotNull(result);
        assertEquals("user-123", result.getId());
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void getUserById_shouldThrowExceptionForNotFound() {
        when(userRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserById("nonexistent"));
    }

    @Test
    void deleteUser_shouldSoftDeleteUser() {
        User user = User.builder()
            .id("user-123")
            .username("testuser")
            .status(UserStatus.ACTIVE)
            .roles(new HashSet<>())
            .build();

        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.deleteUser("user-123");

        assertEquals(UserStatus.DELETED, user.getStatus());
        verify(userRepository).save(user);
    }
}