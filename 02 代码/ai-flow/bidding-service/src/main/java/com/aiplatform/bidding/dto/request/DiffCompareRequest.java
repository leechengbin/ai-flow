package com.aiplatform.bidding.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DiffCompareRequest {
    @NotBlank
    private String documentId;

    private String versionA;
    private String versionB;
}
