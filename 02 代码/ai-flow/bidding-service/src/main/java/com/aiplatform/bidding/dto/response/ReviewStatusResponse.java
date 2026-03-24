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
public class ReviewStatusResponse {
    private String reviewId;
    private String documentId;
    private String currentState;
    private String submitterId;
    private String reviewerId;
    private String deadline;
    private List<HistoryDto> history;
    private List<String> pendingIssues;
    private List<String> approvedIssues;
    private List<String> rejectedIssues;

    @Data
    @Builder
    public static class HistoryDto {
        private String fromState;
        private String toState;
        private String actor;
        private String action;
        private String timestamp;
    }
}
