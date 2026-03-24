package com.aiplatform.user.service;

import com.aiplatform.user.domain.entity.Permission;
import com.aiplatform.user.domain.entity.Role;
import com.aiplatform.user.dto.request.CreateRoleRequest;
import com.aiplatform.user.dto.response.PermissionDto;
import com.aiplatform.user.dto.response.RoleDto;
import com.aiplatform.user.exception.DuplicateResourceException;
import com.aiplatform.user.exception.RoleNotFoundException;
import com.aiplatform.user.repository.PermissionRepository;
import com.aiplatform.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Transactional
    public RoleDto createRole(CreateRoleRequest request) {
        if (roleRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Role code already exists: " + request.getCode());
        }

        Role role = Role.builder()
            .code(request.getCode())
            .name(request.getName())
            .description(request.getDescription())
            .build();

        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            List<Permission> permissions = permissionRepository.findAllById(request.getPermissionIds());
            role.setPermissions(java.util.Set.copyOf(permissions));
        }

        Role saved = roleRepository.save(role);
        log.info("Created new role: {}", saved.getId());
        return RoleDto.from(saved);
    }

    @Transactional(readOnly = true)
    public RoleDto getRoleById(String id) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new RoleNotFoundException(id));
        return RoleDto.from(role);
    }

    @Transactional(readOnly = true)
    public List<RoleDto> getAllRoles() {
        return roleRepository.findAll().stream().map(RoleDto::from).toList();
    }

    @Transactional
    public RoleDto addPermissionToRole(String roleId, String permissionId) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new RoleNotFoundException(roleId));
        Permission permission = permissionRepository.findById(permissionId)
            .orElseThrow(() -> new RuntimeException("Permission not found: " + permissionId));
        role.getPermissions().add(permission);
        Role updated = roleRepository.save(role);
        log.info("Added permission {} to role {}", permissionId, roleId);
        return RoleDto.from(updated);
    }

    @Transactional
    public RoleDto removePermissionFromRole(String roleId, String permissionId) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new RoleNotFoundException(roleId));
        role.getPermissions().removeIf(p -> p.getId().equals(permissionId));
        Role updated = roleRepository.save(role);
        log.info("Removed permission {} from role {}", permissionId, roleId);
        return RoleDto.from(updated);
    }
}