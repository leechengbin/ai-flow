package com.aiplatform.bidding.dto.response;

import com.aiplatform.bidding.domain.enums.Severity;

public record ContentIssueDto(
    String issueId,
    String type,
    String description,
    String expectedContent,
    String actualContent,
    Severity severity
) {}