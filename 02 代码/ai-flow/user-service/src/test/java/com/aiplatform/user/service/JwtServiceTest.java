package com.aiplatform.user.service;

import com.aiplatform.user.config.JwtConfig;
import com.aiplatform.user.domain.entity.User;
import com.aiplatform.user.domain.entity.Role;
import com.aiplatform.user.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private JwtTokenProvider jwtTokenProvider;
    private User testUser;

    @BeforeEach
    void setUp() {
        JwtConfig jwtConfig = new JwtConfig();
        jwtConfig.setSecret("test-secret-key-must-be-at-least-32-characters-long-for-hs256");
        jwtConfig.setAccessTokenExpirationMs(3600000);
        jwtConfig.setRefreshTokenExpirationMs(604800000);

        jwtTokenProvider = new JwtTokenProvider(jwtConfig);
        jwtTokenProvider.init();

        Role testRole = Role.builder()
            .id("role-1")
            .code("USER")
            .name("User")
            .build();

        testUser = User.builder()
            .id("user-123")
            .username("testuser")
            .roles(new HashSet<>())
            .build();
        testUser.getRoles().add(testRole);
    }

    @Test
    void generateAccessToken_shouldCreateValidToken() {
        String token = jwtTokenProvider.generateAccessToken(testUser);
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void validateToken_shouldReturnTrueForValidToken() {
        String token = jwtTokenProvider.generateAccessToken(testUser);
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void validateToken_shouldReturnFalseForInvalidToken() {
        assertFalse(jwtTokenProvider.validateToken("invalid.token.here"));
    }

    @Test
    void getUserIdFromToken_shouldReturnCorrectUserId() {
        String token = jwtTokenProvider.generateAccessToken(testUser);
        String userId = jwtTokenProvider.getUserIdFromToken(token);
        assertEquals("user-123", userId);
    }

    @Test
    void generateRefreshToken_shouldCreateRefreshToken() {
        String refreshToken = jwtTokenProvider.generateRefreshToken(testUser);
        assertNotNull(refreshToken);
        assertTrue(jwtTokenProvider.validateRefreshToken(refreshToken));
    }
}