package com.aiplatform.bidding.domain.entity;

import com.aiplatform.bidding.domain.enums.ResponseStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "clauses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Clause {
    @Id private String id;
    @Column(nullable = false) private String documentId;
    private String clauseNumber;
    private String title;
    @Column(columnDefinition = "text", nullable = false) private String content;
    @Builder.Default private Boolean isStarred = false;
    @Enumerated(EnumType.STRING) private ClauseType clauseType;
    @Enumerated(EnumType.STRING) private ResponseStatus responseStatus;
    private Integer pageNumber;
    private Integer startPage;
    private Integer endPage;
    @Column(updatable = false) private LocalDateTime createdAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}