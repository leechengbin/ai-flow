package com.aiplatform.bidding.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiddingCheckResponse {
    private String checkId;
    private String documentId;
    private int totalClauses;
    private int matchedClauses;
    private int partiallyMatched;
    private int unmatched;
    private double score;
    private List<IssueDto> issues;
    private boolean eliminationRisk;
    private List<String> riskReasons;
    private String checkedAt;

    @Data
    @Builder
    public static class IssueDto {
        private String issueId;
        private String clauseNumber;
        private String issueType;
        private String originalText;
        private String requirementText;
        private String suggestionText;
        private String severity;
        private boolean eliminationRisk;
    }
}
