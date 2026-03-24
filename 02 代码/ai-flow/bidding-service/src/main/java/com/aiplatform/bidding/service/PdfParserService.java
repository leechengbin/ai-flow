package com.aiplatform.bidding.service;

import com.aiplatform.bidding.dto.response.ParsedDocumentDto;
import com.aiplatform.bidding.dto.response.ParsedDocumentDto.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class PdfParserService implements DocumentParserService {

    private static final Pattern DATE_PATTERN = Pattern.compile(
        "(\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2}[日]?)"
    );

    @Override
    public ParsedDocumentDto parse(byte[] content, String fileName) {
        try (PDDocument document = Loader.loadPDF(content)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            int totalPages = document.getNumberOfPages();
            List<PageContentDto> pages = new ArrayList<>();
            List<DateInfoDto> dates = new ArrayList<>();
            StringBuilder fullTextBuilder = new StringBuilder();

            for (int i = 1; i <= totalPages; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String pageText = stripper.getText(document);
                pages.add(new PageContentDto(i, pageText));
                fullTextBuilder.append(pageText).append("\n");

                // Extract dates
                Matcher matcher = DATE_PATTERN.matcher(pageText);
                while (matcher.find()) {
                    try {
                        String dateStr = matcher.group(1).replace("年", "-").replace("月", "-").replace("日", "");
                        LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        dates.add(new DateInfoDto(matcher.group(1), date, "page " + i));
                    } catch (Exception e) {
                        // Skip invalid dates
                    }
                }
            }

            return new ParsedDocumentDto(
                UUID.randomUUID().toString(),
                fileName,
                "PDF",
                totalPages,
                fullTextBuilder.toString(),
                pages,
                List.of(), // Signatures - simplified for now
                dates
            );
        } catch (IOException e) {
            log.error("Failed to parse PDF: {}", fileName, e);
            throw new RuntimeException("Failed to parse PDF: " + fileName, e);
        }
    }

    @Override
    public boolean supports(String fileType) {
        return "pdf".equalsIgnoreCase(fileType);
    }
}