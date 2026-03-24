package com.aiplatform.bidding.service;

import com.aiplatform.bidding.dto.response.ClauseDto;
import com.aiplatform.bidding.dto.response.ContentIssueDto;
import com.aiplatform.bidding.dto.response.MatchResultDto;
import com.aiplatform.bidding.domain.enums.Severity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class ClauseMatcherService {

    private static final double EXACT_MATCH_SIMILARITY = 1.0;
    private static final double PARTIAL_MATCH_SIMILARITY = 0.8;

    public List<MatchResultDto> matchClauses(
            List<ClauseDto> tenderClauses,
            List<ClauseDto> biddingClauses) {

        if (tenderClauses == null || biddingClauses == null) {
            log.warn("Null clause list provided - tender: {}, bidding: {}",
                tenderClauses == null, biddingClauses == null);
            return List.of();
        }

        List<MatchResultDto> results = new ArrayList<>();

        for (ClauseDto tender : tenderClauses) {
            Optional<ClauseDto> exactMatch = biddingClauses.stream()
                .filter(b -> b.clauseNumber().equals(tender.clauseNumber()))
                .findFirst();

            if (exactMatch.isPresent()) {
                ClauseDto bidding = exactMatch.get();
                // Check content completeness
                List<ContentIssueDto> issues = checkContentCompleteness(tender, bidding);
                MatchType matchType = issues.isEmpty() ? MatchType.EXACT : MatchType.PARTIAL;
                double similarity = issues.isEmpty() ? EXACT_MATCH_SIMILARITY : PARTIAL_MATCH_SIMILARITY;
                results.add(new MatchResultDto(tender, bidding, matchType, similarity, issues));
            } else {
                // No exact match - report as missing
                results.add(new MatchResultDto(tender, null, MatchType.MISSING, 0.0, List.of()));
            }
        }

        return results;
    }

    private List<ContentIssueDto> checkContentCompleteness(ClauseDto tender, ClauseDto bidding) {
        List<ContentIssueDto> issues = new ArrayList<>();

        if (tender.isStarred() && !bidding.isStarred()) {
            issues.add(new ContentIssueDto(
                "ISSUE-" + UUID.randomUUID().toString(),
                "STARRED_MISSING",
                "星号条款在投标文件中未标记",
                "★ " + tender.title(),
                bidding.title(),
                Severity.HIGH
            ));
        }

        // Check content length difference
        int tenderLength = tender.content() != null ? tender.content().length() : 0;
        int biddingLength = bidding.content() != null ? bidding.content().length() : 0;
        if (tenderLength > 0 && biddingLength < tenderLength * 0.5) {
            issues.add(new ContentIssueDto(
                "ISSUE-" + UUID.randomUUID().toString(),
                "INCOMPLETE_CONTENT",
                "内容明显不完整",
                tender.content(),
                bidding.content(),
                Severity.MEDIUM
            ));
        }

        return issues;
    }
}