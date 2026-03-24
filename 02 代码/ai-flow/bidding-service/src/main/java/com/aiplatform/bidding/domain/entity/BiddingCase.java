package com.aiplatform.bidding.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bidding_cases")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BiddingCase {
    @Id private String id;
    @Column(nullable = false) private String tenderTitle;
    private String industry;
    private String region;
    private String winningBidder;
    @Column(precision = 15, scale = 2) private BigDecimal bidAmount;
    private LocalDate winningDate;
    private String tenderFilePath;
    @Column(columnDefinition = "vector(1536)") private String embedding;
    @Column(updatable = false) private LocalDateTime createdAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}