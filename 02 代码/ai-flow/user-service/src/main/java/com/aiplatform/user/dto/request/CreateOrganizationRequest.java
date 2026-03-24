package com.aiplatform.user.dto.request;

import com.aiplatform.user.domain.enums.OrgType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateOrganizationRequest {
    @NotBlank(message = "Organization name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @NotNull(message = "Organization type is required")
    private OrgType orgType;

    @Size(max = 50, message = "Code must not exceed 50 characters")
    private String code;

    private String description;

    private String parentId;

    private String managerId;
}