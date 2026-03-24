package com.aiplatform.bidding.dto.response;

import java.util.List;

public record CompletenessCheck(
    int score,
    List<FormatIssue> issues
) {}