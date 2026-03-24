package com.aiplatform.bidding.service;

import com.aiplatform.bidding.domain.entity.BiddingDocument;
import com.aiplatform.bidding.repository.BiddingDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("BiddingDocumentRepository Tests")
class BiddingDocumentRepositoryTest {

    @Autowired
    private BiddingDocumentRepository repository;

    private BiddingDocument document1;
    private BiddingDocument document2;
    private BiddingDocument document3;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        document1 = BiddingDocument.builder()
            .id("DOC-001")
            .title("Project Alpha Bid")
            .projectId("PROJ-A")
            .uploaderId("user-1")
            .status(BiddingDocument.DocumentStatus.DRAFT)
            .build();

        document2 = BiddingDocument.builder()
            .id("DOC-002")
            .title("Project Beta Bid")
            .projectId("PROJ-B")
            .uploaderId("user-1")
            .status(BiddingDocument.DocumentStatus.SUBMITTED)
            .build();

        document3 = BiddingDocument.builder()
            .id("DOC-003")
            .title("Project Gamma Bid")
            .projectId("PROJ-A")
            .uploaderId("user-2")
            .status(BiddingDocument.DocumentStatus.APPROVED)
            .build();

        repository.saveAll(List.of(document1, document2, document3));
    }

    @Nested
    @DisplayName("findByUploaderId")
    class FindByUploaderId {

        @Test
        @DisplayName("Returns all documents uploaded by a specific user")
        void findByUploaderId_existingUser_returnsDocuments() {
            List<BiddingDocument> results = repository.findByUploaderId("user-1");

            assertThat(results).hasSize(2);
            assertThat(results).extracting(BiddingDocument::getId)
                .containsExactlyInAnyOrder("DOC-001", "DOC-002");
        }

        @Test
        @DisplayName("Returns empty list when user has no documents")
        void findByUploaderId_nonexistentUser_returnsEmptyList() {
            List<BiddingDocument> results = repository.findByUploaderId("nonexistent-user");

            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByProjectId")
    class FindByProjectId {

        @Test
        @DisplayName("Returns all documents for a specific project")
        void findByProjectId_existingProject_returnsDocuments() {
            List<BiddingDocument> results = repository.findByProjectId("PROJ-A");

            assertThat(results).hasSize(2);
            assertThat(results).extracting(BiddingDocument::getId)
                .containsExactlyInAnyOrder("DOC-001", "DOC-003");
        }

        @Test
        @DisplayName("Returns empty list for project with no documents")
        void findByProjectId_nonexistentProject_returnsEmptyList() {
            List<BiddingDocument> results = repository.findByProjectId("NONEXISTENT");

            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByStatus")
    class FindByStatus {

        @Test
        @DisplayName("Returns all documents with a specific status")
        void findByStatus_existingStatus_returnsDocuments() {
            List<BiddingDocument> results = repository.findByStatus("DRAFT");

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getId()).isEqualTo("DOC-001");
        }

        @Test
        @DisplayName("Returns empty list for status with no documents")
        void findByStatus_nonexistentStatus_returnsEmptyList() {
            List<BiddingDocument> results = repository.findByStatus("ARCHIVED");

            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperations {

        @Test
        @DisplayName("save creates new document")
        void save_createsNewDocument() {
            BiddingDocument newDoc = BiddingDocument.builder()
                .id("DOC-NEW")
                .title("New Document")
                .projectId("PROJ-C")
                .uploaderId("user-3")
                .status(BiddingDocument.DocumentStatus.DRAFT)
                .build();

            BiddingDocument saved = repository.save(newDoc);

            assertThat(saved.getId()).isEqualTo("DOC-NEW");
            assertThat(repository.findById("DOC-NEW")).isPresent();
        }

        @Test
        @DisplayName("save updates existing document")
        void save_updatesExistingDocument() {
            document1.setTitle("Updated Title");
            document1.setStatus(BiddingDocument.DocumentStatus.SUBMITTED);

            BiddingDocument updated = repository.save(document1);

            assertThat(updated.getTitle()).isEqualTo("Updated Title");
            assertThat(updated.getStatus()).isEqualTo(BiddingDocument.DocumentStatus.SUBMITTED);
        }

        @Test
        @DisplayName("deleteById removes document")
        void deleteById_removesDocument() {
            repository.deleteById("DOC-001");

            assertThat(repository.findById("DOC-001")).isEmpty();
        }

        @Test
        @DisplayName("findAll returns all documents")
        void findAll_returnsAllDocuments() {
            List<BiddingDocument> all = repository.findAll();

            assertThat(all).hasSize(3);
        }
    }
}
