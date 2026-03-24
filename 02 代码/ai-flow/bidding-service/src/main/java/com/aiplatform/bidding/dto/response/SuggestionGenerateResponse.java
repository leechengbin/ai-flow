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
public class SuggestionGenerateResponse {
    private List<SuggestionDto> suggestions;
    private String generatedBy;
    private String generatedAt;

    @Data
    @Builder
    public static class SuggestionDto {
        private String suggestionId;
        private String issueId;
        private String granularity;
        private String originalContent;
        private String suggestedContent;
        private String explanation;
    }
}
