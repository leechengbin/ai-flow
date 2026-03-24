package com.aiplatform.bidding.controller;

import com.aiplatform.bidding.dto.request.BiddingCheckRequest;
import com.aiplatform.bidding.dto.request.CheckOptions;
import com.aiplatform.bidding.dto.response.ApiResponse;
import com.aiplatform.bidding.dto.response.BiddingCheckReportDto;
import com.aiplatform.bidding.service.BiddingCheckService;
import com.aiplatform.bidding.service.CheckRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/bidding")
@RequiredArgsConstructor
@Slf4j
public class BiddingController {
    private final BiddingCheckService biddingCheckService;
    private final CheckRecordService checkRecordService;

    @PostMapping("/check")
    public ResponseEntity<ApiResponse<BiddingCheckReportDto>> checkBidding(
            @RequestParam("tenderFile") MultipartFile tenderFile,
            @RequestParam("biddingFile") MultipartFile biddingFile,
            @RequestParam(value = "checkCoverage", required = false, defaultValue = "true") boolean checkCoverage,
            @RequestParam(value = "checkFormat", required = false, defaultValue = "true") boolean checkFormat,
            @RequestParam(value = "checkStarred", required = false, defaultValue = "true") boolean checkStarred) {

        BiddingCheckRequest request = new BiddingCheckRequest(
            tenderFile,
            biddingFile,
            new CheckOptions(checkCoverage, checkFormat, checkStarred)
        );

        BiddingCheckReportDto report = biddingCheckService.checkBidding(request);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/report/{reportId}")
    public ResponseEntity<ApiResponse<BiddingCheckReportDto>> getReport(@PathVariable String reportId) {
        Optional<BiddingCheckReportDto> report = checkRecordService.getReport(reportId);
        return report
            .map(r -> ResponseEntity.ok(ApiResponse.success(r)))
            .orElse(ResponseEntity.status(404).body(ApiResponse.error(404, "Report not found: " + reportId)));
    }
}
