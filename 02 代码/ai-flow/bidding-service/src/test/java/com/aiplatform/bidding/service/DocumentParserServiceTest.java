package com.aiplatform.bidding.service;

import com.aiplatform.bidding.exception.DocumentParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentParserServiceTest {

    private PdfParserService pdfParserService;
    private DocxParserService docxParserService;

    @BeforeEach
    void setUp() {
        pdfParserService = new PdfParserService();
        docxParserService = new DocxParserService();
    }

    @Test
    @DisplayName("PdfParserService should support PDF files")
    void pdfParser_shouldSupportPdf() {
        assertTrue(pdfParserService.supports("pdf"));
        assertFalse(pdfParserService.supports("docx"));
    }

    @Test
    @DisplayName("DocxParserService should support DOCX files")
    void docxParser_shouldSupportDocx() {
        assertTrue(docxParserService.supports("docx"));
        assertFalse(docxParserService.supports("pdf"));
    }

    @Test
    @DisplayName("PdfParserService should throw on null content")
    void pdfParser_shouldThrowOnNullContent() {
        assertThrows(DocumentParseException.class, () -> pdfParserService.parse(null, "test.pdf"));
    }

    @Test
    @DisplayName("DocxParserService should throw on null content")
    void docxParser_shouldThrowOnNullContent() {
        assertThrows(DocumentParseException.class, () -> docxParserService.parse(null, "test.docx"));
    }

    @Test
    @DisplayName("PdfParserService should throw on empty content")
    void pdfParser_shouldThrowOnEmptyContent() {
        assertThrows(DocumentParseException.class, () -> pdfParserService.parse(new byte[0], "test.pdf"));
    }

    @Test
    @DisplayName("DocxParserService should throw on empty content")
    void docxParser_shouldThrowOnEmptyContent() {
        assertThrows(DocumentParseException.class, () -> docxParserService.parse(new byte[0], "test.docx"));
    }

    @Test
    @DisplayName("PdfParserService supports should be case insensitive")
    void pdfParser_supportsShouldBeCaseInsensitive() {
        assertTrue(pdfParserService.supports("PDF"));
        assertTrue(pdfParserService.supports("Pdf"));
    }

    @Test
    @DisplayName("DocxParserService supports should be case insensitive")
    void docxParser_supportsShouldBeCaseInsensitive() {
        assertTrue(docxParserService.supports("DOCX"));
        assertTrue(docxParserService.supports("Docx"));
    }
}