package com.aiplatform.bidding.repository;

import com.aiplatform.bidding.domain.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, String> {
    List<DocumentVersion> findByDocumentIdOrderByCreatedAtDesc(String documentId);
    Optional<DocumentVersion> findByDocumentIdAndVersionNumber(String documentId, String versionNumber);
}
