package com.aiplatform.bidding.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "clause_issues")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClauseIssue {
    @Id private String id;
    private String clauseId;
    private String clauseNumber;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private IssueType issueType;
    @Column(columnDefinition = "text") private String originalText;
    @Column(columnDefinition = "text") private String requirementText;
    @Column(columnDefinition = "text") private String suggestionText;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Severity severity;
    @Builder.Default private Boolean eliminationRisk = false;
    @Column(updatable = false) private LocalDateTime createdAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}