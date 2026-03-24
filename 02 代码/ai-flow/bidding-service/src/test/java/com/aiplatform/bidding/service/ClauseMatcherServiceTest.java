package com.aiplatform.bidding.service;

import com.aiplatform.bidding.dto.response.ClauseDto;
import com.aiplatform.bidding.dto.response.MatchResultDto;
import com.aiplatform.bidding.domain.enums.ClauseType;
import com.aiplatform.bidding.dto.response.MatchType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClauseMatcherServiceTest {

    private ClauseMatcherService service;

    @BeforeEach
    void setUp() {
        service = new ClauseMatcherService();
    }

    @Test
    @DisplayName("Should match exact clause by number")
    void matchClauses_shouldMatchExactByNumber() {
        List<ClauseDto> tender = List.of(
            new ClauseDto("3.1", "技术要求", "内容", false, ClauseType.OTHER, 1, 1, "3.1 技术要求")
        );
        List<ClauseDto> bidding = List.of(
            new ClauseDto("3.1", "技术要求", "内容", false, ClauseType.OTHER, 1, 1, "3.1 技术要求")
        );

        List<MatchResultDto> results = service.matchClauses(tender, bidding);

        assertEquals(1, results.size());
        assertEquals(MatchType.EXACT, results.get(0).matchType());
    }

    @Test
    @DisplayName("Should report missing clause")
    void matchClauses_shouldReportMissing() {
        List<ClauseDto> tender = List.of(
            new ClauseDto("3.1", "技术要求", "内容", false, ClauseType.OTHER, 1, 1, "3.1 技术要求")
        );
        List<ClauseDto> bidding = List.of();

        List<MatchResultDto> results = service.matchClauses(tender, bidding);

        assertEquals(1, results.size());
        assertEquals(MatchType.MISSING, results.get(0).matchType());
    }

    @Test
    @DisplayName("Should handle null lists")
    void matchClauses_shouldHandleNullLists() {
        List<MatchResultDto> results = service.matchClauses(null, null);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should handle null tender list")
    void matchClauses_shouldHandleNullTenderList() {
        List<ClauseDto> bidding = List.of(
            new ClauseDto("3.1", "技术要求", "内容", false, ClauseType.OTHER, 1, 1, "3.1 技术要求")
        );

        List<MatchResultDto> results = service.matchClauses(null, bidding);

        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should handle null bidding list")
    void matchClauses_shouldHandleNullBiddingList() {
        List<ClauseDto> tender = List.of(
            new ClauseDto("3.1", "技术要求", "内容", false, ClauseType.OTHER, 1, 1, "3.1 技术要求")
        );

        List<MatchResultDto> results = service.matchClauses(tender, null);

        assertEquals(1, results.size());
        assertEquals(MatchType.MISSING, results.get(0).matchType());
    }

    @Test
    @DisplayName("Should detect starred clause missing in bidding")
    void matchClauses_shouldDetectStarredMissing() {
        List<ClauseDto> tender = List.of(
            new ClauseDto("3.1", "重点条款", "内容", true, ClauseType.OTHER, 1, 1, "3.1 ★重点条款")
        );
        List<ClauseDto> bidding = List.of(
            new ClauseDto("3.1", "重点条款", "", false, ClauseType.OTHER, 1, 1, "3.1 重点条款")
        );

        List<MatchResultDto> results = service.matchClauses(tender, bidding);

        assertEquals(1, results.size());
        assertEquals(MatchType.PARTIAL, results.get(0).matchType());
        assertFalse(results.get(0).contentIssues().isEmpty());
    }

    @Test
    @DisplayName("Should match multiple clauses")
    void matchClauses_shouldMatchMultipleClauses() {
        List<ClauseDto> tender = List.of(
            new ClauseDto("1", "条款1", "内容1", false, ClauseType.OTHER, 1, 1, "第1条 条款1"),
            new ClauseDto("2", "条款2", "内容2", false, ClauseType.OTHER, 1, 1, "第2条 条款2"),
            new ClauseDto("3", "条款3", "内容3", false, ClauseType.OTHER, 1, 1, "第3条 条款3")
        );
        List<ClauseDto> bidding = List.of(
            new ClauseDto("1", "条款1", "内容1", false, ClauseType.OTHER, 1, 1, "第1条 条款1"),
            new ClauseDto("3", "条款3", "内容3", false, ClauseType.OTHER, 1, 1, "第3条 条款3")
        );

        List<MatchResultDto> results = service.matchClauses(tender, bidding);

        assertEquals(3, results.size());
        assertEquals(MatchType.EXACT, results.get(0).matchType());
        assertEquals(MatchType.MISSING, results.get(1).matchType());
        assertEquals(MatchType.EXACT, results.get(2).matchType());
    }
}