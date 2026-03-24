package com.aiplatform.user.controller;

import com.aiplatform.user.dto.request.LoginRequest;
import com.aiplatform.user.dto.request.RefreshTokenRequest;
import com.aiplatform.user.dto.response.ApiResponse;
import com.aiplatform.user.dto.response.LoginResponse;
import com.aiplatform.user.dto.response.UserDto;
import com.aiplatform.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.aiplatform.user.domain.entity.User;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(@AuthenticationPrincipal User user) {
        UserDto userDto = authService.getCurrentUser(user.getId());
        return ResponseEntity.ok(ApiResponse.success(userDto));
    }
}