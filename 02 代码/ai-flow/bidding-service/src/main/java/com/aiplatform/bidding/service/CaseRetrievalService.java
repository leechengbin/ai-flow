package com.aiplatform.bidding.service;

import com.aiplatform.bidding.domain.entity.BiddingCase;
import com.aiplatform.bidding.dto.request.CaseRetrieveRequest;
import com.aiplatform.bidding.dto.response.CaseRetrieveResponse;
import com.aiplatform.bidding.repository.BiddingCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaseRetrievalService {
    private final BiddingCaseRepository caseRepository;

    @Transactional(readOnly = true)
    public CaseRetrieveResponse retrieveSimilarCases(CaseRetrieveRequest request) {
        var cases = caseRepository.findAll();
        if (request.getIndustry() != null && !request.getIndustry().isBlank()) {
            cases = cases.stream().filter(c -> request.getIndustry().equals(c.getIndustry())).collect(Collectors.toList());
        }
        int topK = request.getTopK() != null ? request.getTopK() : 5;
        var topCases = cases.stream().limit(topK).toList();
        var dtos = topCases.stream().map(c -> CaseRetrieveResponse.CaseDto.builder()
            .caseId(c.getId()).tenderTitle(c.getTenderTitle()).industry(c.getIndustry())
            .region(c.getRegion()).winningBidder(c.getWinningBidder())
            .bidAmount(c.getBidAmount()).winningDate(c.getWinningDate() != null ? c.getWinningDate().toString() : null)
            .similarityScore(0.85).build()).toList();
        return CaseRetrieveResponse.builder().cases(dtos).total(dtos.size()).build();
    }
}