package com.aiplatform.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssignRoleRequest {
    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Role ID is required")
    private String roleId;
}