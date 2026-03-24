package com.aiplatform.bidding.service;

import com.aiplatform.bidding.dto.request.CheckOptions;
import com.aiplatform.bidding.dto.response.*;
import com.aiplatform.bidding.domain.enums.ClauseType;
import com.aiplatform.bidding.dto.response.MatchResultDto.MatchType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReportGeneratorServiceTest {

    private ReportGeneratorService service;

    @BeforeEach
    void setUp() {
        service = new ReportGeneratorService();
    }

    @Test
    @DisplayName("Should calculate correct summary")
    void generateReport_shouldCalculateCorrectSummary() {
        ClauseDto tenderClause = new ClauseDto("1", "要求", "内容", false,
            ClauseType.OTHER, 1, 1, "第1条 要求");

        List<MatchResultDto> matches = List.of(
            new MatchResultDto(tenderClause, tenderClause, MatchType.EXACT, 1.0, List.of()),
            new MatchResultDto(tenderClause, tenderClause, MatchType.EXACT, 1.0, List.of()),
            new MatchResultDto(tenderClause, null, MatchType.MISSING, 0.0, List.of())
        );

        FormatCheckDto format = new FormatCheckDto(
            new CompletenessCheck(100, List.of()),
            new SignatureCheck(100, List.of()),
            new DateCheck(100, List.of()),
            100
        );

        BiddingCheckReportDto report = service.generateReport(matches, format, new CheckOptions(true, true, true));

        assertNotNull(report.reportId());
        assertNotNull(report.checkId());
        assertNotNull(report.summary());
        assertTrue(report.summary().coverageRate() > 0);
    }

    @Test
    @DisplayName("Should handle empty match results")
    void generateReport_shouldHandleEmptyMatchResults() {
        FormatCheckDto format = new FormatCheckDto(
            new CompletenessCheck(0, List.of()),
            new SignatureCheck(0, List.of()),
            new DateCheck(0, List.of()),
            0
        );

        BiddingCheckReportDto report = service.generateReport(List.of(), format, new CheckOptions(true, true, true));

        assertNotNull(report);
        assertEquals(0.0, report.summary().totalScore());
        assertEquals(0.0, report.summary().coverageRate());
    }

    @Test
    @DisplayName("Should handle null match results")
    void generateReport_shouldHandleNullMatchResults() {
        FormatCheckDto format = new FormatCheckDto(
            new CompletenessCheck(100, List.of()),
            new SignatureCheck(100, List.of()),
            new DateCheck(100, List.of()),
            100
        );

        BiddingCheckReportDto report = service.generateReport(null, format, new CheckOptions(true, true, true));

        assertNotNull(report);
        assertEquals(0.0, report.summary().totalScore());
    }

    @Test
    @DisplayName("Should identify elimination risk for starred missing clauses")
    void generateReport_shouldIdentifyEliminationRisk() {
        ClauseDto starredClause = new ClauseDto("1", "关键条款", "内容", true,
            ClauseType.OTHER, 1, 1, "第1条 ★关键条款");

        List<MatchResultDto> matches = List.of(
            new MatchResultDto(starredClause, null, MatchType.MISSING, 0.0, List.of())
        );

        FormatCheckDto format = new FormatCheckDto(
            new CompletenessCheck(100, List.of()),
            new SignatureCheck(100, List.of()),
            new DateCheck(100, List.of()),
            100
        );

        BiddingCheckReportDto report = service.generateReport(matches, format, new CheckOptions(true, true, true));

        assertTrue(report.summary().eliminationRisk());
        // RiskLevel is package-private, so we check via name comparison
        assertEquals("CRITICAL", report.summary().riskLevel().name());
    }

    @Test
    @DisplayName("Should calculate coverage correctly")
    void generateReport_shouldCalculateCoverageCorrectly() {
        ClauseDto clause = new ClauseDto("1", "条款", "内容", false,
            ClauseType.OTHER, 1, 1, "第1条 条款");

        List<MatchResultDto> matches = List.of(
            new MatchResultDto(clause, clause, MatchType.EXACT, 1.0, List.of()),
            new MatchResultDto(clause, clause, MatchType.PARTIAL, 0.8, List.of()),
            new MatchResultDto(clause, null, MatchType.MISSING, 0.0, List.of())
        );

        FormatCheckDto format = new FormatCheckDto(
            new CompletenessCheck(100, List.of()),
            new SignatureCheck(100, List.of()),
            new DateCheck(100, List.of()),
            100
        );

        BiddingCheckReportDto report = service.generateReport(matches, format, new CheckOptions(true, true, true));

        assertEquals(3, report.coverage().totalClauses());
        assertEquals(1, report.coverage().matchedClauses());
        assertEquals(1, report.coverage().partialClauses());
        assertEquals(1, report.coverage().missingClauses());
    }

    @Test
    @DisplayName("Should extract issues from match results")
    void generateReport_shouldExtractIssues() {
        ClauseDto clause = new ClauseDto("1", "条款", "内容", false,
            ClauseType.OTHER, 1, 1, "第1条 条款");

        List<MatchResultDto> matches = List.of(
            new MatchResultDto(clause, null, MatchType.MISSING, 0.0, List.of())
        );

        FormatCheckDto format = new FormatCheckDto(
            new CompletenessCheck(100, List.of()),
            new SignatureCheck(100, List.of()),
            new DateCheck(100, List.of()),
            100
        );

        BiddingCheckReportDto report = service.generateReport(matches, format, new CheckOptions(true, true, true));

        assertEquals(1, report.issues().size());
        assertEquals("MISSING_CLAUSE", report.issues().get(0).type());
    }

    @Test
    @DisplayName("Should generate visualization data")
    void generateReport_shouldGenerateVisualization() {
        ClauseDto clause = new ClauseDto("1", "条款", "内容", false,
            ClauseType.OTHER, 1, 1, "第1条 条款");

        List<MatchResultDto> matches = List.of(
            new MatchResultDto(clause, clause, MatchType.EXACT, 1.0, List.of())
        );

        FormatCheckDto format = new FormatCheckDto(
            new CompletenessCheck(100, List.of()),
            new SignatureCheck(100, List.of()),
            new DateCheck(100, List.of()),
            100
        );

        BiddingCheckReportDto report = service.generateReport(matches, format, new CheckOptions(true, true, true));

        assertNotNull(report.visualization());
        assertNotNull(report.visualization().coverageChart());
        assertNotNull(report.visualization().issueDistribution());
        assertNotNull(report.visualization().riskGauge());
    }
}