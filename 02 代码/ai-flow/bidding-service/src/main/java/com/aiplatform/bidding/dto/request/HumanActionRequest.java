package com.aiplatform.bidding.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class HumanActionRequest {
    @NotBlank
    private String action;  // APPROVE, REJECT, REQUEST_REVISION

    private String comment;

    private List<String> approvedIssueIds;

    private List<String> rejectedIssueIds;

    private List<String> issueIdsToRequestRevision;
}
