package com.aiplatform.user.service;

import com.aiplatform.user.domain.entity.Permission;
import com.aiplatform.user.dto.response.PermissionDto;
import com.aiplatform.user.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionService {
    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public List<PermissionDto> getAllPermissions() {
        return permissionRepository.findAll().stream().map(PermissionDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PermissionDto> getPermissionsByResource(String resource) {
        return permissionRepository.findByResource(resource).stream().map(PermissionDto::from).toList();
    }
}