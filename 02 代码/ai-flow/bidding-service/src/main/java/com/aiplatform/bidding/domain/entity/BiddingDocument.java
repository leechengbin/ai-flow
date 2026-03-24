package com.aiplatform.bidding.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bidding_documents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BiddingDocument {
    @Id private String id;
    @Column(nullable = false) private String title;
    private String projectId;
    @Column(nullable = false) private String uploaderId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) @Builder.Default
    private DocumentStatus status = DocumentStatus.DRAFT;
    private String currentVersion;
    @Column(columnDefinition = "text") private String tenderRequirements;
    private String tenderFileId;
    @Column(updatable = false) private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}

enum DocumentStatus { DRAFT, SUBMITTED, APPROVED, REJECTED }