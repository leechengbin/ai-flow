package com.aiplatform.bidding.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "suggestions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Suggestion {
    @Id private String id;
    private String issueId;
    @Column(nullable = false) private String documentId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Granularity granularity;
    @Column(columnDefinition = "text") private String originalContent;
    @Column(columnDefinition = "text") private String suggestedContent;
    @Column(columnDefinition = "text") private String explanation;
    @Enumerated(EnumType.STRING) @Builder.Default private SuggestionStatus status = SuggestionStatus.PENDING;
    private String generatedBy;
    @Column(updatable = false) private LocalDateTime createdAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}