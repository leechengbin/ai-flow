package com.aiplatform.user.service;

import com.aiplatform.user.domain.entity.User;
import com.aiplatform.user.domain.enums.UserStatus;
import com.aiplatform.user.dto.request.LoginRequest;
import com.aiplatform.user.dto.response.LoginResponse;
import com.aiplatform.user.exception.AuthenticationFailedException;
import com.aiplatform.user.repository.UserRepository;
import com.aiplatform.user.security.JwtTokenProvider;
import com.aiplatform.user.config.JwtConfig;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        JwtConfig jwtConfig = new JwtConfig();
        jwtConfig.setSecret("test-secret-key-must-be-at-least-32-characters-long-for-hs256");
        jwtConfig.setAccessTokenExpirationMs(3600000);
        jwtConfig.setRefreshTokenExpirationMs(604800000);
        jwtTokenProvider = new JwtTokenProvider(jwtConfig);
        jwtTokenProvider.init();
        authService = new AuthService(userRepository, passwordEncoder, jwtTokenProvider);
    }

    @Test
    void login_shouldReturnTokensForValidCredentials() {
        User user = User.builder()
            .id("user-123")
            .username("testuser")
            .password(passwordEncoder.encode("password123"))
            .status(UserStatus.ACTIVE)
            .roles(new HashSet<>())
            .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        LoginResponse response = authService.login(request);

        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
    }

    @Test
    void login_shouldThrowExceptionForInvalidUsername() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setUsername("nonexistent");
        request.setPassword("password123");

        assertThrows(AuthenticationFailedException.class, () -> authService.login(request));
    }

    @Test
    void login_shouldThrowExceptionForInvalidPassword() {
        User user = User.builder()
            .id("user-123")
            .username("testuser")
            .password(passwordEncoder.encode("correctpassword"))
            .status(UserStatus.ACTIVE)
            .roles(new HashSet<>())
            .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrongpassword");

        assertThrows(AuthenticationFailedException.class, () -> authService.login(request));
    }

    @Test
    void login_shouldThrowExceptionForInactiveUser() {
        User user = User.builder()
            .id("user-123")
            .username("testuser")
            .password(passwordEncoder.encode("password123"))
            .status(UserStatus.SUSPENDED)
            .roles(new HashSet<>())
            .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        assertThrows(AuthenticationFailedException.class, () -> authService.login(request));
    }
}