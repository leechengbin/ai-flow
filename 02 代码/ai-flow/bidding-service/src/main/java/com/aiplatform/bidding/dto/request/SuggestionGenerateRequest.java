package com.aiplatform.bidding.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class SuggestionGenerateRequest {
    @NotBlank
    private String documentId;

    private List<String> issueIds;
}
