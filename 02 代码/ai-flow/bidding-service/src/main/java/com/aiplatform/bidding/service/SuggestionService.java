package com.aiplatform.bidding.service;

import com.aiplatform.bidding.domain.entity.ClauseIssue;
import com.aiplatform.bidding.domain.entity.Suggestion;
import com.aiplatform.bidding.domain.enums.Granularity;
import com.aiplatform.bidding.domain.enums.SuggestionStatus;
import com.aiplatform.bidding.dto.request.SuggestionGenerateRequest;
import com.aiplatform.bidding.dto.response.SuggestionGenerateResponse;
import com.aiplatform.bidding.repository.ClauseIssueRepository;
import com.aiplatform.bidding.repository.SuggestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuggestionService {
    private final SuggestionRepository suggestionRepository;
    private final ClauseIssueRepository clauseIssueRepository;

    @Transactional
    public SuggestionGenerateResponse generateSuggestions(SuggestionGenerateRequest request) {
        List<String> issueIds = request.getIssueIds();
        if (issueIds == null || issueIds.isEmpty()) {
            issueIds = clauseIssueRepository.findAll().stream().map(c -> c.getId()).toList();
        }
        List<SuggestionGenerateResponse.SuggestionDto> suggestions = new ArrayList<>();
        for (String issueId : issueIds) {
            ClauseIssue issue = clauseIssueRepository.findById(issueId).orElse(null);
            if (issue == null) continue;
            Suggestion suggestion = Suggestion.builder()
                .id("SUG-" + UUID.randomUUID()).issueId(issueId).documentId(request.getDocumentId())
                .granularity(Granularity.CLAUSE).originalContent(issue.getOriginalText())
                .suggestedContent(issue.getSuggestionText())
                .explanation("基于问题类型 " + issue.getIssueType() + " 生成")
                .status(SuggestionStatus.PENDING).generatedBy("bidding-service").build();
            suggestionRepository.save(suggestion);
            suggestions.add(SuggestionGenerateResponse.SuggestionDto.builder()
                .suggestionId(suggestion.getId()).issueId(issueId)
                .granularity(Granularity.CLAUSE.name()).originalContent(issue.getOriginalText())
                .suggestedContent(issue.getSuggestionText()).explanation(suggestion.getExplanation()).build());
        }
        return SuggestionGenerateResponse.builder()
            .suggestions(suggestions).generatedBy("bidding-service")
            .generatedAt(java.time.LocalDateTime.now().toString()).build();
    }
}