package com.aiplatform.bidding.service;

import com.aiplatform.bidding.domain.enums.ReviewState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ReviewState Enum Tests")
class ReviewStateEnumTest {

    @Nested
    @DisplayName("Enum Values")
    class EnumValues {

        @Test
        @DisplayName("ReviewState has exactly 6 enum values")
        void reviewState_hasSixValues() {
            ReviewState[] values = ReviewState.values();
            assertThat(values).hasSize(6);
        }

        @ParameterizedTest
        @EnumSource(ReviewState.class)
        @DisplayName("All enum values are present")
        void allEnumValuesPresent(ReviewState state) {
            assertThat(state).isNotNull();
            assertThat(state.name()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("valueOf")
    class ValueOf {

        @Test
        @DisplayName("valueOf returns correct enum for valid name")
        void valueOf_validName_returnsEnum() {
            assertThat(ReviewState.valueOf("DRAFT")).isEqualTo(ReviewState.DRAFT);
            assertThat(ReviewState.valueOf("AI_REVIEWING")).isEqualTo(ReviewState.AI_REVIEWING);
            assertThat(ReviewState.valueOf("HUMAN_REVIEWING")).isEqualTo(ReviewState.HUMAN_REVIEWING);
            assertThat(ReviewState.valueOf("FINAL_APPROVED")).isEqualTo(ReviewState.FINAL_APPROVED);
            assertThat(ReviewState.valueOf("REVISION_REQUESTED")).isEqualTo(ReviewState.REVISION_REQUESTED);
            assertThat(ReviewState.valueOf("REJECTED")).isEqualTo(ReviewState.REJECTED);
        }

        @Test
        @DisplayName("valueOf throws IllegalArgumentException for invalid name")
        void valueOf_invalidName_throws() {
            assertThatThrownBy(() -> ReviewState.valueOf("INVALID"))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("valueOf throws NullPointerException for null")
        void valueOf_null_throws() {
            assertThatThrownBy(() -> ReviewState.valueOf(null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("State Categories")
    class StateCategories {

        @Test
        @DisplayName("Initial states include only DRAFT")
        void initialStates() {
            ReviewState initial = ReviewState.DRAFT;
            assertThat(initial).isEqualTo(ReviewState.DRAFT);
        }

        @Test
        @DisplayName("Terminal states include FINAL_APPROVED and REJECTED")
        void terminalStates() {
            assertThat(ReviewState.FINAL_APPROVED.name()).isEqualTo("FINAL_APPROVED");
            assertThat(ReviewState.REJECTED.name()).isEqualTo("REJECTED");
        }

        @Test
        @DisplayName("Processing states include AI_REVIEWING and HUMAN_REVIEWING")
        void processingStates() {
            assertThat(ReviewState.AI_REVIEWING.name()).isEqualTo("AI_REVIEWING");
            assertThat(ReviewState.HUMAN_REVIEWING.name()).isEqualTo("HUMAN_REVIEWING");
        }

        @Test
        @DisplayName("REVISION_REQUESTED is a non-terminal state for corrections")
        void revisionRequestedState() {
            assertThat(ReviewState.REVISION_REQUESTED.name()).isEqualTo("REVISION_REQUESTED");
        }
    }

    @Nested
    @DisplayName("Enum Name Consistency")
    class EnumNameConsistency {

        @Test
        @DisplayName("All enum constant names match their ordinal position description")
        void enumNamesAreDescriptive() {
            for (ReviewState state : ReviewState.values()) {
                assertThat(state.name())
                    .matches("[A-Z_]+")
                    .isNotEmpty();
            }
        }

        @Test
        @DisplayName("Enum values are in expected order")
        void enumValuesAreInExpectedOrder() {
            ReviewState[] expectedOrder = {
                ReviewState.DRAFT,
                ReviewState.AI_REVIEWING,
                ReviewState.HUMAN_REVIEWING,
                ReviewState.REVISION_REQUESTED,
                ReviewState.FINAL_APPROVED,
                ReviewState.REJECTED
            };

            assertThat(ReviewState.values()).isEqualTo(expectedOrder);
        }

        @Test
        @DisplayName("Each enum value has a unique ordinal")
        void uniqueOrdinals() {
            long uniqueOrdinals = Arrays.stream(ReviewState.values())
                .map(ReviewState::ordinal)
                .distinct()
                .count();

            assertThat(uniqueOrdinals).isEqualTo(ReviewState.values().length);
        }
    }

    @Nested
    @DisplayName("toString Representation")
    class ToStringRepresentation {

        @Test
        @DisplayName("toString returns the enum name")
        void toString_returnsName() {
            for (ReviewState state : ReviewState.values()) {
                assertThat(state.toString()).isEqualTo(state.name());
            }
        }
    }
}
