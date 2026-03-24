package com.aiplatform.bidding.controller;

import com.aiplatform.bidding.dto.request.CaseRetrieveRequest;
import com.aiplatform.bidding.dto.response.ApiResponse;
import com.aiplatform.bidding.dto.response.CaseRetrieveResponse;
import com.aiplatform.bidding.service.CaseRetrievalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cases")
@RequiredArgsConstructor
public class CaseController {
    private final CaseRetrievalService caseRetrievalService;

    @PostMapping("/retrieve")
    public ResponseEntity<ApiResponse<CaseRetrieveResponse>> retrieveCases(@Valid @RequestBody CaseRetrieveRequest request) {
        return ResponseEntity.ok(ApiResponse.success(caseRetrievalService.retrieveSimilarCases(request)));
    }
}