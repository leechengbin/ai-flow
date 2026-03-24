package com.aiplatform.bidding.service;

import com.aiplatform.bidding.dto.response.ParsedDocumentDto;
import com.aiplatform.bidding.dto.response.ParsedDocumentDto.*;
import com.aiplatform.bidding.exception.DocumentParseException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
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
public class DocxParserService implements DocumentParserService {

    private static final Pattern DATE_PATTERN = Pattern.compile(
        "(\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2}[日]?)"
    );

    @Override
    public ParsedDocumentDto parse(byte[] content, String fileName) {
        if (content == null || content.length == 0) {
            throw new DocumentParseException("Document content is null or empty: " + fileName, null);
        }
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(content))) {
            List<PageContentDto> pages = new ArrayList<>();
            List<DateInfoDto> dates = new ArrayList<>();
            StringBuilder fullTextBuilder = new StringBuilder();
            int pageNumber = 1;

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.isBlank()) {
                    pages.add(new PageContentDto(pageNumber++, text));
                    fullTextBuilder.append(text).append("\n");

                    // Extract dates
                    Matcher matcher = DATE_PATTERN.matcher(text);
                    while (matcher.find()) {
                        try {
                            String dateStr = matcher.group(1).replace("年", "-").replace("月", "-").replace("日", "");
                            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                            dates.add(new DateInfoDto(matcher.group(1), date, text.substring(0, Math.min(50, text.length()))));
                        } catch (Exception e) {
                            log.warn("Failed to parse date: {}", e.getMessage());
                        }
                    }
                }
            }

            return new ParsedDocumentDto(
                UUID.randomUUID().toString(),
                fileName,
                "DOCX",
                pages.size(),
                fullTextBuilder.toString(),
                pages,
                List.of(), // Signatures - simplified for now
                dates
            );
        } catch (IOException e) {
            log.error("Failed to parse DOCX: {}", fileName, e);
            throw new DocumentParseException("Failed to parse DOCX: " + fileName, e);
        }
    }

    @Override
    public boolean supports(String fileType) {
        return "docx".equalsIgnoreCase(fileType);
    }
}