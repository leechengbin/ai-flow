package com.aiplatform.bidding.dto.response;

import java.util.List;

public record FormatCheckDto(
    CompletenessCheck completeness,
    SignatureCheck signatures,
    DateCheck dates,
    int totalScore
) {}