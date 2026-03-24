package com.aiplatform.bidding.dto.response;

import com.aiplatform.bidding.domain.enums.Severity;

public record FormatIssue(
    String type,
    String description,
    Severity severity,
    String context
) {}