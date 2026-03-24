package com.aiplatform.bidding.dto.response;

import com.aiplatform.bidding.domain.enums.ClauseType;

public record ClauseDto(
    String clauseNumber,
    String title,
    String content,
    boolean isStarred,
    ClauseType type,
    int startPage,
    int endPage,
    String rawText
) {}