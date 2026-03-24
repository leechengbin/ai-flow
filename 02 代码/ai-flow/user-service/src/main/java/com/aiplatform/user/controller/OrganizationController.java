package com.aiplatform.user.controller;

import com.aiplatform.user.dto.request.CreateOrganizationRequest;
import com.aiplatform.user.dto.response.ApiResponse;
import com.aiplatform.user.dto.response.OrganizationDto;
import com.aiplatform.user.dto.response.UserDto;
import com.aiplatform.user.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {
    private final OrganizationService organizationService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrganizationDto>> createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request) {
        OrganizationDto org = organizationService.createOrganization(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(org));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrganizationDto>>> getAllOrganizations() {
        List<OrganizationDto> orgs = organizationService.getAllOrganizations();
        return ResponseEntity.ok(ApiResponse.success(orgs));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationDto>> getOrganizationById(@PathVariable String id) {
        OrganizationDto org = organizationService.getOrganizationById(id);
        return ResponseEntity.ok(ApiResponse.success(org));
    }

    @GetMapping("/{id}/children")
    public ResponseEntity<ApiResponse<List<OrganizationDto>>> getChildOrganizations(@PathVariable String id) {
        List<OrganizationDto> children = organizationService.getChildOrganizations(id);
        return ResponseEntity.ok(ApiResponse.success(children));
    }

    @GetMapping("/{id}/users")
    public ResponseEntity<ApiResponse<List<UserDto>>> getUsersInOrganization(@PathVariable String id) {
        List<UserDto> users = organizationService.getUsersInOrganization(id);
        return ResponseEntity.ok(ApiResponse.success(users));
    }
}