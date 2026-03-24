package com.aiplatform.bidding.dto.response;

public record SignatureIssue(
    String type,
    int page,
    String status,
    String description
) {}