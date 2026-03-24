package com.aiplatform.bidding.service;

import com.aiplatform.bidding.dto.request.BiddingCheckRequest;
import com.aiplatform.bidding.dto.request.CheckOptions;
import com.aiplatform.bidding.dto.response.*;
import com.aiplatform.bidding.domain.enums.ClauseType;
import com.aiplatform.bidding.dto.response.MatchResultDto.MatchType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BiddingCheckServiceTest {

    @Mock
    private DocumentParserService documentParserService;

    @Mock
    private ClauseExtractorService clauseExtractorService;

    @Mock
    private ClauseMatcherService clauseMatcherService;

    @Mock
    private FormatCheckerService formatCheckerService;

    @Mock
    private ReportGeneratorService reportGeneratorService;

    @Mock
    private MultipartFile tenderFile;

    @Mock
    private MultipartFile biddingFile;

    private BiddingCheckService service;

    @BeforeEach
    void setUp() {
        service = new BiddingCheckService(
            documentParserService, clauseExtractorService,
            clauseMatcherService, formatCheckerService, reportGeneratorService
        );
    }

    @Test
    @DisplayName("Should orchestrate bidding check flow")
    void checkBidding_shouldOrchestrateFlow() throws Exception {
        // Setup mocks
        ParsedDocumentDto tenderDoc = new ParsedDocumentDto(
            "t1", "t.pdf", "PDF", 1, "text",
            List.of(), List.of(), List.of()
        );
        ParsedDocumentDto biddingDoc = new ParsedDocumentDto(
            "b1", "b.pdf", "PDF", 1, "text",
            List.of(), List.of(), List.of()
        );

        when(tenderFile.getOriginalFilename()).thenReturn("tender.pdf");
        when(tenderFile.getBytes()).thenReturn(new byte[]{1});
        when(biddingFile.getOriginalFilename()).thenReturn("bidding.pdf");
        when(biddingFile.getBytes()).thenReturn(new byte[]{1});

        when(documentParserService.parse(any(byte[].class), eq("tender.pdf")))
            .thenReturn(tenderDoc);
        when(documentParserService.parse(any(byte[].class), eq("bidding.pdf")))
            .thenReturn(biddingDoc);

        when(clauseExtractorService.extractClauses(any())).thenReturn(List.of());
        when(clauseMatcherService.matchClauses(any(), any())).thenReturn(List.of());
        when(formatCheckerService.checkFormat(any())).thenReturn(
            new FormatCheckDto(
                new CompletenessCheck(100, List.of()),
                new SignatureCheck(100, List.of()),
                new DateCheck(100, List.of()),
                100
            )
        );
        BiddingCheckReportDto fakeReport = mock(BiddingCheckReportDto.class);
        when(reportGeneratorService.generateReport(any(), any(), any()))
            .thenReturn(fakeReport);

        BiddingCheckRequest request = new BiddingCheckRequest(tenderFile, biddingFile, new CheckOptions(true, true, true));
        BiddingCheckReportDto result = service.checkBidding(request);

        assertNotNull(result);
        verify(documentParserService, times(2)).parse(any(byte[].class), any());
        verify(clauseExtractorService, times(2)).extractClauses(any());
        verify(clauseMatcherService).matchClauses(any(), any());
        verify(formatCheckerService).checkFormat(any());
        verify(reportGeneratorService).generateReport(any(), any(), any());
    }

    @Test
    @DisplayName("Should use default check options when null")
    void checkBidding_shouldUseDefaultCheckOptions() throws Exception {
        ParsedDocumentDto tenderDoc = new ParsedDocumentDto(
            "t1", "t.pdf", "PDF", 1, "text",
            List.of(), List.of(), List.of()
        );
        ParsedDocumentDto biddingDoc = new ParsedDocumentDto(
            "b1", "b.pdf", "PDF", 1, "text",
            List.of(), List.of(), List.of()
        );

        when(tenderFile.getOriginalFilename()).thenReturn("tender.pdf");
        when(tenderFile.getBytes()).thenReturn(new byte[]{1});
        when(biddingFile.getOriginalFilename()).thenReturn("bidding.pdf");
        when(biddingFile.getBytes()).thenReturn(new byte[]{1});

        when(documentParserService.parse(any(byte[].class), any()))
            .thenReturn(tenderDoc)
            .thenReturn(biddingDoc);
        when(clauseExtractorService.extractClauses(any())).thenReturn(List.of());
        when(clauseMatcherService.matchClauses(any(), any())).thenReturn(List.of());
        when(formatCheckerService.checkFormat(any())).thenReturn(
            new FormatCheckDto(
                new CompletenessCheck(100, List.of()),
                new SignatureCheck(100, List.of()),
                new DateCheck(100, List.of()),
                100
            )
        );
        BiddingCheckReportDto fakeReport = mock(BiddingCheckReportDto.class);
        when(reportGeneratorService.generateReport(any(), any(), any()))
            .thenReturn(fakeReport);

        BiddingCheckRequest request = new BiddingCheckRequest(tenderFile, biddingFile, null);
        BiddingCheckReportDto result = service.checkBidding(request);

        assertNotNull(result);
    }

    @Test
    @DisplayName("Should propagate parsing exceptions")
    void checkBidding_shouldPropagateParsingExceptions() throws Exception {
        when(tenderFile.getOriginalFilename()).thenReturn("tender.pdf");
        when(tenderFile.getBytes()).thenThrow(new RuntimeException("File read error"));

        BiddingCheckRequest request = new BiddingCheckRequest(tenderFile, biddingFile, new CheckOptions(true, true, true));

        assertThrows(RuntimeException.class, () -> service.checkBidding(request));
    }

    @Test
    @DisplayName("Should process clauses through full pipeline")
    void checkBidding_shouldProcessClausesThroughPipeline() throws Exception {
        ClauseDto tenderClause = new ClauseDto("1", "条款", "内容", false,
            ClauseType.OTHER, 1, 1, "第1条 条款");
        ClauseDto biddingClause = new ClauseDto("1", "条款", "内容", false,
            ClauseType.OTHER, 1, 1, "第1条 条款");

        ParsedDocumentDto tenderDoc = new ParsedDocumentDto(
            "t1", "t.pdf", "PDF", 1, "第1条 条款",
            List.of(), List.of(), List.of()
        );
        ParsedDocumentDto biddingDoc = new ParsedDocumentDto(
            "b1", "b.pdf", "PDF", 1, "第1条 条款",
            List.of(), List.of(), List.of()
        );

        MatchResultDto matchResult = new MatchResultDto(
            tenderClause, biddingClause, MatchType.EXACT, 1.0, List.of()
        );

        when(tenderFile.getOriginalFilename()).thenReturn("tender.pdf");
        when(tenderFile.getBytes()).thenReturn(new byte[]{1});
        when(biddingFile.getOriginalFilename()).thenReturn("bidding.pdf");
        when(biddingFile.getBytes()).thenReturn(new byte[]{1});

        when(documentParserService.parse(any(byte[].class), eq("tender.pdf")))
            .thenReturn(tenderDoc);
        when(documentParserService.parse(any(byte[].class), eq("bidding.pdf")))
            .thenReturn(biddingDoc);

        when(clauseExtractorService.extractClauses("第1条 条款"))
            .thenReturn(List.of(tenderClause))
            .thenReturn(List.of(biddingClause));

        when(clauseMatcherService.matchClauses(any(), any()))
            .thenReturn(List.of(matchResult));

        when(formatCheckerService.checkFormat(any())).thenReturn(
            new FormatCheckDto(
                new CompletenessCheck(100, List.of()),
                new SignatureCheck(100, List.of()),
                new DateCheck(100, List.of()),
                100
            )
        );

        BiddingCheckReportDto fakeReport = mock(BiddingCheckReportDto.class);
        when(reportGeneratorService.generateReport(any(), any(), any()))
            .thenReturn(fakeReport);

        BiddingCheckRequest request = new BiddingCheckRequest(tenderFile, biddingFile, new CheckOptions(true, true, true));
        BiddingCheckReportDto result = service.checkBidding(request);

        assertNotNull(result);
        verify(clauseExtractorService, times(2)).extractClauses(any());
        verify(clauseMatcherService).matchClauses(any(), any());
    }
}