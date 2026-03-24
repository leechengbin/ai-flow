package com.aiplatform.bidding.dto.response;

import com.aiplatform.bidding.domain.enums.Severity;

public record DateIssue(
    String type,
    String rawText,
    String description,
    Severity severity
) {}