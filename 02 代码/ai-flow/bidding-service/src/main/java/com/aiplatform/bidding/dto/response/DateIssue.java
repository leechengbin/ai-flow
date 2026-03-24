package com.aiplatform.bidding.dto.response;

public record DateIssue(
    String type,
    String rawText,
    String description,
    String severity
) {}