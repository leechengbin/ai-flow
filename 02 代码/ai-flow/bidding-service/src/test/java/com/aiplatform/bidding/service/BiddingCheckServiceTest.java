package com.aiplatform.bidding.service;

import com.aiplatform.bidding.domain.entity.BiddingDocument;
import com.aiplatform.bidding.domain.entity.Clause;
import com.aiplatform.bidding.domain.enums.ClauseType;
import com.aiplatform.bidding.dto.request.BiddingCheckRequest;
import com.aiplatform.bidding.dto.response.BiddingCheckResponse;
import com.aiplatform.bidding.repository.BiddingDocumentRepository;
import com.aiplatform.bidding.repository.ClauseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BiddingCheckService Tests")
class BiddingCheckServiceTest {

    @Mock
    private BiddingDocumentRepository documentRepository;

    @Mock
    private ClauseRepository clauseRepository;

    @InjectMocks
    private BiddingCheckService biddingCheckService;

    private BiddingDocument sampleDocument;
    private BiddingCheckRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleDocument = BiddingDocument.builder()
            .id("DOC-001")
            .title("Test Bidding Document")
            .uploaderId("user-123")
            .status(BiddingDocument.DocumentStatus.DRAFT)
            .build();

        sampleRequest = new BiddingCheckRequest();
        sampleRequest.setDocumentId("DOC-001");
    }

    @Nested
    @DisplayName("checkBidding - Basic Functionality")
    class CheckBiddingBasic {

        @Test
        @DisplayName("checkBidding throws when document not found")
        void checkBidding_documentNotFound_throws() {
            when(documentRepository.findById("DOC-001")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> biddingCheckService.checkBidding(sampleRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Document not found: DOC-001");
        }

        @Test
        @DisplayName("checkBidding returns 100% score when all tender clauses match")
        void checkBidding_allClausesMatch_fullScore() {
            sampleRequest.setTenderRequirements("第一条：这是要求1\n第二条：这是要求2");

            when(documentRepository.findById("DOC-001")).thenReturn(Optional.of(sampleDocument));
            when(clauseRepository.findByDocumentId("DOC-001")).thenReturn(List.of(
                Clause.builder().id("C1").documentId("DOC-001").clauseNumber("1").content("第一条：这是要求1").build(),
                Clause.builder().id("C2").documentId("DOC-001").clauseNumber("2").content("第二条：这是要求2").build()
            ));

            BiddingCheckResponse response = biddingCheckService.checkBidding(sampleRequest);

            assertThat(response.getScore()).isEqualTo(100.0);
            assertThat(response.getMatchedClauses()).isEqualTo(2);
            assertThat(response.getUnmatched()).isZero();
            assertThat(response.getTotalClauses()).isEqualTo(2);
            assertThat(response.isEliminationRisk()).isFalse();
        }

        @Test
        @DisplayName("checkBidding returns lower score when clauses are missing")
        void checkBidding_missingClauses_lowerScore() {
            sampleRequest.setTenderRequirements("第一条：这是要求1\n第二条：这是要求2\n第三条：这是要求3");

            when(documentRepository.findById("DOC-001")).thenReturn(Optional.of(sampleDocument));
            when(clauseRepository.findByDocumentId("DOC-001")).thenReturn(List.of(
                Clause.builder().id("C1").documentId("DOC-001").clauseNumber("1").content("第一条：这是要求1").build()
            ));

            BiddingCheckResponse response = biddingCheckService.checkBidding(sampleRequest);

            assertThat(response.getScore()).isCloseTo(33.33, within(0.01));
            assertThat(response.getMatchedClauses()).isEqualTo(1);
            assertThat(response.getUnmatched()).isEqualTo(2);
            assertThat(response.getTotalClauses()).isEqualTo(3);
            assertThat(response.getIssues()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("checkBidding - Edge Cases")
    class CheckBiddingEdgeCases {

        @Test
        @DisplayName("checkBidding returns 100% when tender requirements is empty")
        void checkBidding_emptyTenderRequirements_fullScore() {
            sampleRequest.setTenderRequirements("");

            when(documentRepository.findById("DOC-001")).thenReturn(Optional.of(sampleDocument));
            when(clauseRepository.findByDocumentId("DOC-001")).thenReturn(Collections.emptyList());

            BiddingCheckResponse response = biddingCheckService.checkBidding(sampleRequest);

            assertThat(response.getScore()).isEqualTo(100.0);
            assertThat(response.getTotalClauses()).isZero();
        }

        @Test
        @DisplayName("checkBidding returns 100% when tender requirements is null")
        void checkBidding_nullTenderRequirements_fullScore() {
            sampleRequest.setTenderRequirements(null);

            when(documentRepository.findById("DOC-001")).thenReturn(Optional.of(sampleDocument));
            when(clauseRepository.findByDocumentId("DOC-001")).thenReturn(Collections.emptyList());

            BiddingCheckResponse response = biddingCheckService.checkBidding(sampleRequest);

            assertThat(response.getScore()).isEqualTo(100.0);
            assertThat(response.getTotalClauses()).isZero();
        }

        @Test
        @DisplayName("checkBidding returns 100% when tender requirements is blank")
        void checkBidding_blankTenderRequirements_fullScore() {
            sampleRequest.setTenderRequirements("   \n\t  ");

            when(documentRepository.findById("DOC-001")).thenReturn(Optional.of(sampleDocument));
            when(clauseRepository.findByDocumentId("DOC-001")).thenReturn(Collections.emptyList());

            BiddingCheckResponse response = biddingCheckService.checkBidding(sampleRequest);

            assertThat(response.getScore()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("checkBidding returns empty issues when no bidding clauses exist")
        void checkBidding_noBiddingClauses_createsIssues() {
            sampleRequest.setTenderRequirements("第一条：这是要求1\n第二条：这是要求2");

            when(documentRepository.findById("DOC-001")).thenReturn(Optional.of(sampleDocument));
            when(clauseRepository.findByDocumentId("DOC-001")).thenReturn(Collections.emptyList());

            BiddingCheckResponse response = biddingCheckService.checkBidding(sampleRequest);

            assertThat(response.getScore()).isZero();
            assertThat(response.getIssues()).hasSize(2);
            assertThat(response.getUnmatched()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("checkBidding - Starred Clauses (Critical Issues)")
    class CheckBiddingStarredClauses {

        @Test
        @DisplayName("Starred clause missing sets elimination risk to true")
        void checkBidding_starredClauseMissing_eliminationRisk() {
            sampleRequest.setTenderRequirements("★这是关键条款\n普通条款");

            when(documentRepository.findById("DOC-001")).thenReturn(Optional.of(sampleDocument));
            when(clauseRepository.findByDocumentId("DOC-001")).thenReturn(List.of(
                Clause.builder()
                    .id("C2")
                    .documentId("DOC-001")
                    .clauseNumber("2")
                    .content("普通条款")
                    .isStarred(false)
                    .build()
            ));

            BiddingCheckResponse response = biddingCheckService.checkBidding(sampleRequest);

            assertThat(response.isEliminationRisk()).isTrue();
            assertThat(response.getRiskReasons()).isNotEmpty();
        }

        @Test
        @DisplayName("CRITICAL severity clause sets elimination risk")
        void checkBidding_criticalSeverity_eliminationRisk() {
            sampleRequest.setTenderRequirements("★关键要求1\n★关键要求2");

            when(documentRepository.findById("DOC-001")).thenReturn(Optional.of(sampleDocument));
            when(clauseRepository.findByDocumentId("DOC-001")).thenReturn(Collections.emptyList());

            BiddingCheckResponse response = biddingCheckService.checkBidding(sampleRequest);

            assertThat(response.isEliminationRisk()).isTrue();
            assertThat(response.getIssues()).allMatch(issue ->
                "CRITICAL".equals(issue.getSeverity()) || issue.isEliminationRisk()
            );
        }
    }

    @Nested
    @DisplayName("checkBidding - Clause Number Matching")
    class CheckBiddingClauseMatching {

        @Test
        @DisplayName("Clauses are matched by clause number, not by content")
        void checkBidding_matchByClauseNumber() {
            sampleRequest.setTenderRequirements("1. 第一条要求\n2. 第二条要求");

            // Bidding document has same clause numbers but different content
            when(documentRepository.findById("DOC-001")).thenReturn(Optional.of(sampleDocument));
            when(clauseRepository.findByDocumentId("DOC-001")).thenReturn(List.of(
                Clause.builder()
                    .id("C1")
                    .documentId("DOC-001")
                    .clauseNumber("1")
                    .content("1. 第一条要求（实际内容）")  // Same number, different content
                    .isStarred(false)
                    .build(),
                Clause.builder()
                    .id("C2")
                    .documentId("DOC-001")
                    .clauseNumber("2")
                    .content("2. 第二条要求（实际内容）")
                    .isStarred(false)
                    .build()
            ));

            BiddingCheckResponse response = biddingCheckService.checkBidding(sampleRequest);

            // Matched because clause numbers match
            assertThat(response.getMatchedClauses()).isEqualTo(2);
            assertThat(response.getScore()).isEqualTo(100.0);
        }
    }

    @Nested
    @DisplayName("checkBidding - Response Fields")
    class CheckBiddingResponseFields {

        @Test
        @DisplayName("Response contains correct document ID")
        void checkBidding_responseHasDocumentId() {
            sampleRequest.setTenderRequirements("第一条要求");

            when(documentRepository.findById("DOC-001")).thenReturn(Optional.of(sampleDocument));
            when(clauseRepository.findByDocumentId("DOC-001")).thenReturn(Collections.emptyList());

            BiddingCheckResponse response = biddingCheckService.checkBidding(sampleRequest);

            assertThat(response.getDocumentId()).isEqualTo("DOC-001");
            assertThat(response.getCheckId()).startsWith("CHECK-");
        }

        @Test
        @DisplayName("Response contains timestamp")
        void checkBidding_responseHasTimestamp() {
            sampleRequest.setTenderRequirements("第一条要求");

            when(documentRepository.findById("DOC-001")).thenReturn(Optional.of(sampleDocument));
            when(clauseRepository.findByDocumentId("DOC-001")).thenReturn(Collections.emptyList());

            BiddingCheckResponse response = biddingCheckService.checkBidding(sampleRequest);

            assertThat(response.getCheckedAt()).isNotNull();
            assertThat(response.getCheckedAt()).isNotEmpty();
        }

        @Test
        @DisplayName("Issue DTOs have correct structure")
        void checkBidding_issueDtoStructure() {
            sampleRequest.setTenderRequirements("★关键条款");

            when(documentRepository.findById("DOC-001")).thenReturn(Optional.of(sampleDocument));
            when(clauseRepository.findByDocumentId("DOC-001")).thenReturn(Collections.emptyList());

            BiddingCheckResponse response = biddingCheckService.checkBidding(sampleRequest);

            assertThat(response.getIssues()).hasSize(1);
            BiddingCheckResponse.IssueDto issue = response.getIssues().get(0);
            assertThat(issue.getIssueId()).startsWith("ISSUE-");
            assertThat(issue.getClauseNumber()).isEqualTo("1");
            assertThat(issue.getIssueType()).isEqualTo("MISSING");
            assertThat(issue.getOriginalText()).isEqualTo("(未提供)");
            assertThat(issue.getRequirementText()).isEqualTo("★关键条款");
            assertThat(issue.getSuggestionText()).startsWith("请补充:");
            assertThat(issue.getSeverity()).isEqualTo("CRITICAL");
            assertThat(issue.isEliminationRisk()).isTrue();
        }
    }
}
