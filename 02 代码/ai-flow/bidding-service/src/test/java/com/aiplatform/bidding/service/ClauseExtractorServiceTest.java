package com.aiplatform.bidding.service;

import com.aiplatform.bidding.dto.response.ClauseDto;
import com.aiplatform.bidding.domain.enums.ClauseType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClauseExtractorServiceTest {

    private ClauseExtractorService service;

    @BeforeEach
    void setUp() {
        service = new ClauseExtractorService();
    }

    @Test
    @DisplayName("Should extract Chinese numbered clauses")
    void extractClauses_shouldExtractChineseNumberedClauses() {
        String text = "第1条 投标人资格要求\n第2条 招标范围\n第3条 ★星号条款";

        List<ClauseDto> clauses = service.extractClauses(text);

        assertEquals(3, clauses.size());
        assertEquals("1", clauses.get(0).clauseNumber());
        assertFalse(clauses.get(0).isStarred());
        assertEquals("3", clauses.get(2).clauseNumber());
        assertTrue(clauses.get(2).isStarred());
    }

    @Test
    @DisplayName("Should extract dot notation clauses")
    void extractClauses_shouldExtractDotNotationClauses() {
        String text = "3.1 技术要求\n3.2 商务条款";

        List<ClauseDto> clauses = service.extractClauses(text);

        assertEquals(2, clauses.size());
        assertEquals("3.1", clauses.get(0).clauseNumber());
        assertEquals("3.2", clauses.get(1).clauseNumber());
    }

    @Test
    @DisplayName("Should handle empty text")
    void extractClauses_shouldHandleEmptyText() {
        List<ClauseDto> clauses = service.extractClauses("");
        assertTrue(clauses.isEmpty());
    }

    @Test
    @DisplayName("Should handle null text")
    void extractClauses_shouldHandleNullText() {
        List<ClauseDto> clauses = service.extractClauses(null);
        assertTrue(clauses.isEmpty());
    }

    @Test
    @DisplayName("Should extract starred clauses with star marker")
    void extractClauses_shouldDetectStarredClauses() {
        String text = "第1条 普通条款\n第2条 ★重点条款\n第3条 ☆也是重点";

        List<ClauseDto> clauses = service.extractClauses(text);

        assertEquals(3, clauses.size());
        assertFalse(clauses.get(0).isStarred());
        assertTrue(clauses.get(1).isStarred());
        assertTrue(clauses.get(2).isStarred());
    }

    @Test
    @DisplayName("Should extract clause title correctly")
    void extractClauses_shouldExtractTitle() {
        String text = "第1条 技术规格要求";

        List<ClauseDto> clauses = service.extractClauses(text);

        assertEquals(1, clauses.size());
        assertEquals("技术规格要求", clauses.get(0).title());
    }

    @Test
    @DisplayName("Should handle multi-level dot notation")
    void extractClauses_shouldHandleMultiLevelDotNotation() {
        String text = "1.2.3 多级条款";

        List<ClauseDto> clauses = service.extractClauses(text);

        assertEquals(1, clauses.size());
        assertEquals("1.2.3", clauses.get(0).clauseNumber());
    }
}