package com.aiplatform.bidding.repository;

import com.aiplatform.bidding.domain.entity.StateTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StateTransitionRepository extends JpaRepository<StateTransition, String> {
    List<StateTransition> findByReviewIdOrderByTimestampAsc(String reviewId);
}
