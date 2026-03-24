package com.aiplatform.bidding.dto.response;

public record FormatIssue(
    String type,
    String description,
    String severity,
    String context
) {}