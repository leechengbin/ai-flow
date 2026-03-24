package com.aiplatform.bidding.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseRetrieveResponse {
    private List<CaseDto> cases;
    private int total;

    @Data
    @Builder
    public static class CaseDto {
        private String caseId;
        private String tenderTitle;
        private String industry;
        private String region;
        private String winningBidder;
        private BigDecimal bidAmount;
        private String winningDate;
        private double similarityScore;
    }
}
