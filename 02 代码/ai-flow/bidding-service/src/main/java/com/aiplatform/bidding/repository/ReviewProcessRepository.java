package com.aiplatform.bidding.repository;

import com.aiplatform.bidding.domain.entity.ReviewProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ReviewProcessRepository extends JpaRepository<ReviewProcess, String> {
    Optional<ReviewProcess> findByDocumentId(String documentId);
}
