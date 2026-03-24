package com.aiplatform.bidding.service;

import com.aiplatform.bidding.entity.ClauseEmbedding;
import com.aiplatform.bidding.repository.ClauseEmbeddingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock
    private ClauseEmbeddingRepository embeddingRepository;

    private EmbeddingService service;

    @BeforeEach
    void setUp() {
        service = new EmbeddingService(embeddingRepository);
    }

    @Test
    @DisplayName("Should generate embedding vector")
    void generateEmbedding_shouldGenerateVector() {
        float[] embedding = service.generateEmbedding("test text");

        assertEquals(1536, embedding.length);
    }

    @Test
    @DisplayName("Should handle null text in generateEmbedding")
    void generateEmbedding_shouldHandleNullText() {
        float[] embedding = service.generateEmbedding(null);

        assertEquals(1536, embedding.length);
    }

    @Test
    @DisplayName("Should handle empty text in generateEmbedding")
    void generateEmbedding_shouldHandleEmptyText() {
        float[] embedding = service.generateEmbedding("");

        assertEquals(1536, embedding.length);
    }

    @Test
    @DisplayName("Should generate consistent embeddings for same text")
    void generateEmbedding_shouldBeConsistent() {
        float[] embedding1 = service.generateEmbedding("same text");
        float[] embedding2 = service.generateEmbedding("same text");

        assertArrayEquals(embedding1, embedding2);
    }

    @Test
    @DisplayName("Should generate different embeddings for different text")
    void generateEmbedding_shouldDifferForDifferentText() {
        float[] embedding1 = service.generateEmbedding("text one");
        float[] embedding2 = service.generateEmbedding("text two");

        assertFalse(java.util.Arrays.equals(embedding1, embedding2));
    }

    @Test
    @DisplayName("Should find similar clauses")
    void findSimilarClauses_shouldReturnResults() {
        ClauseEmbedding emb = new ClauseEmbedding();
        emb.setId("id1");
        emb.setClauseId("clause1");
        emb.setDocumentId("doc1");

        when(embeddingRepository.findSimilarClauses(eq("doc1"), anyString(), eq(3)))
            .thenReturn(java.util.Arrays.asList(new Object[]{"id1", "clause1", 0.95f}));

        List<ClauseEmbedding> results = service.findSimilarClauses("doc1", "test query", 3);

        assertEquals(1, results.size());
        verify(embeddingRepository).findSimilarClauses(eq("doc1"), anyString(), eq(3));
    }

    @Test
    @DisplayName("Should handle null documentId in findSimilarClauses")
    void findSimilarClauses_shouldHandleNullDocumentId() {
        List<ClauseEmbedding> results = service.findSimilarClauses(null, "test query", 3);

        assertTrue(results.isEmpty());
        verifyNoInteractions(embeddingRepository);
    }

    @Test
    @DisplayName("Should handle null queryText in findSimilarClauses")
    void findSimilarClauses_shouldHandleNullQueryText() {
        List<ClauseEmbedding> results = service.findSimilarClauses("doc1", null, 3);

        assertTrue(results.isEmpty());
        verifyNoInteractions(embeddingRepository);
    }

    @Test
    @DisplayName("Should handle empty queryText in findSimilarClauses")
    void findSimilarClauses_shouldHandleEmptyQueryText() {
        List<ClauseEmbedding> results = service.findSimilarClauses("doc1", "", 3);

        assertTrue(results.isEmpty());
        verifyNoInteractions(embeddingRepository);
    }

    @Test
    @DisplayName("Should handle repository exception in findSimilarClauses")
    void findSimilarClauses_shouldHandleRepositoryException() {
        when(embeddingRepository.findSimilarClauses(anyString(), anyString(), anyInt()))
            .thenThrow(new RuntimeException("Database error"));

        List<ClauseEmbedding> results = service.findSimilarClauses("doc1", "test query", 3);

        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should save embedding")
    void saveEmbedding_shouldSaveEmbedding() {
        ClauseEmbedding saved = new ClauseEmbedding();
        saved.setId("saved-id");
        saved.setClauseId("clause1");
        saved.setDocumentId("doc1");

        when(embeddingRepository.save(any(ClauseEmbedding.class))).thenReturn(saved);

        ClauseEmbedding result = service.saveEmbedding("clause1", "doc1", "test text");

        assertNotNull(result);
        assertEquals("clause1", result.getClauseId());
        assertEquals("doc1", result.getDocumentId());
        verify(embeddingRepository).save(any(ClauseEmbedding.class));
    }
}