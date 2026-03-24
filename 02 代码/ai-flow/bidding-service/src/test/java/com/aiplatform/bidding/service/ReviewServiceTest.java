package com.aiplatform.bidding.service;

import com.aiplatform.bidding.domain.entity.ReviewProcess;
import com.aiplatform.bidding.domain.entity.StateTransition;
import com.aiplatform.bidding.domain.enums.ReviewState;
import com.aiplatform.bidding.dto.request.HumanActionRequest;
import com.aiplatform.bidding.dto.request.ReviewSubmitRequest;
import com.aiplatform.bidding.dto.response.ReviewStatusResponse;
import com.aiplatform.bidding.repository.ReviewProcessRepository;
import com.aiplatform.bidding.repository.StateTransitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService Tests")
class ReviewServiceTest {

    @Mock
    private ReviewProcessRepository reviewProcessRepository;

    @Mock
    private StateTransitionRepository transitionRepository;

    @Mock
    private StateMachineEngine stateMachineEngine;

    @InjectMocks
    private ReviewService reviewService;

    private ReviewSubmitRequest submitRequest;
    private HumanActionRequest approveRequest;
    private HumanActionRequest rejectRequest;
    private HumanActionRequest revisionRequest;

    @BeforeEach
    void setUp() {
        submitRequest = new ReviewSubmitRequest();
        submitRequest.setReviewerId("reviewer-1");
        submitRequest.setDeadline("2024-12-31");

        approveRequest = new HumanActionRequest();
        approveRequest.setAction("APPROVE");
        approveRequest.setComment("Looks good");

        rejectRequest = new HumanActionRequest();
        rejectRequest.setAction("REJECT");
        rejectRequest.setComment("Needs major changes");

        revisionRequest = new HumanActionRequest();
        revisionRequest.setAction("REQUEST_REVISION");
        revisionRequest.setComment("Please revise section 3");
    }

    @Nested
    @DisplayName("submitReview")
    class SubmitReview {

        @Test
        @DisplayName("submitReview creates review process and transitions to AI_REVIEWING")
        void submitReview_success() {
            String documentId = "DOC-001";
            ReviewProcess savedProcess = ReviewProcess.builder()
                .id("REV-123")
                .documentId(documentId)
                .currentState(ReviewState.AI_REVIEWING)
                .submitterId("reviewer-1")
                .reviewerId("reviewer-1")
                .build();

            StateTransition savedTransition = StateTransition.builder()
                .id("TRANS-001")
                .reviewId("REV-123")
                .fromState(ReviewState.DRAFT)
                .toState(ReviewState.AI_REVIEWING)
                .actor("system")
                .action("提交审核")
                .timestamp(LocalDateTime.now())
                .build();

            when(stateMachineEngine.executeTransition(any(), eq(ReviewState.AI_REVIEWING), any(), any()))
                .thenReturn(savedTransition);
            when(reviewProcessRepository.save(any())).thenReturn(savedProcess);
            when(transitionRepository.save(any())).thenReturn(savedTransition);
            when(transitionRepository.findByReviewIdOrderByTimestampAsc("REV-123"))
                .thenReturn(List.of(savedTransition));

            ReviewStatusResponse response = reviewService.submitReview(documentId, submitRequest);

            assertThat(response.getReviewId()).isEqualTo("REV-123");
            assertThat(response.getDocumentId()).isEqualTo(documentId);
            assertThat(response.getCurrentState()).isEqualTo("AI_REVIEWING");
            assertThat(response.getSubmitterId()).isEqualTo("reviewer-1");
            assertThat(response.getReviewerId()).isEqualTo("reviewer-1");

            verify(reviewProcessRepository).save(any(ReviewProcess.class));
            verify(transitionRepository).save(any(StateTransition.class));
        }

        @Test
        @DisplayName("submitReview creates process with DRAFT state initially")
        void submitReview_processHasDraftStateInitially() {
            String documentId = "DOC-002";
            ArgumentCaptor<ReviewProcess> processCaptor = ArgumentCaptor.forClass(ReviewProcess.class);

            ReviewProcess savedProcess = ReviewProcess.builder()
                .id("REV-456")
                .documentId(documentId)
                .currentState(ReviewState.AI_REVIEWING)
                .submitterId("reviewer-1")
                .reviewerId("reviewer-1")
                .build();

            StateTransition savedTransition = StateTransition.builder()
                .id("TRANS-002")
                .reviewId("REV-456")
                .fromState(ReviewState.DRAFT)
                .toState(ReviewState.AI_REVIEWING)
                .actor("system")
                .action("提交审核")
                .timestamp(LocalDateTime.now())
                .build();

            when(stateMachineEngine.executeTransition(any(), eq(ReviewState.AI_REVIEWING), any(), any()))
                .thenReturn(savedTransition);
            when(reviewProcessRepository.save(any())).thenReturn(savedProcess);
            when(transitionRepository.save(any())).thenReturn(savedTransition);
            when(transitionRepository.findByReviewIdOrderByTimestampAsc("REV-456"))
                .thenReturn(List.of(savedTransition));

            reviewService.submitReview(documentId, submitRequest);

            verify(reviewProcessRepository).save(processCaptor.capture());
            ReviewProcess capturedProcess = processCaptor.getValue();
            assertThat(capturedProcess.getDocumentId()).isEqualTo(documentId);
            assertThat(capturedProcess.getCurrentState()).isEqualTo(ReviewState.DRAFT);
            assertThat(capturedProcess.getSubmitterId()).isEqualTo("reviewer-1");
        }
    }

    @Nested
    @DisplayName("executeHumanAction - APPROVE")
    class ExecuteHumanActionApprove {

        @Test
        @DisplayName("APPROVE action transitions to FINAL_APPROVED")
        void approveAction_transitionsToFinalApproved() {
            String reviewId = "REV-123";
            ReviewProcess existingProcess = ReviewProcess.builder()
                .id(reviewId)
                .documentId("DOC-001")
                .currentState(ReviewState.HUMAN_REVIEWING)
                .submitterId("user-1")
                .reviewerId("reviewer-1")
                .build();

            StateTransition transition = StateTransition.builder()
                .id("TRANS-001")
                .reviewId(reviewId)
                .fromState(ReviewState.HUMAN_REVIEWING)
                .toState(ReviewState.FINAL_APPROVED)
                .actor("APPROVE")
                .comment("Looks good")
                .timestamp(LocalDateTime.now())
                .build();

            when(reviewProcessRepository.findById(reviewId)).thenReturn(Optional.of(existingProcess));
            when(stateMachineEngine.executeTransition(any(), eq(ReviewState.FINAL_APPROVED), any(), any()))
                .thenReturn(transition);
            when(reviewProcessRepository.save(any())).thenReturn(existingProcess);
            when(transitionRepository.save(any())).thenReturn(transition);
            when(transitionRepository.findByReviewIdOrderByTimestampAsc(reviewId))
                .thenReturn(List.of(transition));

            ReviewStatusResponse response = reviewService.executeHumanAction(reviewId, approveRequest);

            assertThat(response.getCurrentState()).isEqualTo("FINAL_APPROVED");
            verify(stateMachineEngine).executeTransition(any(), eq(ReviewState.FINAL_APPROVED), eq("APPROVE"), eq("Looks good"));
        }
    }

    @Nested
    @DisplayName("executeHumanAction - REJECT")
    class ExecuteHumanActionReject {

        @Test
        @DisplayName("REJECT action transitions to REJECTED")
        void rejectAction_transitionsToRejected() {
            String reviewId = "REV-123";
            ReviewProcess existingProcess = ReviewProcess.builder()
                .id(reviewId)
                .documentId("DOC-001")
                .currentState(ReviewState.HUMAN_REVIEWING)
                .submitterId("user-1")
                .reviewerId("reviewer-1")
                .build();

            StateTransition transition = StateTransition.builder()
                .id("TRANS-001")
                .reviewId(reviewId)
                .fromState(ReviewState.HUMAN_REVIEWING)
                .toState(ReviewState.REJECTED)
                .actor("REJECT")
                .comment("Needs major changes")
                .timestamp(LocalDateTime.now())
                .build();

            when(reviewProcessRepository.findById(reviewId)).thenReturn(Optional.of(existingProcess));
            when(stateMachineEngine.executeTransition(any(), eq(ReviewState.REJECTED), any(), any()))
                .thenReturn(transition);
            when(reviewProcessRepository.save(any())).thenReturn(existingProcess);
            when(transitionRepository.save(any())).thenReturn(transition);
            when(transitionRepository.findByReviewIdOrderByTimestampAsc(reviewId))
                .thenReturn(List.of(transition));

            ReviewStatusResponse response = reviewService.executeHumanAction(reviewId, rejectRequest);

            assertThat(response.getCurrentState()).isEqualTo("REJECTED");
            verify(stateMachineEngine).executeTransition(any(), eq(ReviewState.REJECTED), eq("REJECT"), eq("Needs major changes"));
        }
    }

    @Nested
    @DisplayName("executeHumanAction - REQUEST_REVISION")
    class ExecuteHumanActionRevision {

        @Test
        @DisplayName("REQUEST_REVISION action transitions to REVISION_REQUESTED")
        void revisionAction_transitionsToRevisionRequested() {
            String reviewId = "REV-123";
            ReviewProcess existingProcess = ReviewProcess.builder()
                .id(reviewId)
                .documentId("DOC-001")
                .currentState(ReviewState.HUMAN_REVIEWING)
                .submitterId("user-1")
                .reviewerId("reviewer-1")
                .build();

            StateTransition transition = StateTransition.builder()
                .id("TRANS-001")
                .reviewId(reviewId)
                .fromState(ReviewState.HUMAN_REVIEWING)
                .toState(ReviewState.REVISION_REQUESTED)
                .actor("REQUEST_REVISION")
                .comment("Please revise section 3")
                .timestamp(LocalDateTime.now())
                .build();

            when(reviewProcessRepository.findById(reviewId)).thenReturn(Optional.of(existingProcess));
            when(stateMachineEngine.executeTransition(any(), eq(ReviewState.REVISION_REQUESTED), any(), any()))
                .thenReturn(transition);
            when(reviewProcessRepository.save(any())).thenReturn(existingProcess);
            when(transitionRepository.save(any())).thenReturn(transition);
            when(transitionRepository.findByReviewIdOrderByTimestampAsc(reviewId))
                .thenReturn(List.of(transition));

            ReviewStatusResponse response = reviewService.executeHumanAction(reviewId, revisionRequest);

            assertThat(response.getCurrentState()).isEqualTo("REVISION_REQUESTED");
            verify(stateMachineEngine).executeTransition(any(), eq(ReviewState.REVISION_REQUESTED), eq("REQUEST_REVISION"), eq("Please revise section 3"));
        }
    }

    @Nested
    @DisplayName("executeHumanAction - Error Cases")
    class ExecuteHumanActionErrors {

        @Test
        @DisplayName("executeHumanAction throws when review not found")
        void executeHumanAction_reviewNotFound_throws() {
            String reviewId = "NONEXISTENT";
            when(reviewProcessRepository.findById(reviewId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.executeHumanAction(reviewId, approveRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Review not found: " + reviewId);
        }

        @Test
        @DisplayName("executeHumanAction throws on unknown action")
        void executeHumanAction_unknownAction_throws() {
            String reviewId = "REV-123";
            ReviewProcess existingProcess = ReviewProcess.builder()
                .id(reviewId)
                .documentId("DOC-001")
                .currentState(ReviewState.HUMAN_REVIEWING)
                .submitterId("user-1")
                .reviewerId("reviewer-1")
                .build();

            HumanActionRequest unknownAction = new HumanActionRequest();
            unknownAction.setAction("UNKNOWN_ACTION");

            when(reviewProcessRepository.findById(reviewId)).thenReturn(Optional.of(existingProcess));

            assertThatThrownBy(() -> reviewService.executeHumanAction(reviewId, unknownAction))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown action: UNKNOWN_ACTION");
        }
    }

    @Nested
    @DisplayName("getReviewStatus")
    class GetReviewStatus {

        @Test
        @DisplayName("getReviewStatus returns review status")
        void getReviewStatus_success() {
            String reviewId = "REV-123";
            ReviewProcess process = ReviewProcess.builder()
                .id(reviewId)
                .documentId("DOC-001")
                .currentState(ReviewState.HUMAN_REVIEWING)
                .submitterId("user-1")
                .reviewerId("reviewer-1")
                .build();

            StateTransition transition = StateTransition.builder()
                .id("TRANS-001")
                .reviewId(reviewId)
                .fromState(ReviewState.DRAFT)
                .toState(ReviewState.AI_REVIEWING)
                .actor("system")
                .action("提交审核")
                .timestamp(LocalDateTime.now())
                .build();

            when(reviewProcessRepository.findById(reviewId)).thenReturn(Optional.of(process));
            when(transitionRepository.findByReviewIdOrderByTimestampAsc(reviewId))
                .thenReturn(List.of(transition));

            ReviewStatusResponse response = reviewService.getReviewStatus(reviewId);

            assertThat(response.getReviewId()).isEqualTo(reviewId);
            assertThat(response.getDocumentId()).isEqualTo("DOC-001");
            assertThat(response.getCurrentState()).isEqualTo("HUMAN_REVIEWING");
            assertThat(response.getSubmitterId()).isEqualTo("user-1");
            assertThat(response.getReviewerId()).isEqualTo("reviewer-1");
            assertThat(response.getHistory()).hasSize(1);
        }

        @Test
        @DisplayName("getReviewStatus throws when review not found")
        void getReviewStatus_notFound_throws() {
            String reviewId = "NONEXISTENT";
            when(reviewProcessRepository.findById(reviewId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.getReviewStatus(reviewId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Review not found: " + reviewId);
        }

        @Test
        @DisplayName("getReviewStatus returns empty lists for pending/approved/rejected issues")
        void getReviewStatus_emptyIssueLists() {
            String reviewId = "REV-123";
            ReviewProcess process = ReviewProcess.builder()
                .id(reviewId)
                .documentId("DOC-001")
                .currentState(ReviewState.AI_REVIEWING)
                .submitterId("user-1")
                .reviewerId("reviewer-1")
                .build();

            when(reviewProcessRepository.findById(reviewId)).thenReturn(Optional.of(process));
            when(transitionRepository.findByReviewIdOrderByTimestampAsc(reviewId))
                .thenReturn(Collections.emptyList());

            ReviewStatusResponse response = reviewService.getReviewStatus(reviewId);

            assertThat(response.getPendingIssues()).isEmpty();
            assertThat(response.getApprovedIssues()).isEmpty();
            assertThat(response.getRejectedIssues()).isEmpty();
        }
    }

    @Nested
    @DisplayName("toResponse - History DTO")
    class ToResponseHistory {

        @Test
        @DisplayName("History contains correct transition data")
        void toResponse_historyContainsTransitionData() {
            String reviewId = "REV-123";
            ReviewProcess process = ReviewProcess.builder()
                .id(reviewId)
                .documentId("DOC-001")
                .currentState(ReviewState.AI_REVIEWING)
                .submitterId("user-1")
                .reviewerId("reviewer-1")
                .build();

            LocalDateTime timestamp = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
            StateTransition transition = StateTransition.builder()
                .id("TRANS-001")
                .reviewId(reviewId)
                .fromState(ReviewState.DRAFT)
                .toState(ReviewState.AI_REVIEWING)
                .actor("system")
                .action("提交审核")
                .timestamp(timestamp)
                .build();

            when(reviewProcessRepository.findById(reviewId)).thenReturn(Optional.of(process));
            when(transitionRepository.findByReviewIdOrderByTimestampAsc(reviewId))
                .thenReturn(List.of(transition));

            ReviewStatusResponse response = reviewService.getReviewStatus(reviewId);

            assertThat(response.getHistory()).hasSize(1);
            ReviewStatusResponse.HistoryDto historyDto = response.getHistory().get(0);
            assertThat(historyDto.getFromState()).isEqualTo("DRAFT");
            assertThat(historyDto.getToState()).isEqualTo("AI_REVIEWING");
            assertThat(historyDto.getActor()).isEqualTo("system");
            assertThat(historyDto.getAction()).isEqualTo("提交审核");
            assertThat(historyDto.getTimestamp()).isEqualTo(timestamp.toString());
        }

        @Test
        @DisplayName("History handles null fromState correctly")
        void toResponse_nullFromState_handled() {
            String reviewId = "REV-123";
            ReviewProcess process = ReviewProcess.builder()
                .id(reviewId)
                .documentId("DOC-001")
                .currentState(ReviewState.AI_REVIEWING)
                .submitterId("user-1")
                .reviewerId("reviewer-1")
                .build();

            StateTransition transition = StateTransition.builder()
                .id("TRANS-001")
                .reviewId(reviewId)
                .fromState(null)  // null fromState
                .toState(ReviewState.AI_REVIEWING)
                .actor("system")
                .action("提交审核")
                .timestamp(LocalDateTime.now())
                .build();

            when(reviewProcessRepository.findById(reviewId)).thenReturn(Optional.of(process));
            when(transitionRepository.findByReviewIdOrderByTimestampAsc(reviewId))
                .thenReturn(List.of(transition));

            ReviewStatusResponse response = reviewService.getReviewStatus(reviewId);

            assertThat(response.getHistory()).hasSize(1);
            assertThat(response.getHistory().get(0).getFromState()).isNull();
        }
    }
}
