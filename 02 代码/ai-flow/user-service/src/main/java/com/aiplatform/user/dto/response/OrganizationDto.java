package com.aiplatform.user.dto.response;

import com.aiplatform.user.domain.entity.Organization;
import com.aiplatform.user.domain.enums.OrgType;
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
public class OrganizationDto {
    private String id;
    private String name;
    private OrgType orgType;
    private String code;
    private String description;
    private String parentId;
    private Integer level;
    private String managerId;
    private List<OrganizationDto> children;
    private LocalDateTime createdAt;

    public static OrganizationDto from(Organization org) {
        return OrganizationDto.builder()
            .id(org.getId())
            .name(org.getName())
            .orgType(org.getOrgType())
            .code(org.getCode())
            .description(org.getDescription())
            .parentId(org.getParent() != null ? org.getParent().getId() : null)
            .level(org.getLevel())
            .managerId(org.getManagerId())
            .createdAt(org.getCreatedAt())
            .build();
    }
}