package com.aiplatform.bidding.dto.response;

import java.util.List;

public record SignatureCheck(
    int score,
    List<SignatureIssue> issues
) {}