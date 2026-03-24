package com.aiplatform.user.controller;

import com.aiplatform.user.dto.request.AssignUserToOrgRequest;
import com.aiplatform.user.dto.request.AssignRoleRequest;
import com.aiplatform.user.dto.request.CreateUserRequest;
import com.aiplatform.user.dto.request.UpdateUserRequest;
import com.aiplatform.user.dto.response.ApiResponse;
import com.aiplatform.user.dto.response.UserDto;
import com.aiplatform.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserDto user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(user));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserDto>>> getAllUsers(Pageable pageable) {
        Page<UserDto> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable String id) {
        UserDto user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserRequest request) {
        UserDto user = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted successfully"));
    }

    @PostMapping("/roles")
    public ResponseEntity<ApiResponse<UserDto>> assignRole(@Valid @RequestBody AssignRoleRequest request) {
        UserDto user = userService.assignRoleToUser(request.getUserId(), request.getRoleId());
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @DeleteMapping("/{id}/roles/{roleId}")
    public ResponseEntity<ApiResponse<UserDto>> removeRole(
            @PathVariable String id,
            @PathVariable String roleId) {
        UserDto user = userService.removeRoleFromUser(id, roleId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping("/organizations")
    public ResponseEntity<ApiResponse<UserDto>> assignToOrganization(@Valid @RequestBody AssignUserToOrgRequest request) {
        UserDto user = userService.assignUserToOrganization(request.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(user));
    }
}