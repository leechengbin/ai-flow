package com.aiplatform.bidding.service;

import com.aiplatform.bidding.domain.entity.BiddingDocument;
import com.aiplatform.bidding.domain.entity.Clause;
import com.aiplatform.bidding.domain.enums.*;
import com.aiplatform.bidding.dto.request.BiddingCheckRequest;
import com.aiplatform.bidding.dto.response.BiddingCheckResponse;
import com.aiplatform.bidding.repository.BiddingDocumentRepository;
import com.aiplatform.bidding.repository.ClauseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BiddingCheckService {
    private final BiddingDocumentRepository documentRepository;
    private final ClauseRepository clauseRepository;
    private static final Pattern STARRED_PATTERN = Pattern.compile("★|☆");

    @Transactional
    public BiddingCheckResponse checkBidding(BiddingCheckRequest request) {
        BiddingDocument document = documentRepository.findById(request.getDocumentId())
            .orElseThrow(() -> new RuntimeException("Document not found: " + request.getDocumentId()));
        List<Clause> biddingClauses = clauseRepository.findByDocumentId(document.getId());
        List<Clause> tenderClauses = extractClauses(request.getTenderRequirements());

        List<BiddingCheckResponse.IssueDto> issues = new ArrayList<>();
        int matched = 0, partiallyMatched = 0, unmatched = 0;

        for (Clause tender : tenderClauses) {
            Optional<Clause> matchedClause = biddingClauses.stream()
                .filter(b -> b.getClauseNumber().equals(tender.getClauseNumber())).findFirst();
            if (matchedClause.isPresent()) {
                matched++;
            } else {
                unmatched++;
                issues.add(BiddingCheckResponse.IssueDto.builder()
                    .issueId("ISSUE-" + UUID.randomUUID())
                    .clauseNumber(tender.getClauseNumber())
                    .issueType(IssueType.MISSING.name())
                    .originalText("(未提供)")
                    .requirementText(tender.getContent())
                    .suggestionText("请补充: " + tender.getContent())
                    .severity(tender.getIsStarred() ? Severity.CRITICAL.name() : Severity.HIGH.name())
                    .eliminationRisk(tender.getIsStarred())
                    .build());
            }
        }

        double score = tenderClauses.isEmpty() ? 100.0 : (matched * 1.0 / tenderClauses.size()) * 100;
        boolean eliminationRisk = issues.stream().anyMatch(i -> i.isEliminationRisk() || "CRITICAL".equals(i.getSeverity()));

        return BiddingCheckResponse.builder()
            .checkId("CHECK-" + UUID.randomUUID())
            .documentId(document.getId())
            .totalClauses(tenderClauses.size())
            .matchedClauses(matched)
            .partiallyMatched(partiallyMatched)
            .unmatched(unmatched)
            .score(score)
            .issues(issues)
            .eliminationRisk(eliminationRisk)
            .riskReasons(issues.stream().filter(BiddingCheckResponse.IssueDto::isEliminationRisk).map(BiddingCheckResponse.IssueDto::getOriginalText).toList())
            .checkedAt(java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
            .build();
    }

    private List<Clause> extractClauses(String text) {
        if (text == null || text.isBlank()) return Collections.emptyList();
        List<Clause> clauses = new ArrayList<>();
        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            clauses.add(Clause.builder()
                .id("CLAUSE-" + UUID.randomUUID())
                .clauseNumber(String.valueOf(i + 1))
                .content(line)
                .isStarred(STARRED_PATTERN.matcher(line).find())
                .clauseType(ClauseType.OTHER)
                .build());
        }
        return clauses;
    }
}