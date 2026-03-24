package com.aiplatform.bidding.dto.response;

import java.util.List;

public record MatchResultDto(
    ClauseDto tenderClause,
    ClauseDto biddingClause,
    MatchType matchType,
    double similarityScore,
    List<ContentIssueDto> contentIssues
) {}