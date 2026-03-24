package com.aiplatform.bidding.service;

import com.aiplatform.bidding.dto.request.BiddingCheckRequest;
import com.aiplatform.bidding.dto.response.*;
import com.aiplatform.bidding.dto.response.BiddingCheckReportDto.*;
import com.aiplatform.bidding.exception.DocumentParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BiddingCheckService {
    private final DocumentParserService documentParserService;
    private final ClauseExtractorService clauseExtractorService;
    private final ClauseMatcherService clauseMatcherService;
    private final FormatCheckerService formatCheckerService;
    private final ReportGeneratorService reportGeneratorService;

    @Transactional
    public BiddingCheckReportDto checkBidding(BiddingCheckRequest request) {
        log.info("Starting bidding check for files: {}, {}",
            request.tenderFile().getOriginalFilename(),
            request.biddingFile().getOriginalFilename());

        try {
            // 1. Parse documents
            ParsedDocumentDto tenderDoc = parseDocument(request.tenderFile());
            ParsedDocumentDto biddingDoc = parseDocument(request.biddingFile());

            // 2. Extract clauses
            List<ClauseDto> tenderClauses = clauseExtractorService.extractClauses(tenderDoc.fullText());
            List<ClauseDto> biddingClauses = clauseExtractorService.extractClauses(biddingDoc.fullText());
            log.info("Extracted {} tender clauses and {} bidding clauses", tenderClauses.size(), biddingClauses.size());

            // 3. Match clauses
            List<MatchResultDto> matchResults = clauseMatcherService.matchClauses(tenderClauses, biddingClauses);
            log.info("Matched {} tender clauses against bidding clauses", matchResults.size());

            // 4. Check format
            FormatCheckDto formatCheck = formatCheckerService.checkFormat(biddingDoc);
            log.info("Format check completed with score: {}", formatCheck.totalScore());

            // 5. Generate report
            CheckOptions options = request.checkOptions() != null
                ? request.checkOptions()
                : new CheckOptions(true, true, true);
            BiddingCheckReportDto report = reportGeneratorService.generateReport(matchResults, formatCheck, options);

            log.info("Report generated: {} with total score: {}", report.reportId(), report.summary().totalScore());
            return report;

        } catch (Exception e) {
            log.error("Error during bidding check", e);
            throw new RuntimeException("Bidding check failed: " + e.getMessage(), e);
        }
    }

    private ParsedDocumentDto parseDocument(org.springframework.web.multipart.MultipartFile file) {
        String fileName = file.getOriginalFilename();
        String fileType = getFileType(fileName);

        try {
            byte[] content = file.getBytes();
            return documentParserService.parse(content, fileName);
        } catch (IOException e) {
            throw new DocumentParseException("Failed to read file: " + fileName, e);
        }
    }

    private String getFileType(String fileName) {
        if (fileName == null) return "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }
}
