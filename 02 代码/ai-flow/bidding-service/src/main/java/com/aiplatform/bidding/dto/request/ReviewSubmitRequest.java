package com.aiplatform.bidding.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReviewSubmitRequest {
    @NotBlank
    private String reviewerId;

    private String deadline;
}
