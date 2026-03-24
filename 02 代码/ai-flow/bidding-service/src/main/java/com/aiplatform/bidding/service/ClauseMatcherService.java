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

@Service
@Slf4j
public class ClauseMatcherService {

    public List<MatchResultDto> matchClauses(
            List<ClauseDto> tenderClauses,
            List<ClauseDto> biddingClauses) {

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
                double similarity = issues.isEmpty() ? 1.0 : 0.8;
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
                "ISSUE-" + System.currentTimeMillis(),
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
                "ISSUE-" + System.currentTimeMillis(),
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