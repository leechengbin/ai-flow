package com.aiplatform.bidding.controller;

import com.aiplatform.bidding.dto.request.DiffCompareRequest;
import com.aiplatform.bidding.dto.response.ApiResponse;
import com.aiplatform.bidding.dto.response.DiffCompareResponse;
import com.aiplatform.bidding.service.VersionDiffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/diff")
@RequiredArgsConstructor
public class DiffController {
    private final VersionDiffService versionDiffService;

    @PostMapping("/compare")
    public ResponseEntity<ApiResponse<DiffCompareResponse>> compareVersions(@Valid @RequestBody DiffCompareRequest request) {
        return ResponseEntity.ok(ApiResponse.success(versionDiffService.compareVersions(request)));
    }
}