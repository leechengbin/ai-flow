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
public class DiffCompareResponse {
    private String documentId;
    private String versionA;
    private String versionB;
    private SummaryDto summary;
    private List<ChangeDto> details;

    @Data
    @Builder
    public static class SummaryDto {
        private int totalChanges;
        private int added;
        private int modified;
        private int deleted;
    }

    @Data
    @Builder
    public static class ChangeDto {
        private String clauseNumber;
        private String changeType;
        private String oldContent;
        private String newContent;
        private Integer pageNumber;
    }
}
