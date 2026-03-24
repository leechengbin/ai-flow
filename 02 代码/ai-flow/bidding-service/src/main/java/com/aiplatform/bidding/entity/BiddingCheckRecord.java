package com.aiplatform.bidding.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bidding_check_records")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BiddingCheckRecord {
    @Id
    private String id;

    @Column(name = "tender_document_id")
    private String tenderDocumentId;

    @Column(name = "bidding_document_id")
    private String biddingDocumentId;

    @Column(name = "report_json", columnDefinition = "TEXT")
    private String reportJson;

    @Column(name = "total_score")
    private Double totalScore;

    @Column(name = "coverage_rate")
    private Double coverageRate;

    @Column(name = "format_score")
    private Double formatScore;

    @Column(name = "risk_level")
    private String riskLevel;

    @Column(name = "elimination_risk")
    private Boolean eliminationRisk;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}