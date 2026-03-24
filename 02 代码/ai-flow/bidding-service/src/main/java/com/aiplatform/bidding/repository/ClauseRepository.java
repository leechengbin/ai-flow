package com.aiplatform.bidding.repository;

import com.aiplatform.bidding.domain.entity.Clause;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ClauseRepository extends JpaRepository<Clause, String> {
    List<Clause> findByDocumentId(String documentId);
    List<Clause> findByDocumentIdAndIsStarredTrue(String documentId);
}
