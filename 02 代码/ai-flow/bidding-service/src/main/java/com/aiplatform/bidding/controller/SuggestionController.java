package com.aiplatform.bidding.controller;

import com.aiplatform.bidding.dto.request.SuggestionGenerateRequest;
import com.aiplatform.bidding.dto.response.ApiResponse;
import com.aiplatform.bidding.dto.response.SuggestionGenerateResponse;
import com.aiplatform.bidding.service.SuggestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/suggestions")
@RequiredArgsConstructor
public class SuggestionController {
    private final SuggestionService suggestionService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<SuggestionGenerateResponse>> generateSuggestions(@Valid @RequestBody SuggestionGenerateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(suggestionService.generateSuggestions(request)));
    }
}