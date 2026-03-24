package com.aiplatform.bidding.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "review_processes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewProcess {
    @Id private String id;
    @Column(nullable = false) private String documentId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ReviewState currentState;
    @Column(nullable = false) private String submitterId;
    private String reviewerId;
    private LocalDateTime deadline;
    @Column(updatable = false) private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}