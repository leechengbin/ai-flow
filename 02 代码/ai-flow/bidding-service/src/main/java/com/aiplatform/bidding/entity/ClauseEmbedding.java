package com.aiplatform.bidding.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "clause_embeddings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClauseEmbedding {
    @Id
    private String id;

    @Column(name = "clause_id", nullable = false)
    private String clauseId;

    @Column(name = "document_id", nullable = false)
    private String documentId;

    @Column(name = "content_vector", columnDefinition = "vector(1536)")
    private String contentVector;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}