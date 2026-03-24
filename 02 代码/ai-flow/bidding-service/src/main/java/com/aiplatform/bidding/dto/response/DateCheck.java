package com.aiplatform.bidding.dto.response;

import java.util.List;

public record DateCheck(
    int score,
    List<DateIssue> issues
) {}