package com.aiplatform.user.service;

import com.aiplatform.user.domain.entity.Organization;
import com.aiplatform.user.domain.entity.UserOrganization;
import com.aiplatform.user.dto.request.CreateOrganizationRequest;
import com.aiplatform.user.dto.response.OrganizationDto;
import com.aiplatform.user.dto.response.UserDto;
import com.aiplatform.user.exception.OrganizationNotFoundException;
import com.aiplatform.user.repository.OrganizationRepository;
import com.aiplatform.user.repository.UserOrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationService {
    private final OrganizationRepository organizationRepository;
    private final UserOrganizationRepository userOrganizationRepository;

    @Transactional
    public OrganizationDto createOrganization(CreateOrganizationRequest request) {
        Organization org = Organization.builder()
            .name(request.getName())
            .orgType(request.getOrgType())
            .code(request.getCode())
            .description(request.getDescription())
            .managerId(request.getManagerId())
            .build();

        if (request.getParentId() != null) {
            Organization parent = organizationRepository.findById(request.getParentId())
                .orElseThrow(() -> new OrganizationNotFoundException(request.getParentId()));
            org.setParent(parent);
            org.setLevel(parent.getLevel() + 1);
        } else {
            org.setLevel(1);
        }

        Organization saved = organizationRepository.save(org);
        log.info("Created new organization: {}", saved.getId());
        return OrganizationDto.from(saved);
    }

    @Transactional(readOnly = true)
    public OrganizationDto getOrganizationById(String id) {
        Organization org = organizationRepository.findById(id)
            .orElseThrow(() -> new OrganizationNotFoundException(id));
        return OrganizationDto.from(org);
    }

    @Transactional(readOnly = true)
    public List<OrganizationDto> getAllOrganizations() {
        return organizationRepository.findAll().stream().map(OrganizationDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<OrganizationDto> getChildOrganizations(String parentId) {
        return organizationRepository.findByParentId(parentId).stream().map(OrganizationDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<UserDto> getUsersInOrganization(String organizationId) {
        List<UserOrganization> userOrgs = userOrganizationRepository.findByOrganizationId(organizationId);
        return userOrgs.stream()
            .map(uo -> UserDto.from(uo.getUser()))
            .toList();
    }
}