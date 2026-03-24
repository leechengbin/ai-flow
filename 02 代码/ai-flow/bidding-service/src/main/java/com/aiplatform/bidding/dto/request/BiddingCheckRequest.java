package com.aiplatform.bidding.dto.request;

import org.springframework.web.multipart.MultipartFile;

public record BiddingCheckRequest(
    MultipartFile tenderFile,
    MultipartFile biddingFile,
    CheckOptions checkOptions
) {}
