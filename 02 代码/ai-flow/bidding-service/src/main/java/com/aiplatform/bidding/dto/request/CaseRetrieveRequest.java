package com.aiplatform.bidding.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class CaseRetrieveRequest {
    private String query;
    private String industry;
    private String region;
    private String dateFrom;
    private String dateTo;

    @Min(1)
    @Max(20)
    private Integer topK = 5;
}
