package com.aiplatform.bidding.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record BiddingCheckReportDto(
    String reportId,
    String checkId,
    LocalDateTime generatedAt,
    SummaryDto summary,
    CoverageDto coverage,
    List<IssueDto> issues,
    FormatCheckDto format,
    VisualizationDto visualization
) {}

record SummaryDto(
    double totalScore,
    double coverageRate,
    double formatScore,
    boolean eliminationRisk,
    RiskLevel riskLevel
) {}

record CoverageDto(
    int totalClauses,
    int matchedClauses,
    int partialClauses,
    int missingClauses,
    int starredMatched,
    int starredMissing
) {}

record IssueDto(
    String issueId,
    String type,
    String severity,
    String clauseNumber,
    String title,
    String description,
    boolean isStarred,
    boolean eliminationRisk,
    String suggestion,
    String contextText
) {}

enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
