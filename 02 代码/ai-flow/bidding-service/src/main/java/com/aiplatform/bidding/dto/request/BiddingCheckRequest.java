package com.aiplatform.bidding.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BiddingCheckRequest {
    @NotBlank(message = "documentId is required")
    private String documentId;

    private String tenderRequirements;

    private String tenderFileId;
}
