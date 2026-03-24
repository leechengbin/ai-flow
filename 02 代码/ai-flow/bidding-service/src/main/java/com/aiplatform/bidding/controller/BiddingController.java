package com.aiplatform.bidding.controller;

import com.aiplatform.bidding.dto.request.BiddingCheckRequest;
import com.aiplatform.bidding.dto.response.ApiResponse;
import com.aiplatform.bidding.dto.response.BiddingCheckResponse;
import com.aiplatform.bidding.service.BiddingCheckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bidding")
@RequiredArgsConstructor
public class BiddingController {
    private final BiddingCheckService biddingCheckService;

    @PostMapping("/check")
    public ResponseEntity<ApiResponse<BiddingCheckResponse>> checkBidding(@Valid @RequestBody BiddingCheckRequest request) {
        BiddingCheckResponse response = biddingCheckService.checkBidding(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}