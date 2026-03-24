package com.aiplatform.bidding.repository;

import com.aiplatform.bidding.domain.entity.BiddingDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BiddingDocumentRepository extends JpaRepository<BiddingDocument, String> {
    List<BiddingDocument> findByUploaderId(String uploaderId);
    List<BiddingDocument> findByProjectId(String projectId);
    List<BiddingDocument> findByStatus(String status);
}
