package com.aiplatform.bidding.repository;

import com.aiplatform.bidding.domain.entity.BiddingCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BiddingCaseRepository extends JpaRepository<BiddingCase, String> {
    List<BiddingCase> findByIndustry(String industry);
    List<BiddingCase> findByRegion(String region);
    List<BiddingCase> findByWinningBidderContaining(String bidder);

    @Query(value = "SELECT * FROM bidding_cases ORDER BY embedding <=> CAST(:embedding AS vector) LIMIT :topK", nativeQuery = true)
    List<BiddingCase> findSimilarCases(@Param("embedding") String embedding, @Param("topK") int topK);
}
