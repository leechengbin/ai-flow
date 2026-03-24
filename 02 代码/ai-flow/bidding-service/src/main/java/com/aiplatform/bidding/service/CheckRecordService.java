package com.aiplatform.bidding.service;

import com.aiplatform.bidding.dto.response.BiddingCheckReportDto;
import com.aiplatform.bidding.entity.BiddingCheckRecord;
import com.aiplatform.bidding.exception.ReportSerializationException;
import com.aiplatform.bidding.repository.BiddingCheckRecordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckRecordService {
    private final BiddingCheckRecordRepository recordRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public BiddingCheckRecord saveRecord(BiddingCheckReportDto report, String tenderDocId, String biddingDocId) {
        BiddingCheckRecord record = BiddingCheckRecord.builder()
            .id(report.reportId())
            .tenderDocumentId(tenderDocId)
            .biddingDocumentId(biddingDocId)
            .reportJson(toJson(report))
            .totalScore(report.summary().totalScore())
            .coverageRate(report.summary().coverageRate())
            .formatScore(report.summary().formatScore())
            .riskLevel(report.summary().riskLevel().name())
            .eliminationRisk(report.summary().eliminationRisk())
            .build();

        log.info("Saving check record: {} with score: {}", record.getId(), record.getTotalScore());
        return recordRepository.save(record);
    }

    @Transactional(readOnly = true)
    public Optional<BiddingCheckReportDto> getReport(String reportId) {
        return recordRepository.findById(reportId)
            .map(this::fromJson);
    }

    private String toJson(BiddingCheckReportDto report) {
        try {
            return objectMapper.writeValueAsString(report);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize report to JSON", e);
            throw new ReportSerializationException("Failed to serialize report to JSON", e);
        }
    }

    private BiddingCheckReportDto fromJson(BiddingCheckRecord record) {
        try {
            return objectMapper.readValue(record.getReportJson(), BiddingCheckReportDto.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize report from JSON", e);
            throw new ReportSerializationException("Failed to deserialize report from JSON", e);
        }
    }
}