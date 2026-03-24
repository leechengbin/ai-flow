package com.aiplatform.bidding.repository;

import com.aiplatform.bidding.entity.ClauseEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClauseEmbeddingRepository extends JpaRepository<ClauseEmbedding, String> {

    List<ClauseEmbedding> findByDocumentId(String documentId);

    List<ClauseEmbedding> findByClauseId(String clauseId);

    @Query(value = """
        SELECT id, clause_id, 1 - (content_vector <=> cast(:queryVector as vector)) AS similarity
        FROM clause_embeddings
        WHERE document_id = :documentId
        ORDER BY content_vector <=> cast(:queryVector as vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findSimilarClauses(
        @Param("documentId") String documentId,
        @Param("queryVector") String queryVector,
        @Param("limit") int limit
    );
}