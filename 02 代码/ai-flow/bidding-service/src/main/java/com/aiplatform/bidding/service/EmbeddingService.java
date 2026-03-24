package com.aiplatform.bidding.service;

import com.aiplatform.bidding.entity.ClauseEmbedding;
import com.aiplatform.bidding.repository.ClauseEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {
    private final ClauseEmbeddingRepository embeddingRepository;

    public float[] generateEmbedding(String text) {
        // Simplified implementation - in production would call AI service
        // Returns a mock embedding vector
        if (text == null || text.isBlank()) {
            return new float[1536];
        }
        float[] embedding = new float[1536];
        // Create a simple hash-based embedding for demonstration
        int hash = text.hashCode();
        for (int i = 0; i < 1536; i++) {
            embedding[i] = (float) Math.sin(hash * (i + 1)) * 0.5f;
        }
        return embedding;
    }

    public List<ClauseEmbedding> findSimilarClauses(String documentId, String queryText, int topK) {
        if (documentId == null || queryText == null) {
            log.warn("Null documentId or queryText provided to findSimilarClauses");
            return List.of();
        }

        float[] queryVector = generateEmbedding(queryText);
        String queryVectorStr = vectorToString(queryVector);

        try {
            List<Object[]> results = embeddingRepository.findSimilarClauses(documentId, queryVectorStr, topK);

            List<ClauseEmbedding> similarClauses = new ArrayList<>();
            for (Object[] row : results) {
                ClauseEmbedding emb = new ClauseEmbedding();
                emb.setId((String) row[0]);
                emb.setClauseId((String) row[1]);
                emb.setDocumentId(documentId);
                similarClauses.add(emb);
            }
            return similarClauses;
        } catch (Exception e) {
            log.error("Error finding similar clauses for document: {}", documentId, e);
            return List.of();
        }
    }

    public ClauseEmbedding saveEmbedding(String clauseId, String documentId, String text) {
        float[] embedding = generateEmbedding(text);
        String vectorStr = vectorToString(embedding);

        ClauseEmbedding entity = ClauseEmbedding.builder()
            .id(UUID.randomUUID().toString())
            .clauseId(clauseId)
            .documentId(documentId)
            .contentVector(vectorStr)
            .build();

        return embeddingRepository.save(entity);
    }

    private String vectorToString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}