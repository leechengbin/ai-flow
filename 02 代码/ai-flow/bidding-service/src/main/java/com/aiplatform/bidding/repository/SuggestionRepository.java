package com.aiplatform.bidding.repository;

import com.aiplatform.bidding.domain.entity.Suggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SuggestionRepository extends JpaRepository<Suggestion, String> {
    List<Suggestion> findByDocumentId(String documentId);
    List<Suggestion> findByIssueId(String issueId);
    List<Suggestion> findByStatus(String status);
}
