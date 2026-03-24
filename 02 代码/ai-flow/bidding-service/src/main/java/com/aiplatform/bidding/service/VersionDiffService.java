package com.aiplatform.bidding.service;

import com.aiplatform.bidding.dto.request.DiffCompareRequest;
import com.aiplatform.bidding.dto.response.DiffCompareResponse;
import com.aiplatform.bidding.repository.DocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class VersionDiffService {
    private final DocumentVersionRepository versionRepository;

    @Transactional(readOnly = true)
    public DiffCompareResponse compareVersions(DiffCompareRequest request) {
        versionRepository.findByDocumentIdAndVersionNumber(request.getDocumentId(), request.getVersionA())
            .orElseThrow(() -> new RuntimeException("Version not found: " + request.getVersionA()));
        versionRepository.findByDocumentIdAndVersionNumber(request.getDocumentId(), request.getVersionB())
            .orElseThrow(() -> new RuntimeException("Version not found: " + request.getVersionB()));
        return DiffCompareResponse.builder()
            .documentId(request.getDocumentId())
            .versionA(request.getVersionA())
            .versionB(request.getVersionB())
            .summary(DiffCompareResponse.SummaryDto.builder().totalChanges(0).added(0).modified(0).deleted(0).build())
            .details(Collections.emptyList()).build();
    }
}