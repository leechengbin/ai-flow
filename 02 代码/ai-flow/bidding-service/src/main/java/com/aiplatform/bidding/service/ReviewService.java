package com.aiplatform.bidding.service;

import com.aiplatform.bidding.domain.entity.ReviewProcess;
import com.aiplatform.bidding.domain.entity.StateTransition;
import com.aiplatform.bidding.domain.enums.ReviewState;
import com.aiplatform.bidding.dto.request.HumanActionRequest;
import com.aiplatform.bidding.dto.request.ReviewSubmitRequest;
import com.aiplatform.bidding.dto.response.ReviewStatusResponse;
import com.aiplatform.bidding.repository.ReviewProcessRepository;
import com.aiplatform.bidding.repository.StateTransitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {
    private final ReviewProcessRepository reviewProcessRepository;
    private final StateTransitionRepository transitionRepository;
    private final StateMachineEngine stateMachineEngine;

    @Transactional
    public ReviewStatusResponse submitReview(String documentId, ReviewSubmitRequest request) {
        ReviewProcess process = ReviewProcess.builder()
            .id("REV-" + UUID.randomUUID()).documentId(documentId).currentState(ReviewState.DRAFT)
            .submitterId(request.getReviewerId()).reviewerId(request.getReviewerId()).build();
        StateTransition transition = stateMachineEngine.executeTransition(process, ReviewState.AI_REVIEWING, "system", "提交审核");
        reviewProcessRepository.save(process);
        transitionRepository.save(transition);
        return toResponse(process);
    }

    @Transactional
    public ReviewStatusResponse executeHumanAction(String reviewId, HumanActionRequest request) {
        ReviewProcess process = reviewProcessRepository.findById(reviewId)
            .orElseThrow(() -> new RuntimeException("Review not found: " + reviewId));
        ReviewState targetState = switch (request.getAction()) {
            case "APPROVE" -> ReviewState.FINAL_APPROVED;
            case "REJECT" -> ReviewState.REJECTED;
            case "REQUEST_REVISION" -> ReviewState.REVISION_REQUESTED;
            default -> throw new IllegalArgumentException("Unknown action: " + request.getAction());
        };
        StateTransition transition = stateMachineEngine.executeTransition(process, targetState, request.getAction(), request.getComment());
        reviewProcessRepository.save(process);
        transitionRepository.save(transition);
        return toResponse(process);
    }

    @Transactional(readOnly = true)
    public ReviewStatusResponse getReviewStatus(String reviewId) {
        ReviewProcess process = reviewProcessRepository.findById(reviewId)
            .orElseThrow(() -> new RuntimeException("Review not found: " + reviewId));
        return toResponse(process);
    }

    private ReviewStatusResponse toResponse(ReviewProcess process) {
        List<StateTransition> transitions = transitionRepository.findByReviewIdOrderByTimestampAsc(process.getId());
        List<ReviewStatusResponse.HistoryDto> history = transitions.stream().map(t ->
            ReviewStatusResponse.HistoryDto.builder()
                .fromState(t.getFromState() != null ? t.getFromState().name() : null)
                .toState(t.getToState().name()).actor(t.getActor())
                .action(t.getAction()).timestamp(t.getTimestamp().toString()).build()
        ).toList();
        return ReviewStatusResponse.builder()
            .reviewId(process.getId()).documentId(process.getDocumentId())
            .currentState(process.getCurrentState().name())
            .submitterId(process.getSubmitterId()).reviewerId(process.getReviewerId())
            .deadline(process.getDeadline() != null ? process.getDeadline().toString() : null)
            .history(history).pendingIssues(Collections.emptyList())
            .approvedIssues(Collections.emptyList()).rejectedIssues(Collections.emptyList()).build();
    }
}