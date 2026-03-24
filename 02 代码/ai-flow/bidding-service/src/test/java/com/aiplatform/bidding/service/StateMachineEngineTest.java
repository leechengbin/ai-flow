package com.aiplatform.bidding.service;

import com.aiplatform.bidding.domain.entity.ReviewProcess;
import com.aiplatform.bidding.domain.entity.StateTransition;
import com.aiplatform.bidding.domain.enums.ReviewState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

@DisplayName("StateMachineEngine Tests")
class StateMachineEngineTest {

    private StateMachineEngine stateMachineEngine;

    @BeforeEach
    void setUp() {
        stateMachineEngine = new StateMachineEngine();
    }

    @Nested
    @DisplayName("Valid State Transitions")
    class ValidTransitions {

        @Test
        @DisplayName("DRAFT -> AI_REVIEWING is valid")
        void draftToAiReviewing_isValid() {
            assertThat(stateMachineEngine.canTransition(ReviewState.DRAFT, ReviewState.AI_REVIEWING))
                .isTrue();
        }

        @Test
        @DisplayName("AI_REVIEWING -> HUMAN_REVIEWING is valid")
        void aiReviewingToHumanReviewing_isValid() {
            assertThat(stateMachineEngine.canTransition(ReviewState.AI_REVIEWING, ReviewState.HUMAN_REVIEWING))
                .isTrue();
        }

        @Test
        @DisplayName("HUMAN_REVIEWING -> FINAL_APPROVED is valid")
        void humanReviewingToFinalApproved_isValid() {
            assertThat(stateMachineEngine.canTransition(ReviewState.HUMAN_REVIEWING, ReviewState.FINAL_APPROVED))
                .isTrue();
        }

        @Test
        @DisplayName("HUMAN_REVIEWING -> REJECTED is valid")
        void humanReviewingToRejected_isValid() {
            assertThat(stateMachineEngine.canTransition(ReviewState.HUMAN_REVIEWING, ReviewState.REJECTED))
                .isTrue();
        }

        @Test
        @DisplayName("HUMAN_REVIEWING -> REVISION_REQUESTED is valid")
        void humanReviewingToRevisionRequested_isValid() {
            assertThat(stateMachineEngine.canTransition(ReviewState.HUMAN_REVIEWING, ReviewState.REVISION_REQUESTED))
                .isTrue();
        }

        @Test
        @DisplayName("REVISION_REQUESTED -> AI_REVIEWING is valid")
        void revisionRequestedToAiReviewing_isValid() {
            assertThat(stateMachineEngine.canTransition(ReviewState.REVISION_REQUESTED, ReviewState.AI_REVIEWING))
                .isTrue();
        }
    }

    @Nested
    @DisplayName("Invalid State Transitions")
    class InvalidTransitions {

        @ParameterizedTest
        @MethodSource("provideInvalidTransitions")
        @DisplayName("Invalid transitions should be rejected")
        void invalidTransitions_areRejected(ReviewState from, ReviewState to) {
            assertThat(stateMachineEngine.canTransition(from, to))
                .isFalse();
        }

        static Stream<Arguments> provideInvalidTransitions() {
            return Stream.of(
                // Cannot go backwards
                Arguments.of(ReviewState.AI_REVIEWING, ReviewState.DRAFT),
                Arguments.of(ReviewState.HUMAN_REVIEWING, ReviewState.AI_REVIEWING),
                Arguments.of(ReviewState.REVISION_REQUESTED, ReviewState.DRAFT),
                Arguments.of(ReviewState.REVISION_REQUESTED, ReviewState.HUMAN_REVIEWING),
                // Cannot skip states
                Arguments.of(ReviewState.DRAFT, ReviewState.HUMAN_REVIEWING),
                Arguments.of(ReviewState.DRAFT, ReviewState.FINAL_APPROVED),
                Arguments.of(ReviewState.DRAFT, ReviewState.REJECTED),
                Arguments.of(ReviewState.AI_REVIEWING, ReviewState.FINAL_APPROVED),
                Arguments.of(ReviewState.AI_REVIEWING, ReviewState.REJECTED),
                // Terminal states have no outgoing transitions
                Arguments.of(ReviewState.FINAL_APPROVED, ReviewState.DRAFT),
                Arguments.of(ReviewState.FINAL_APPROVED, ReviewState.AI_REVIEWING),
                Arguments.of(ReviewState.FINAL_APPROVED, ReviewState.HUMAN_REVIEWING),
                Arguments.of(ReviewState.REJECTED, ReviewState.DRAFT),
                Arguments.of(ReviewState.REJECTED, ReviewState.AI_REVIEWING),
                Arguments.of(ReviewState.REJECTED, ReviewState.HUMAN_REVIEWING)
            );
        }
    }

    @Nested
    @DisplayName("Get Valid Next States")
    class GetValidNextStates {

        @Test
        @DisplayName("DRAFT has only AI_REVIEWING as valid next state")
        void draft_validNextStates() {
            Set<ReviewState> validNext = stateMachineEngine.getValidNextStates(ReviewState.DRAFT);
            assertThat(validNext)
                .containsExactly(ReviewState.AI_REVIEWING);
        }

        @Test
        @DisplayName("AI_REVIEWING has only HUMAN_REVIEWING as valid next state")
        void aiReviewing_validNextStates() {
            Set<ReviewState> validNext = stateMachineEngine.getValidNextStates(ReviewState.AI_REVIEWING);
            assertThat(validNext)
                .containsExactly(ReviewState.HUMAN_REVIEWING);
        }

        @Test
        @DisplayName("HUMAN_REVIEWING has three valid next states")
        void humanReviewing_validNextStates() {
            Set<ReviewState> validNext = stateMachineEngine.getValidNextStates(ReviewState.HUMAN_REVIEWING);
            assertThat(validNext)
                .containsExactlyInAnyOrder(
                    ReviewState.FINAL_APPROVED,
                    ReviewState.REVISION_REQUESTED,
                    ReviewState.REJECTED
                );
        }

        @Test
        @DisplayName("REVISION_REQUESTED has only AI_REVIEWING as valid next state")
        void revisionRequested_validNextStates() {
            Set<ReviewState> validNext = stateMachineEngine.getValidNextStates(ReviewState.REVISION_REQUESTED);
            assertThat(validNext)
                .containsExactly(ReviewState.AI_REVIEWING);
        }

        @Test
        @DisplayName("FINAL_APPROVED has no valid next states (terminal)")
        void finalApproved_noNextStates() {
            Set<ReviewState> validNext = stateMachineEngine.getValidNextStates(ReviewState.FINAL_APPROVED);
            assertThat(validNext).isEmpty();
        }

        @Test
        @DisplayName("REJECTED has no valid next states (terminal)")
        void rejected_noNextStates() {
            Set<ReviewState> validNext = stateMachineEngine.getValidNextStates(ReviewState.REJECTED);
            assertThat(validNext).isEmpty();
        }
    }

    @Nested
    @DisplayName("Execute Transition")
    class ExecuteTransition {

        @Test
        @DisplayName("executeTransition creates StateTransition and updates process state")
        void executeTransition_validTransition_succeeds() {
            ReviewProcess process = ReviewProcess.builder()
                .id("REV-123")
                .documentId("DOC-456")
                .currentState(ReviewState.DRAFT)
                .submitterId("user-1")
                .reviewerId("user-1")
                .build();

            StateTransition transition = stateMachineEngine.executeTransition(
                process, ReviewState.AI_REVIEWING, "system", "提交审核"
            );

            assertThat(transition.getReviewId()).isEqualTo("REV-123");
            assertThat(transition.getFromState()).isEqualTo(ReviewState.DRAFT);
            assertThat(transition.getToState()).isEqualTo(ReviewState.AI_REVIEWING);
            assertThat(transition.getActor()).isEqualTo("system");
            assertThat(transition.getAction()).isEqualTo("提交审核");
            assertThat(transition.getTimestamp()).isNotNull();

            assertThat(process.getCurrentState()).isEqualTo(ReviewState.AI_REVIEWING);
        }

        @Test
        @DisplayName("executeTransition throws IllegalStateException for invalid transition")
        void executeTransition_invalidTransition_throws() {
            ReviewProcess process = ReviewProcess.builder()
                .id("REV-123")
                .documentId("DOC-456")
                .currentState(ReviewState.DRAFT)
                .submitterId("user-1")
                .reviewerId("user-1")
                .build();

            assertThatThrownBy(() ->
                stateMachineEngine.executeTransition(process, ReviewState.FINAL_APPROVED, "user", "invalid"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid transition from DRAFT to FINAL_APPROVED");
        }
    }

    @Nested
    @DisplayName("Get Event For Transition")
    class GetEventForTransition {

        @ParameterizedTest
        @MethodSource("provideTransitionEvents")
        @DisplayName("getEventForTransition returns correct event name")
        void getEventForTransition_validTransitions(ReviewState from, ReviewState to, String expectedEvent) {
            assertThat(stateMachineEngine.getEventForTransition(from, to))
                .isEqualTo(expectedEvent);
        }

        static Stream<Arguments> provideTransitionEvents() {
            return Stream.of(
                Arguments.of(ReviewState.DRAFT, ReviewState.AI_REVIEWING, "SUBMIT"),
                Arguments.of(ReviewState.AI_REVIEWING, ReviewState.HUMAN_REVIEWING, "AI_COMPLETE"),
                Arguments.of(ReviewState.HUMAN_REVIEWING, ReviewState.FINAL_APPROVED, "APPROVE"),
                Arguments.of(ReviewState.HUMAN_REVIEWING, ReviewState.REVISION_REQUESTED, "REQUEST_REVISION"),
                Arguments.of(ReviewState.HUMAN_REVIEWING, ReviewState.REJECTED, "REJECT"),
                Arguments.of(ReviewState.REVISION_REQUESTED, ReviewState.AI_REVIEWING, "RESUBMIT")
            );
        }

        @Test
        @DisplayName("getEventForTransition returns UNKNOWN for invalid transition")
        void getEventForTransition_invalidTransition_returnsUnknown() {
            assertThat(stateMachineEngine.getEventForTransition(ReviewState.DRAFT, ReviewState.REJECTED))
                .isEqualTo("UNKNOWN");
        }
    }
}
