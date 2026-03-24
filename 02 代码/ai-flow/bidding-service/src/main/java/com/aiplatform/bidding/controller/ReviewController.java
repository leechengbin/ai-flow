package com.aiplatform.bidding.controller;

import com.aiplatform.bidding.dto.request.HumanActionRequest;
import com.aiplatform.bidding.dto.request.ReviewSubmitRequest;
import com.aiplatform.bidding.dto.response.ApiResponse;
import com.aiplatform.bidding.dto.response.ReviewStatusResponse;
import com.aiplatform.bidding.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/review")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping("/{documentId}/submit")
    public ResponseEntity<ApiResponse<ReviewStatusResponse>> submitReview(@PathVariable String documentId, @Valid @RequestBody ReviewSubmitRequest request) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.submitReview(documentId, request)));
    }

    @GetMapping("/{reviewId}/status")
    public ResponseEntity<ApiResponse<ReviewStatusResponse>> getReviewStatus(@PathVariable String reviewId) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getReviewStatus(reviewId)));
    }

    @PostMapping("/{reviewId}/human-action")
    public ResponseEntity<ApiResponse<ReviewStatusResponse>> executeHumanAction(@PathVariable String reviewId, @Valid @RequestBody HumanActionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.executeHumanAction(reviewId, request)));
    }
}