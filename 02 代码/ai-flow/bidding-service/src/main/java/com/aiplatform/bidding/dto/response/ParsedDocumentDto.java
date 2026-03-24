package com.aiplatform.bidding.dto.response;

import java.time.LocalDate;
import java.util.List;

public record ParsedDocumentDto(
    String documentId,
    String fileName,
    String fileType,
    int totalPages,
    String fullText,
    List<PageContentDto> pages,
    List<SignatureDto> signatures,
    List<DateInfoDto> dates
) {
    public record PageContentDto(int pageNumber, String text) {}
    public record SignatureDto(int pageNumber, float x, float y, float width, float height, String type) {}
    public record DateInfoDto(String rawText, LocalDate date, String context) {}
}