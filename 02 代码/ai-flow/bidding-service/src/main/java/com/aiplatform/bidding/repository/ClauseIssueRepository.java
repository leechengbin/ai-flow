package com.aiplatform.bidding.repository;

import com.aiplatform.bidding.domain.entity.ClauseIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ClauseIssueRepository extends JpaRepository<ClauseIssue, String> {
    List<ClauseIssue> findByClauseId(String clauseId);
    List<ClauseIssue> findByClauseNumber(String clauseNumber);
    List<ClauseIssue> findBySeverity(String severity);
}
