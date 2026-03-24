package com.aiplatform.bidding.service;

import com.aiplatform.bidding.dto.response.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FormatCheckerServiceTest {

    private FormatCheckerService service;

    @BeforeEach
    void setUp() {
        service = new FormatCheckerService();
    }

    @Test
    @DisplayName("Should return zero score for null document")
    void checkFormat_shouldHandleNullDocument() {
        FormatCheckDto result = service.checkFormat(null);
        assertEquals(0, result.totalScore());
        assertEquals(0, result.completeness().score());
        assertEquals(0, result.signatures().score());
        assertEquals(0, result.dates().score());
    }

    @Test
    @DisplayName("Should detect empty content")
    void checkFormat_shouldDetectEmptyContent() {
        ParsedDocumentDto doc = new ParsedDocumentDto(
            "doc1", "test.pdf", "PDF", 0, "", List.of(), List.of(), List.of()
        );

        FormatCheckDto result = service.checkFormat(doc);

        assertEquals(0, result.completeness().score());
        assertFalse(result.completeness().issues().isEmpty());
    }

    @Test
    @DisplayName("Should check signatures")
    void checkFormat_shouldCheckSignatures() {
        ParsedDocumentDto doc = new ParsedDocumentDto(
            "doc1", "test.pdf", "PDF", 10, "正文内容", List.of(), List.of(), List.of()
        );

        FormatCheckDto result = service.checkFormat(doc);

        assertTrue(result.signatures().score() < 100);
        assertFalse(result.signatures().issues().isEmpty());
    }

    @Test
    @DisplayName("Should detect blank content")
    void checkFormat_shouldDetectBlankContent() {
        ParsedDocumentDto doc = new ParsedDocumentDto(
            "doc1", "test.pdf", "PDF", 1, "   \n\t  ", List.of(), List.of(), List.of()
        );

        FormatCheckDto result = service.checkFormat(doc);

        assertEquals(0, result.completeness().score());
    }

    @Test
    @DisplayName("Should detect minimal content")
    void checkFormat_shouldDetectMinimalContent() {
        ParsedDocumentDto doc = new ParsedDocumentDto(
            "doc1", "test.pdf", "PDF", 1, "short", List.of(), List.of(), List.of()
        );

        FormatCheckDto result = service.checkFormat(doc);

        assertTrue(result.completeness().score() < 100);
        assertTrue(result.completeness().issues().stream()
            .anyMatch(i -> i.type().equals("MINIMAL_CONTENT")));
    }

    @Test
    @DisplayName("Should detect zero pages")
    void checkFormat_shouldDetectZeroPages() {
        ParsedDocumentDto doc = new ParsedDocumentDto(
            "doc1", "test.pdf", "PDF", 0, "some content that is longer than 100 characters to pass the length check but has no pages", List.of(), List.of(), List.of()
        );

        FormatCheckDto result = service.checkFormat(doc);

        assertTrue(result.completeness().score() < 100);
        assertTrue(result.completeness().issues().stream()
            .anyMatch(i -> i.type().equals("NO_PAGES")));
    }

    @Test
    @DisplayName("Should pass completeness check for valid document")
    void checkFormat_shouldPassForValidDocument() {
        ParsedDocumentDto doc = new ParsedDocumentDto(
            "doc1", "test.pdf", "PDF", 5,
            "This is a valid document with sufficient content to pass all checks. " +
            "It contains more than 100 characters which is the minimum required length. " +
            "The document also has multiple pages and proper structure.",
            List.of(), List.of(), List.of()
        );

        FormatCheckDto result = service.checkFormat(doc);

        assertEquals(100, result.completeness().score());
        assertTrue(result.completeness().issues().isEmpty());
    }

    @Test
    @DisplayName("Should calculate total score correctly")
    void checkFormat_shouldCalculateTotalScoreCorrectly() {
        ParsedDocumentDto doc = new ParsedDocumentDto(
            "doc1", "test.pdf", "PDF", 5,
            "Valid content that is longer than 100 characters to pass all basic checks. " +
            "This document has enough text to satisfy the minimum content length requirement.",
            List.of(), List.of(), List.of()
        );

        FormatCheckDto result = service.checkFormat(doc);

        // All scores should be 100 for a valid document
        assertEquals(100, result.totalScore());
    }
}