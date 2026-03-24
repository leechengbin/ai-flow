package com.aiplatform.bidding.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_versions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentVersion {
    @Id private String id;
    @Column(nullable = false) private String documentId;
    @Column(nullable = false) private String versionNumber;
    @Column(nullable = false) private String filePath;
    @Column(columnDefinition = "text") private String diffFromPrevious;
    private String createdBy;
    @Column(updatable = false) private LocalDateTime createdAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}