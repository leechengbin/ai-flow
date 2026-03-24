package com.aiplatform.bidding.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "state_transitions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StateTransition {
    @Id private String id;
    @Column(nullable = false) private String reviewId;
    @Enumerated(EnumType.STRING) private ReviewState fromState;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ReviewState toState;
    @Column(nullable = false) private String actor;
    private String action;
    @Column(columnDefinition = "text") private String comment;
    @Column(updatable = false) private LocalDateTime timestamp;

    @PrePersist protected void onCreate() { timestamp = LocalDateTime.now(); }
}