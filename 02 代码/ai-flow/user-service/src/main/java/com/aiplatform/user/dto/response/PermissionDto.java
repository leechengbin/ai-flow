package com.aiplatform.user.dto.response;

import com.aiplatform.user.domain.entity.Permission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionDto {
    private String id;
    private String code;
    private String name;
    private String description;
    private String resource;
    private String action;

    public static PermissionDto from(Permission permission) {
        return PermissionDto.builder()
            .id(permission.getId())
            .code(permission.getCode())
            .name(permission.getName())
            .description(permission.getDescription())
            .resource(permission.getResource())
            .action(permission.getAction())
            .build();
    }
}