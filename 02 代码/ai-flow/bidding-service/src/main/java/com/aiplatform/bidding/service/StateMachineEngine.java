package com.aiplatform.bidding.service;

import com.aiplatform.bidding.domain.entity.StateTransition;
import com.aiplatform.bidding.domain.entity.ReviewProcess;
import com.aiplatform.bidding.domain.enums.ReviewState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.*;

@Component
@Slf4j
public class StateMachineEngine {
    private static final Map<ReviewState, Set<ReviewState>> TRANSITION_RULES;

    static {
        TRANSITION_RULES = new EnumMap<>(ReviewState.class);
        TRANSITION_RULES.put(ReviewState.DRAFT, EnumSet.of(ReviewState.AI_REVIEWING));
        TRANSITION_RULES.put(ReviewState.AI_REVIEWING, EnumSet.of(ReviewState.HUMAN_REVIEWING));
        TRANSITION_RULES.put(ReviewState.HUMAN_REVIEWING, EnumSet.of(ReviewState.FINAL_APPROVED, ReviewState.REVISION_REQUESTED, ReviewState.REJECTED));
        TRANSITION_RULES.put(ReviewState.REVISION_REQUESTED, EnumSet.of(ReviewState.AI_REVIEWING));
        TRANSITION_RULES.put(ReviewState.FINAL_APPROVED, EnumSet.noneOf(ReviewState.class));
        TRANSITION_RULES.put(ReviewState.REJECTED, EnumSet.noneOf(ReviewState.class));
    }

    public boolean canTransition(ReviewState from, ReviewState to) {
        Set<ReviewState> allowedStates = TRANSITION_RULES.get(from);
        return allowedStates != null && allowedStates.contains(to);
    }

    public Set<ReviewState> getValidNextStates(ReviewState current) {
        return TRANSITION_RULES.getOrDefault(current, EnumSet.noneOf(ReviewState.class));
    }

    public StateTransition executeTransition(ReviewProcess process, ReviewState toState, String actor, String action) {
        ReviewState fromState = process.getCurrentState();
        if (!canTransition(fromState, toState)) {
            throw new IllegalStateException(String.format("Invalid transition from %s to %s", fromState, toState));
        }
        StateTransition transition = StateTransition.builder()
            .id("TRANS-" + UUID.randomUUID())
            .reviewId(process.getId())
            .fromState(fromState)
            .toState(toState)
            .actor(actor)
            .action(action)
            .timestamp(LocalDateTime.now())
            .build();
        process.setCurrentState(toState);
        log.info("State transition: {} -> {} for review {}", fromState, toState, process.getId());
        return transition;
    }

    public String getEventForTransition(ReviewState from, ReviewState to) {
        if (from == ReviewState.DRAFT && to == ReviewState.AI_REVIEWING) return "SUBMIT";
        if (from == ReviewState.AI_REVIEWING && to == ReviewState.HUMAN_REVIEWING) return "AI_COMPLETE";
        if (from == ReviewState.HUMAN_REVIEWING && to == ReviewState.FINAL_APPROVED) return "APPROVE";
        if (from == ReviewState.HUMAN_REVIEWING && to == ReviewState.REVISION_REQUESTED) return "REQUEST_REVISION";
        if (from == ReviewState.HUMAN_REVIEWING && to == ReviewState.REJECTED) return "REJECT";
        if (from == ReviewState.REVISION_REQUESTED && to == ReviewState.AI_REVIEWING) return "RESUBMIT";
        return "UNKNOWN";
    }
}