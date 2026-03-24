package com.aiplatform.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssignUserToOrgRequest {
    @NotBlank(message = "Organization ID is required")
    private String organizationId;

    @NotBlank(message = "Role in organization is required")
    private String roleInOrg;

    private String userTitle;
}