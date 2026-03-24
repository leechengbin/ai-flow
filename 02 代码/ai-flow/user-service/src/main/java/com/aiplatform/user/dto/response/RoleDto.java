package com.aiplatform.user.dto.response;

import com.aiplatform.user.domain.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleDto {
    private String id;
    private String code;
    private String name;
    private String description;
    private List<PermissionDto> permissions;
    private LocalDateTime createdAt;

    public static RoleDto from(Role role) {
        return RoleDto.builder()
            .id(role.getId())
            .code(role.getCode())
            .name(role.getName())
            .description(role.getDescription())
            .permissions(role.getPermissions().stream().map(PermissionDto::from).toList())
            .createdAt(role.getCreatedAt())
            .build();
    }
}