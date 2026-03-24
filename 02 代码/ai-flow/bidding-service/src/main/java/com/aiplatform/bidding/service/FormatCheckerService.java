package com.aiplatform.bidding.service;

import com.aiplatform.bidding.domain.enums.Severity;
import com.aiplatform.bidding.dto.response.*;
import com.aiplatform.bidding.dto.response.FormatCheckDto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class FormatCheckerService {

    private static final int DEFAULT_SCORE = 100;
    private static final int MIN_CONTENT_LENGTH = 100;
    private static final int LOW_CONTENT_THRESHOLD = 50;
    private static final int NO_PAGES_THRESHOLD = 30;
    private static final int SIGNATURE_MISSING_THRESHOLD = 50;
    private static final int SIGNATURE_PARTIAL_THRESHOLD = 70;
    private static final int DATE_VALID_THRESHOLD = 80;

    public FormatCheckDto checkFormat(ParsedDocumentDto document) {
        if (document == null) {
            log.warn("Null document provided to checkFormat");
            return new FormatCheckDto(
                new CompletenessCheck(0, List.of()),
                new SignatureCheck(0, List.of()),
                new DateCheck(0, List.of()),
                0
            );
        }

        CompletenessCheck completeness = checkCompleteness(document);
        SignatureCheck signatures = checkSignatures(document);
        DateCheck dates = checkDates(document);

        int totalScore = (completeness.score() + signatures.score() + dates.score()) / 3;

        return new FormatCheckDto(completeness, signatures, dates, totalScore);
    }

    private CompletenessCheck checkCompleteness(ParsedDocumentDto document) {
        List<FormatIssue> issues = new ArrayList<>();
        int score = DEFAULT_SCORE;

        // Check if document has content
        if (document.fullText() == null || document.fullText().isBlank()) {
            issues.add(new FormatIssue("EMPTY_CONTENT", "文档内容为空", Severity.CRITICAL, "全文"));
            score = 0;
        } else {
            // Check minimum content length
            if (document.fullText().length() < MIN_CONTENT_LENGTH) {
                issues.add(new FormatIssue("MINIMAL_CONTENT", "文档内容过少", Severity.MEDIUM, "全文"));
                score = Math.min(score, LOW_CONTENT_THRESHOLD);
            }

            // Check page count
            if (document.totalPages() < 1) {
                issues.add(new FormatIssue("NO_PAGES", "文档页数为0", Severity.HIGH, "全文"));
                score = Math.min(score, NO_PAGES_THRESHOLD);
            }
        }

        return new CompletenessCheck(score, List.copyOf(issues));
    }

    private SignatureCheck checkSignatures(ParsedDocumentDto document) {
        List<SignatureIssue> issues = new ArrayList<>();
        int score = DEFAULT_SCORE;

        // Check if signatures list is empty
        if (document.signatures() == null || document.signatures().isEmpty()) {
            // This is a simplified check - in real implementation would check specific pages
            issues.add(new SignatureIssue("COMPANY_SEAL", 1, "MISSING", "未检测到公章"));
            score = Math.min(score, SIGNATURE_MISSING_THRESHOLD);
        } else {
            // Check for company seal
            boolean hasSeal = document.signatures().stream()
                .anyMatch(s -> "COMPANY_SEAL".equals(s.type()));
            if (!hasSeal) {
                issues.add(new SignatureIssue("COMPANY_SEAL", 1, "MISSING", "未检测到公章"));
                score = Math.min(score, SIGNATURE_PARTIAL_THRESHOLD);
            }
        }

        return new SignatureCheck(score, List.copyOf(issues));
    }

    private DateCheck checkDates(ParsedDocumentDto document) {
        List<DateIssue> issues = new ArrayList<>();
        int score = DEFAULT_SCORE;

        if (document.dates() == null || document.dates().isEmpty()) {
            // No dates found - informational only
            return new DateCheck(DEFAULT_SCORE, List.of());
        }

        // Check if dates are parseable (already done in parser)
        long invalidDates = document.dates().stream()
            .filter(d -> d.date() == null)
            .count();

        if (invalidDates > 0) {
            issues.add(new DateIssue("UNPARSEABLE_DATE",
                String.valueOf(invalidDates) + " dates could not be parsed",
                "部分日期格式无法解析",
                Severity.MEDIUM));
            score = Math.min(score, DATE_VALID_THRESHOLD);
        }

        return new DateCheck(score, List.copyOf(issues));
    }
}