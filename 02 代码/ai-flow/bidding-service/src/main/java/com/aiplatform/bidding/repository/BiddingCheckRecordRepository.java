package com.aiplatform.bidding.repository;

import com.aiplatform.bidding.entity.BiddingCheckRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BiddingCheckRecordRepository extends JpaRepository<BiddingCheckRecord, String> {
    List<BiddingCheckRecord> findByTenderDocumentId(String tenderDocumentId);
    List<BiddingCheckRecord> findByBiddingDocumentId(String biddingDocumentId);
    List<BiddingCheckRecord> findByRiskLevel(String riskLevel);
}