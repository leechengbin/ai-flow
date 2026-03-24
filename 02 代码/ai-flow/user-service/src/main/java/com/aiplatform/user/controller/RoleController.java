package com.aiplatform.user.controller;

import com.aiplatform.user.dto.request.CreateRoleRequest;
import com.aiplatform.user.dto.response.ApiResponse;
import com.aiplatform.user.dto.response.RoleDto;
import com.aiplatform.user.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {
    private final RoleService roleService;

    @PostMapping
    public ResponseEntity<ApiResponse<RoleDto>> createRole(@Valid @RequestBody CreateRoleRequest request) {
        RoleDto role = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(role));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleDto>>> getAllRoles() {
        List<RoleDto> roles = roleService.getAllRoles();
        return ResponseEntity.ok(ApiResponse.success(roles));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleDto>> getRoleById(@PathVariable String id) {
        RoleDto role = roleService.getRoleById(id);
        return ResponseEntity.ok(ApiResponse.success(role));
    }

    @PostMapping("/{id}/permissions/{permId}")
    public ResponseEntity<ApiResponse<RoleDto>> addPermission(
            @PathVariable String id,
            @PathVariable String permId) {
        RoleDto role = roleService.addPermissionToRole(id, permId);
        return ResponseEntity.ok(ApiResponse.success(role));
    }

    @DeleteMapping("/{id}/permissions/{permId}")
    public ResponseEntity<ApiResponse<RoleDto>> removePermission(
            @PathVariable String id,
            @PathVariable String permId) {
        RoleDto role = roleService.removePermissionFromRole(id, permId);
        return ResponseEntity.ok(ApiResponse.success(role));
    }
}