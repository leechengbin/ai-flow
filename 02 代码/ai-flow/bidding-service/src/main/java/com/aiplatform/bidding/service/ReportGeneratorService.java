package com.aiplatform.bidding.service;

import com.aiplatform.bidding.dto.response.*;
import com.aiplatform.bidding.dto.response.BiddingCheckReportDto.*;
import com.aiplatform.bidding.dto.response.VisualizationDto.*;
import com.aiplatform.bidding.dto.response.MatchResultDto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ReportGeneratorService {

    private static final double HIGH_RISK_THRESHOLD = 60;
    private static final double MEDIUM_RISK_THRESHOLD = 80;
    private static final double COVERAGE_WEIGHT = 0.7;
    private static final double FORMAT_WEIGHT = 0.3;

    public BiddingCheckReportDto generateReport(
            List<MatchResultDto> matchResults,
            FormatCheckDto formatCheck,
            CheckOptions options) {

        log.info("Generating report for {} match results", matchResults != null ? matchResults.size() : 0);
        SummaryDto summary = calculateSummary(matchResults, formatCheck);
        CoverageDto coverage = calculateCoverage(matchResults);
        List<IssueDto> issues = extractIssues(matchResults);
        VisualizationDto visualization = generateVisualization(summary, coverage);

        return new BiddingCheckReportDto(
            "RPT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            "CHECK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            LocalDateTime.now(),
            summary,
            coverage,
            issues,
            formatCheck,
            visualization
        );
    }

    private SummaryDto calculateSummary(List<MatchResultDto> matchResults, FormatCheckDto formatCheck) {
        if (matchResults == null || matchResults.isEmpty()) {
            return new SummaryDto(0.0, 0.0,
                formatCheck != null ? formatCheck.totalScore() : 0.0,
                false, RiskLevel.LOW);
        }

        long total = matchResults.size();
        long matched = matchResults.stream()
            .filter(r -> r.matchType() == MatchType.EXACT).count();
        long partial = matchResults.stream()
            .filter(r -> r.matchType() == MatchType.PARTIAL).count();
        long missing = matchResults.stream()
            .filter(r -> r.matchType() == MatchType.MISSING).count();

        double coverageRate = ((matched + partial * 0.5) / total) * 100;
        double formatScore = formatCheck != null ? formatCheck.totalScore() : 0.0;
        double totalScore = (coverageRate * 0.7 + formatScore * 0.3);

        boolean eliminationRisk = matchResults.stream()
            .anyMatch(r -> r.tenderClause() != null
                && r.tenderClause().isStarred()
                && r.matchType() == MatchType.MISSING);

        RiskLevel riskLevel = calculateRiskLevel(totalScore, eliminationRisk);

        return new SummaryDto(totalScore, coverageRate, formatScore, eliminationRisk, riskLevel);
    }

    private RiskLevel calculateRiskLevel(double totalScore, boolean eliminationRisk) {
        if (eliminationRisk) return RiskLevel.CRITICAL;
        if (totalScore < 60) return RiskLevel.HIGH;
        if (totalScore < 80) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    private CoverageDto calculateCoverage(List<MatchResultDto> matchResults) {
        if (matchResults == null || matchResults.isEmpty()) {
            return new CoverageDto(0, 0, 0, 0, 0, 0);
        }

        int total = matchResults.size();
        int matched = (int) matchResults.stream()
            .filter(r -> r.matchType() == MatchType.EXACT).count();
        int partial = (int) matchResults.stream()
            .filter(r -> r.matchType() == MatchType.PARTIAL).count();
        int missing = (int) matchResults.stream()
            .filter(r -> r.matchType() == MatchType.MISSING).count();
        int starredMatched = (int) matchResults.stream()
            .filter(r -> r.tenderClause() != null
                && r.tenderClause().isStarred()
                && (r.matchType() == MatchType.EXACT || r.matchType() == MatchType.PARTIAL))
            .count();
        int starredMissing = (int) matchResults.stream()
            .filter(r -> r.tenderClause() != null
                && r.tenderClause().isStarred()
                && r.matchType() == MatchType.MISSING)
            .count();

        return new CoverageDto(total, matched, partial, missing, starredMatched, starredMissing);
    }

    private List<IssueDto> extractIssues(List<MatchResultDto> matchResults) {
        List<IssueDto> issues = new ArrayList<>();
        if (matchResults == null) return issues;

        for (MatchResultDto result : matchResults) {
            if (result.matchType() == MatchType.MISSING && result.tenderClause() != null) {
                issues.add(new IssueDto(
                    "ISSUE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                    "MISSING_CLAUSE",
                    "CRITICAL",
                    result.tenderClause().clauseNumber(),
                    result.tenderClause().title(),
                    "条款内容缺失",
                    result.tenderClause().isStarred(),
                    result.tenderClause().isStarred(),
                    "请补充该条款的完整内容",
                    result.tenderClause().rawText()
                ));
            } else if (result.contentIssues() != null && !result.contentIssues().isEmpty()) {
                for (var issue : result.contentIssues()) {
                    issues.add(new IssueDto(
                        issue.issueId(),
                        issue.type(),
                        issue.severity().name(),
                        result.tenderClause() != null ? result.tenderClause().clauseNumber() : null,
                        result.tenderClause() != null ? result.tenderClause().title() : null,
                        issue.description(),
                        result.tenderClause() != null && result.tenderClause().isStarred(),
                        issue.severity() == com.aiplatform.bidding.domain.enums.Severity.CRITICAL,
                        issue.type(),
                        issue.description()
                    ));
                }
            }
        }
        return issues;
    }

    private VisualizationDto generateVisualization(SummaryDto summary, CoverageDto coverage) {
        // Coverage pie chart
        PieChartData coverageChart = new PieChartData(
            List.of("完全匹配", "部分匹配", "缺失"),
            List.of(coverage.matchedClauses(), coverage.partialClauses(), coverage.missingClauses())
        );

        // Issue distribution bar chart
        BarChartData issueDistribution = new BarChartData(
            List.of("缺失", "内容不完整", "格式错误"),
            List.of(coverage.missingClauses(), coverage.partialClauses(), 0)
        );

        // Risk gauge
        GaugeData riskGauge = new GaugeData(
            summary.totalScore(),
            0.0,
            100.0,
            List.of(
                new GaugeThreshold(0.0, 60.0, "red"),
                new GaugeThreshold(60.0, 80.0, "orange"),
                new GaugeThreshold(80.0, 100.0, "green")
            )
        );

        return new VisualizationDto(coverageChart, issueDistribution, riskGauge);
    }

    public record CheckOptions(
        boolean checkCoverage,
        boolean checkFormat,
        boolean checkStarred
    ) {}
}
