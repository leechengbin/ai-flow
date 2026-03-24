package com.aiplatform.bidding.service;

import com.aiplatform.bidding.domain.enums.ClauseType;
import com.aiplatform.bidding.dto.response.ClauseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ClauseExtractorService {

    private static final Pattern STARRED_PATTERN = Pattern.compile("★|☆");
    private static final Pattern CHINESE_CLAUSE_PATTERN = Pattern.compile("第([一二三四五六七八九十百零\\d]+)条");
    private static final Pattern DOT_CLAUSE_PATTERN = Pattern.compile("(\\d+\\.\\d+(?:\\.\\d+)?)");
    private static final Pattern CLAUSE_X_PATTERN = Pattern.compile("(?:条款|Clause)\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final int MAX_LINE_LENGTH_PER_PAGE = 500;

    public List<ClauseDto> extractClauses(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<ClauseDto> clauses = new ArrayList<>();
        String[] lines = text.split("\n");
        int currentPage = 1;

        for (String line : lines) {
            if (line.isBlank()) continue;

            ClauseDto clause = extractClauseFromLine(line, currentPage);
            if (clause != null) {
                clauses.add(clause);
            }

            // Estimate page based on line count (simplified)
            if (line.length() > MAX_LINE_LENGTH_PER_PAGE) {
                currentPage++;
            }
        }

        return clauses;
    }

    private ClauseDto extractClauseFromLine(String line, int currentPage) {
        String clauseNumber = null;
        String title = null;
        int contentStart = 0;

        // Try Chinese clause pattern: 第X条
        Matcher chineseMatcher = CHINESE_CLAUSE_PATTERN.matcher(line);
        if (chineseMatcher.find()) {
            clauseNumber = chineseMatcher.group(1);
            String afterClause = line.substring(chineseMatcher.end());
            title = extractTitle(afterClause);
            contentStart = chineseMatcher.end();
            if (title != null) {
                contentStart += afterClause.length() - afterClause.trim().length();
                String titleMarker = title.trim();
                int titlePos = afterClause.indexOf(titleMarker);
                if (titlePos >= 0) {
                    contentStart = chineseMatcher.end() + titlePos + titleMarker.length();
                }
            }
        }

        // Try dot notation: X.Y.Z
        if (clauseNumber == null) {
            Matcher dotMatcher = DOT_CLAUSE_PATTERN.matcher(line);
            if (dotMatcher.find()) {
                clauseNumber = dotMatcher.group(1);
                String afterDot = line.substring(dotMatcher.end());
                title = extractTitle(afterDot);
                contentStart = dotMatcher.end();
            }
        }

        // Try "条款X" or "Clause X" pattern
        if (clauseNumber == null) {
            Matcher clauseXMatcher = CLAUSE_X_PATTERN.matcher(line);
            if (clauseXMatcher.find()) {
                clauseNumber = clauseXMatcher.group(1);
                String afterClauseX = line.substring(clauseXMatcher.end());
                title = extractTitle(afterClauseX);
                contentStart = clauseXMatcher.end();
            }
        }

        if (clauseNumber == null) {
            return null; // No clause pattern found
        }

        boolean isStarred = isStarred(line);
        String content = line.substring(contentStart).trim();

        return new ClauseDto(
            clauseNumber,
            title,
            content,
            isStarred,
            ClauseType.OTHER,
            currentPage,
            currentPage,
            line
        );
    }

    private String extractTitle(String text) {
        if (text == null || text.isBlank()) return null;
        String trimmed = text.trim();
        // Remove leading separators like ：:,
        if (trimmed.startsWith("：") || trimmed.startsWith(":") || trimmed.startsWith("、")) {
            trimmed = trimmed.substring(1).trim();
        }
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isStarred(String text) {
        return STARRED_PATTERN.matcher(text).find();
    }
}