# 标书检查系统重构实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重构 bidding-service 的标书检查流程，新增文档解析、条款提取、格式检查、报告生成等模块，实现完整的双视图报告（JSON + 可视化数据）。

**Architecture:** 基于现有 bidding-service 结构，新增 DocumentParser、ClauseExtractor、FormatChecker、ReportGenerator 组件，保持 StateMachineEngine 复用。

**Tech Stack:** Spring Boot 3.2, Apache PDFBox 3.0.1, Apache POI 5.2.5, PostgreSQL + pgvector, MinIO

---

## 文件结构

```
bidding-service/src/main/java/com/aiplatform/bidding/
├── controller/
│   └── BiddingCheckController.java       # 修改：增强检查接口
├── service/
│   ├── BiddingCheckService.java          # 重构：主检查流程
│   ├── DocumentParserService.java         # 新增：文档解析入口
│   ├── PdfParserService.java             # 新增：PDF解析
│   ├── DocxParserService.java           # 新增：Word解析
│   ├── ClauseExtractorService.java       # 新增：条款提取
│   ├── ClauseMatcherService.java         # 新增：条款匹配
│   ├── FormatCheckerService.java         # 新增：格式检查
│   ├── ReportGeneratorService.java       # 新增：报告生成
│   └── SuggestionService.java            # 已有：智能建议
├── domain/entity/
│   ├── BiddingDocument.java              # 修改：增加字段
│   ├── Clause.java                       # 修改：增加字段
│   └── BiddingCheckRecord.java          # 新增：检查记录实体
├── dto/
│   ├── request/
│   │   └── BiddingCheckRequest.java     # 修改：支持文件上传
│   └── response/
│       ├── BiddingCheckResponse.java    # 重构：完整报告结构
│       ├── ParsedDocumentDto.java       # 新增：解析结果DTO
│       ├── ClauseDto.java               # 新增：条款DTO
│       └── VisualizationDto.java         # 新增：可视化数据DTO
└── config/
    └── MinioConfig.java                 # 新增：MinIO配置
```

---

## 实现任务

### Task 1: 文档解析服务 (DocumentParser)

**Files:**
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/service/DocumentParserService.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/service/PdfParserService.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/service/DocxParserService.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/dto/response/ParsedDocumentDto.java`

- [ ] **Step 1: 创建 DocumentParserService 接口**

```java
public interface DocumentParserService {
    ParsedDocumentDto parse(byte[] content, String fileName);
    boolean supports(String fileType);
}
```

- [ ] **Step 2: 创建 PdfParserService 实现**

使用 Apache PDFBox 3.0.1 解析 PDF，提取：
- 每页文本内容
- 签章位置（通过图片检测）
- 日期信息（正则匹配）

- [ ] **Step 3: 创建 DocxParserService 实现**

使用 Apache POI 5.2.5 解析 Word，提取：
- 段落文本
- 表格内容
- 签章信息（通过 VBA 或图片检测）

- [ ] **Step 4: 创建 ParsedDocumentDto**

```java
public record ParsedDocumentDto(
    String documentId,
    String fileName,
    String fileType,
    int totalPages,
    String fullText,
    List<PageContentDto> pages,
    List<SignatureDto> signatures,
    List<DateInfoDto> dates
) {}
```

- [ ] **Step 5: 提交代码**

```bash
git add bidding-service/src/main/java/com/aiplatform/bidding/service/DocumentParserService.java
git add bidding-service/src/main/java/com/aiplatform/bidding/service/PdfParserService.java
git add bidding-service/src/main/java/com/aiplatform/bidding/service/DocxParserService.java
git add bidding-service/src/main/java/com/aiplatform/bidding/dto/response/ParsedDocumentDto.java
git commit -m "feat(bidding): add document parser services"
```

---

### Task 2: 条款提取服务 (ClauseExtractor)

**Files:**
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/service/ClauseExtractorService.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/dto/response/ClauseDto.java`
- Modify: `bidding-service/src/main/java/com/aiplatform/bidding/domain/entity/Clause.java`

- [ ] **Step 1: 创建 ClauseDto**

```java
public record ClauseDto(
    String clauseNumber,
    String title,
    String content,
    boolean isStarred,
    ClauseType type,
    int startPage,
    int endPage,
    String rawText
) {}
```

- [ ] **Step 2: 创建 ClauseExtractorService**

条款识别规则：
- `第X条` - 中文数字条款
- `X.Y.Z` - 点分层次编号
- `★` 或 `☆` - 星号标记

```java
@Service
public class ClauseExtractorService {
    private static final Pattern STARRED_PATTERN = Pattern.compile("★|☆");
    private static final Pattern CHINESE_CLAUSE_PATTERN = Pattern.compile("第([一二三四五六七八九十百零\\d]+)条");
    private static final Pattern DOT_CLAUSE_PATTERN = Pattern.compile("(\\d+\\.\\d+(?:\\.\\d+)?)");

    public List<ClauseDto> extractClauses(String text) {
        // 实现条款提取逻辑
    }

    private boolean isStarred(String text) {
        return STARRED_PATTERN.matcher(text).find();
    }
}
```

- [ ] **Step 3: 修改 Clause 实体增加字段**

```java
@Entity
@Table(name = "clauses")
public class Clause {
    // 新增字段
    private String title;
    private boolean isStarred;
    private ClauseType type;
    private Integer startPage;
    private Integer endPage;
}
```

- [ ] **Step 4: 提交代码**

```bash
git add bidding-service/src/main/java/com/aiplatform/bidding/service/ClauseExtractorService.java
git add bidding-service/src/main/java/com/aiplatform/bidding/dto/response/ClauseDto.java
git add bidding-service/src/main/java/com/aiplatform/bidding/domain/entity/Clause.java
git commit -m "feat(bidding): add clause extractor service"
```

---

### Task 3: 条款匹配服务 (ClauseMatcher)

**Files:**
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/service/ClauseMatcherService.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/dto/response/MatchResultDto.java`

- [ ] **Step 1: 创建 MatchResultDto**

```java
public record MatchResultDto(
    ClauseDto tenderClause,
    ClauseDto biddingClause,
    MatchType matchType,
    double similarityScore,
    List<ContentIssueDto> contentIssues
) {}

public enum MatchType {
    EXACT,      // 完全匹配
    SEMANTIC,   // 语义匹配
    PARTIAL,    // 部分匹配
    MISSING     // 完全缺失
}
```

- [ ] **Step 2: 实现精确匹配（条款编号）**

```java
@Service
public class ClauseMatcherService {
    public List<MatchResultDto> matchClauses(
            List<ClauseDto> tenderClauses,
            List<ClauseDto> biddingClauses) {

        List<MatchResultDto> results = new ArrayList<>();

        for (ClauseDto tender : tenderClauses) {
            Optional<ClauseDto> exactMatch = biddingClauses.stream()
                .filter(b -> b.clauseNumber().equals(tender.clauseNumber()))
                .findFirst();

            if (exactMatch.isPresent()) {
                results.add(new MatchResultDto(tender, exactMatch.get(),
                    MatchType.EXACT, 1.0, List.of()));
            } else {
                // 语义匹配逻辑
                ClauseDto semanticMatch = findSemanticMatch(tender, biddingClauses);
                if (semanticMatch != null) {
                    double similarity = calculateSimilarity(tender, semanticMatch);
                    results.add(new MatchResultDto(tender, semanticMatch,
                        MatchType.SEMANTIC, similarity, List.of()));
                } else {
                    results.add(new MatchResultDto(tender, null,
                        MatchType.MISSING, 0.0, List.of()));
                }
            }
        }
        return results;
    }
}
```

- [ ] **Step 3: 提交代码**

```bash
git add bidding-service/src/main/java/com/aiplatform/bidding/service/ClauseMatcherService.java
git add bidding-service/src/main/java/com/aiplatform/bidding/dto/response/MatchResultDto.java
git commit -m "feat(bidding): add clause matcher service"
```

---

### Task 4: 格式检查服务 (FormatChecker)

**Files:**
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/service/FormatCheckerService.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/dto/response/FormatCheckDto.java`

- [ ] **Step 1: 创建 FormatCheckDto**

```java
public record FormatCheckDto(
    CompletenessCheck completeness,
    SignatureCheck signatures,
    DateCheck dates,
    int totalScore
) {}

public record CompletenessCheck(
    int score,
    List<FormatIssue> issues
) {}

public record SignatureCheck(
    int score,
    List<SignatureIssue> issues
) {}

public record DateCheck(
    int score,
    List<DateIssue> issues
) {}
```

- [ ] **Step 2: 实现 FormatCheckerService**

```java
@Service
public class FormatCheckerService {

    public FormatCheckDto checkFormat(ParsedDocumentDto document) {
        CompletenessCheck completeness = checkCompleteness(document);
        SignatureCheck signatures = checkSignatures(document);
        DateCheck dates = checkDates(document);

        int totalScore = (completeness.score() + signatures.score() + dates.score()) / 3;

        return new FormatCheckDto(completeness, signatures, dates, totalScore);
    }

    private SignatureCheck checkSignatures(ParsedDocumentDto document) {
        // 检查法人签字、公章等
        // 严重程度：CRITICAL - 无签字/公章
    }

    private CompletenessCheck checkCompleteness(ParsedDocumentDto document) {
        // 检查页数、章节完整性
    }

    private DateCheck checkDates(ParsedDocumentDto document) {
        // 检查日期逻辑、有效期
    }
}
```

- [ ] **Step 3: 提交代码**

```bash
git add bidding-service/src/main/java/com/aiplatform/bidding/service/FormatCheckerService.java
git add bidding-service/src/main/java/com/aiplatform/bidding/dto/response/FormatCheckDto.java
git commit -m "feat(bidding): add format checker service"
```

---

### Task 5: 报告生成服务 (ReportGenerator)

**Files:**
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/service/ReportGeneratorService.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/dto/response/VisualizationDto.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/dto/response/BiddingCheckReportDto.java`

- [ ] **Step 1: 创建 VisualizationDto**

```java
public record VisualizationDto(
    PieChartData coverageChart,
    BarChartData issueDistribution,
    GaugeData riskGauge
) {}

public record PieChartData(
    List<String> labels,
    List<Integer> data
) {}

public record GaugeData(
    double value,
    double min,
    double max,
    List<GaugeThreshold> thresholds
) {}
```

- [ ] **Step 2: 创建 BiddingCheckReportDto**

```java
public record BiddingCheckReportDto(
    String reportId,
    String checkId,
    LocalDateTime generatedAt,
    SummaryDto summary,
    CoverageDto coverage,
    List<IssueDto> issues,
    FormatCheckDto format,
    VisualizationDto visualization
) {}

public record SummaryDto(
    double totalScore,
    double coverageRate,
    double formatScore,
    boolean eliminationRisk,
    RiskLevel riskLevel
) {}
```

- [ ] **Step 3: 实现 ReportGeneratorService**

```java
@Service
public class ReportGeneratorService {

    public BiddingCheckReportDto generateReport(
            List<MatchResultDto> matchResults,
            FormatCheckDto formatCheck,
            CheckOptions options) {

        SummaryDto summary = calculateSummary(matchResults, formatCheck);
        CoverageDto coverage = calculateCoverage(matchResults);
        List<IssueDto> issues = extractIssues(matchResults, formatCheck);
        VisualizationDto visualization = generateVisualization(summary, issues);

        return new BiddingCheckReportDto(
            "RPT-" + UUID.randomUUID(),
            "CHECK-" + UUID.randomUUID(),
            LocalDateTime.now(),
            summary,
            coverage,
            issues,
            formatCheck,
            visualization
        );
    }

    private RiskLevel calculateRiskLevel(SummaryDto summary) {
        if (summary.eliminationRisk()) return RiskLevel.CRITICAL;
        if (summary.totalScore() < 60) return RiskLevel.HIGH;
        if (summary.totalScore() < 80) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }
}
```

- [ ] **Step 4: 提交代码**

```bash
git add bidding-service/src/main/java/com/aiplatform/bidding/service/ReportGeneratorService.java
git add bidding-service/src/main/java/com/aiplatform/bidding/dto/response/VisualizationDto.java
git add bidding-service/src/main/java/com/aiplatform/bidding/dto/response/BiddingCheckReportDto.java
git commit -m "feat(bidding): add report generator service"
```

---

### Task 6: 重构 BiddingCheckService 主流程

**Files:**
- Modify: `bidding-service/src/main/java/com/aiplatform/bidding/service/BiddingCheckService.java`
- Modify: `bidding-service/src/main/java/com/aiplatform/bidding/controller/BiddingController.java`
- Modify: `bidding-service/src/main/java/com/aiplatform/bidding/dto/request/BiddingCheckRequest.java`
- Modify: `bidding-service/src/main/java/com/aiplatform/bidding/dto/response/BiddingCheckResponse.java`

- [ ] **Step 1: 修改 BiddingCheckRequest 支持文件上传**

```java
public record BiddingCheckRequest(
    MultipartFile tenderFile,
    MultipartFile biddingFile,
    CheckOptions checkOptions
) {}

public record CheckOptions(
    boolean checkCoverage,
    boolean checkFormat,
    boolean checkStarred
) {}
```

- [ ] **Step 2: 重构 BiddingCheckService**

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class BiddingCheckService {
    private final DocumentParserService documentParserService;
    private final ClauseExtractorService clauseExtractorService;
    private final ClauseMatcherService clauseMatcherService;
    private final FormatCheckerService formatCheckerService;
    private final ReportGeneratorService reportGeneratorService;

    public BiddingCheckReportDto checkBidding(BiddingCheckRequest request) {
        // 1. 解析文档
        ParsedDocumentDto tenderDoc = documentParserService.parse(
            request.tenderFile().getBytes(),
            request.tenderFile().getOriginalFilename()
        );
        ParsedDocumentDto biddingDoc = documentParserService.parse(
            request.biddingFile().getBytes(),
            request.biddingFile().getOriginalFilename()
        );

        // 2. 提取条款
        List<ClauseDto> tenderClauses = clauseExtractorService.extractClauses(tenderDoc.fullText());
        List<ClauseDto> biddingClauses = clauseExtractorService.extractClauses(biddingDoc.fullText());

        // 3. 匹配条款
        List<MatchResultDto> matchResults = clauseMatcherService.matchClauses(
            tenderClauses, biddingClauses
        );

        // 4. 格式检查
        FormatCheckDto formatCheck = formatCheckerService.checkFormat(biddingDoc);

        // 5. 生成报告
        return reportGeneratorService.generateReport(matchResults, formatCheck, request.checkOptions());
    }
}
```

- [ ] **Step 3: 修改 BiddingController**

```java
@RestController
@RequestMapping("/api/v1/bidding")
@RequiredArgsConstructor
public class BiddingController {
    private final BiddingCheckService biddingCheckService;

    @PostMapping("/check")
    public ResponseEntity<ApiResponse<BiddingCheckReportDto>> checkBidding(
            @ModelAttribute BiddingCheckRequest request) {
        BiddingCheckReportDto report = biddingCheckService.checkBidding(request);
        return ResponseEntity.ok(ApiResponse.success(report));
    }
}
```

- [ ] **Step 4: 提交代码**

```bash
git add bidding-service/src/main/java/com/aiplatform/bidding/service/BiddingCheckService.java
git add bidding-service/src/main/java/com/aiplatform/bidding/controller/BiddingController.java
git add bidding-service/src/main/java/com/aiplatform/bidding/dto/request/BiddingCheckRequest.java
git add bidding-service/src/main/java/com/aiplatform/bidding/dto/response/BiddingCheckResponse.java
git commit -m "refactor(bidding): refactor check flow to use new services"
```

---

### Task 7: 语义匹配服务 (EmbeddingService)

**Files:**
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/service/EmbeddingService.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/repository/ClauseEmbeddingRepository.java`

- [ ] **Step 1: 创建 ClauseEmbeddingRepository**

```java
@Repository
public interface ClauseEmbeddingRepository extends JpaRepository<ClauseEmbedding, String> {
    @Query(value = """
        SELECT id, clause_id, 1 - (content_vector <=> :queryVector) AS similarity
        FROM clause_embeddings
        WHERE document_id = :documentId
        ORDER BY content_vector <=> :queryVector
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findSimilarClauses(@Param("documentId") String documentId,
                                       @Param("queryVector") Float[] queryVector,
                                       @Param("limit") int limit);
}
```

- [ ] **Step 2: 创建 EmbeddingService**

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {
    private final ClauseEmbeddingRepository embeddingRepository;
    private final EntityManager entityManager;

    // 使用 OpenAI embeddings 或本地模型
    // 这里使用简化实现：通过 AI service 调用

    public float[] generateEmbedding(String text) {
        // 调用 ai-service 生成嵌入向量
        // TODO: 实现实际的嵌入生成
        return new float[1536]; // 简化返回
    }

    public List<ClauseEmbedding> findSimilarClauses(String documentId, String queryText, int topK) {
        float[] queryVector = generateEmbedding(queryText);
        List<Object[]> results = embeddingRepository.findSimilarClauses(documentId, queryVector, topK);

        return results.stream()
            .map(row -> {
                ClauseEmbedding emb = new ClauseEmbedding();
                emb.setId((String) row[0]);
                emb.setClauseId((String) row[1]);
                emb.setSimilarity(((Number) row[2]).floatValue());
                return emb;
            })
            .toList();
    }
}
```

- [ ] **Step 3: 修改 ClauseMatcherService 支持语义匹配**

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ClauseMatcherService {
    private final EmbeddingService embeddingService;

    public List<MatchResultDto> matchClauses(
            List<ClauseDto> tenderClauses,
            List<ClauseDto> biddingClauses,
            String biddingDocumentId) {

        List<MatchResultDto> results = new ArrayList<>();

        for (ClauseDto tender : tenderClauses) {
            Optional<ClauseDto> exactMatch = biddingClauses.stream()
                .filter(b -> b.clauseNumber().equals(tender.clauseNumber()))
                .findFirst();

            if (exactMatch.isPresent()) {
                // 内容完整性检查
                ContentIssue issue = checkContentCompleteness(tender, exactMatch.get());
                MatchType matchType = issue == null ? MatchType.EXACT : MatchType.PARTIAL;
                results.add(new MatchResultDto(tender, exactMatch.get(),
                    matchType, 1.0, issue == null ? List.of() : List.of(issue)));
            } else {
                // 语义匹配
                List<ClauseEmbedding> similar = embeddingService.findSimilarClauses(
                    biddingDocumentId, tender.content(), 3
                );
                if (!similar.isEmpty() && similar.get(0).getSimilarity() > 0.8) {
                    ClauseDto bestMatch = findClauseById(biddingClauses, similar.get(0).getClauseId());
                    results.add(new MatchResultDto(tender, bestMatch,
                        MatchType.SEMANTIC, similar.get(0).getSimilarity(), List.of()));
                } else {
                    results.add(new MatchResultDto(tender, null,
                        MatchType.MISSING, 0.0, List.of()));
                }
            }
        }
        return results;
    }
}
```

- [ ] **Step 4: 提交代码**

```bash
git add bidding-service/src/main/java/com/aiplatform/bidding/service/EmbeddingService.java
git add bidding-service/src/main/java/com/aiplatform/bidding/repository/ClauseEmbeddingRepository.java
git add bidding-service/src/main/java/com/aiplatform/bidding/service/ClauseMatcherService.java
git commit -m "feat(bidding): add embedding service for semantic matching"
```

---

### Task 8: 报告查询接口

**Files:**
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/service/CheckRecordService.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/repository/BiddingCheckRecordRepository.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/domain/entity/BiddingCheckRecord.java`
- Modify: `bidding-service/src/main/java/com/aiplatform/bidding/controller/BiddingController.java`

- [ ] **Step 1: 创建 CheckRecordService**

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class CheckRecordService {
    private final BiddingCheckRecordRepository recordRepository;

    @Transactional
    public BiddingCheckRecord saveRecord(BiddingCheckReportDto report,
            String tenderDocId, String biddingDocId) {
        BiddingCheckRecord record = BiddingCheckRecord.builder()
            .id(report.reportId())
            .tenderDocumentId(tenderDocId)
            .biddingDocumentId(biddingDocId)
            .reportJson(toJson(report))
            .totalScore(report.summary().totalScore())
            .coverageRate(report.summary().coverageRate())
            .formatScore(report.summary().formatScore())
            .riskLevel(report.summary().riskLevel())
            .eliminationRisk(report.summary().eliminationRisk())
            .build();
        return recordRepository.save(record);
    }

    @Transactional(readOnly = true)
    public Optional<BiddingCheckReportDto> getReport(String reportId) {
        return recordRepository.findById(reportId)
            .map(this::fromJson);
    }
}
```

- [ ] **Step 2: 添加报告查询接口到 BiddingController**

```java
@GetMapping("/report/{reportId}")
public ResponseEntity<ApiResponse<BiddingCheckReportDto>> getReport(@PathVariable String reportId) {
    return checkRecordService.getReport(reportId)
        .map(report -> ResponseEntity.ok(ApiResponse.success(report)))
        .orElse(ResponseEntity.notFound().build());
}
```

- [ ] **Step 3: 修改 BiddingCheckService 保存记录**

```java
public BiddingCheckReportDto checkBidding(BiddingCheckRequest request) {
    // ... 现有逻辑 ...
    BiddingCheckReportDto report = reportGeneratorService.generateReport(...);

    // 保存检查记录
    checkRecordService.saveRecord(report, tenderDoc.id(), biddingDoc.id());

    return report;
}
```

- [ ] **Step 4: 提交代码**

```bash
git add bidding-service/src/main/java/com/aiplatform/bidding/service/CheckRecordService.java
git add bidding-service/src/main/java/com/aiplatform/bidding/controller/BiddingController.java
git add bidding-service/src/main/java/com/aiplatform/bidding/service/BiddingCheckService.java
git commit -m "feat(bidding): add report query endpoint and record persistence"
```

---

### Task 9: 单元测试（完整）

**Files:**
- Create: `bidding-service/src/test/java/com/aiplatform/bidding/service/DocumentParserServiceTest.java`
- Create: `bidding-service/src/test/java/com/aiplatform/bidding/service/BiddingCheckServiceTest.java`
- Create: `bidding-service/src/test/java/com/aiplatform/bidding/service/EmbeddingServiceTest.java`
- Create: `bidding-service/src/test/java/com/aiplatform/bidding/service/ClauseExtractorServiceTest.java`
- Create: `bidding-service/src/test/java/com/aiplatform/bidding/service/ClauseMatcherServiceTest.java`
- Create: `bidding-service/src/test/java/com/aiplatform/bidding/service/FormatCheckerServiceTest.java`
- Create: `bidding-service/src/test/java/com/aiplatform/bidding/service/ReportGeneratorServiceTest.java`

- [ ] **Step 1: DocumentParserServiceTest**

```java
@ExtendWith(MockitoExtension.class)
class DocumentParserServiceTest {

    @Test
    void parse_shouldDetectPdfFile() {
        DocumentParserService service = new PdfParserService();
        assertTrue(service.supports("pdf"));
        assertFalse(service.supports("docx"));
    }

    @Test
    void parse_shouldExtractTextFromSimplePdf() {
        // 使用测试 PDF 文件
        byte[] content = loadTestFile("simple.pdf");
        ParsedDocumentDto result = pdfParserService.parse(content, "simple.pdf");

        assertNotNull(result.fullText());
        assertTrue(result.fullText().contains("测试内容"));
    }
}
```

- [ ] **Step 2: BiddingCheckServiceTest (Orchestrator)**

```java
@ExtendWith(MockitoExtension.class)
class BiddingCheckServiceTest {

    @Mock private DocumentParserService documentParserService;
    @Mock private ClauseExtractorService clauseExtractorService;
    @Mock private ClauseMatcherService clauseMatcherService;
    @Mock private FormatCheckerService formatCheckerService;
    @Mock private ReportGeneratorService reportGeneratorService;

    private BiddingCheckService service;

    @BeforeEach
    void setUp() {
        service = new BiddingCheckService(
            documentParserService, clauseExtractorService,
            clauseMatcherService, formatCheckerService, reportGeneratorService
        );
    }

    @Test
    void checkBidding_shouldParseDocumentAndMatchClauses() {
        // 设置 mock 行为
        when(documentParserService.parse(any(), any()))
            .thenReturn(new ParsedDocumentDto(...));
        when(clauseExtractorService.extractClauses(any()))
            .thenReturn(List.of());
        when(clauseMatcherService.matchClauses(any(), any(), any()))
            .thenReturn(List.of());
        when(formatCheckerService.checkFormat(any()))
            .thenReturn(new FormatCheckDto(...));
        when(reportGeneratorService.generateReport(any(), any(), any()))
            .thenReturn(new BiddingCheckReportDto(...));

        BiddingCheckRequest request = new BiddingCheckRequest(...);
        BiddingCheckReportDto result = service.checkBidding(request);

        assertNotNull(result);
        verify(documentParserService, times(2)).parse(any(), any());
        verify(clauseMatcherService).matchClauses(any(), any(), any());
    }
}
```

- [ ] **Step 3: 提交测试代码**

```bash
git add bidding-service/src/test/java/com/aiplatform/bidding/service/DocumentParserServiceTest.java
git add bidding-service/src/test/java/com/aiplatform/bidding/service/BiddingCheckServiceTest.java
git add bidding-service/src/test/java/com/aiplatform/bidding/service/EmbeddingServiceTest.java
git commit -m "test(bidding): add missing unit tests"
```

---

### Task 10: 数据库迁移（扩展）

**Files:**
- Modify: `bidding-service/src/main/resources/db/migration/V2__add_bidding_check_fields.sql`

- [ ] **Step 1: 扩展迁移脚本增加向量表**

```sql
-- 追加到 V2__add_bidding_check_fields.sql

-- 创建条款向量表（用于语义匹配）
CREATE TABLE clause_embeddings (
    id VARCHAR(50) PRIMARY KEY,
    clause_id VARCHAR(50) REFERENCES clauses(id),
    document_id VARCHAR(50) REFERENCES bidding_documents(id),
    content_vector VECTOR(1536),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 向量索引
CREATE INDEX idx_embeddings_document ON clause_embeddings(document_id);
CREATE INDEX idx_embeddings_clause ON clause_embeddings(clause_id);
```

- [ ] **Step 2: 提交迁移**

```bash
git add bidding-service/src/main/resources/db/migration/V2__add_bidding_check_fields.sql
git commit -m "db(bidding): add clause embeddings table for semantic search"
```

---

## 验收标准（更新）

1. ✅ `POST /api/v1/bidding/check` 支持文件上传
2. ✅ 条款提取准确识别编号格式（中文/点分/星号）
3. ✅ 条款匹配支持精确匹配、语义匹配、部分匹配
4. ✅ 格式检查覆盖完整性、签章、日期
5. ✅ 报告生成 JSON + 可视化数据结构
6. ✅ `GET /api/v1/bidding/report/{reportId}` 查询历史报告
7. ✅ EmbeddingService 支持语义相似度检索
8. ✅ 所有新服务有单元测试覆盖
9. ✅ 数据库迁移脚本包含向量表

- [ ] **Step 1: ClauseExtractorServiceTest**

```java
@ExtendWith(MockitoExtension.class)
class ClauseExtractorServiceTest {

    private ClauseExtractorService service;

    @BeforeEach
    void setUp() {
        service = new ClauseExtractorService();
    }

    @Test
    void extractClauses_shouldExtractNumberedClauses() {
        String text = "第1条 投标人资格要求\n第2条 招标范围\n第3条 ★星号条款";

        List<ClauseDto> clauses = service.extractClauses(text);

        assertEquals(3, clauses.size());
        assertEquals("1", clauses.get(0).clauseNumber());
        assertTrue(clauses.get(2).isStarred());
    }

    @Test
    void extractClauses_shouldExtractDotNotationClauses() {
        String text = "3.1 技术要求\n3.2 商务条款";

        List<ClauseDto> clauses = service.extractClauses(text);

        assertEquals(2, clauses.size());
        assertEquals("3.1", clauses.get(0).clauseNumber());
    }
}
```

- [ ] **Step 2: ClauseMatcherServiceTest**

```java
@ExtendWith(MockitoExtension.class)
class ClauseMatcherServiceTest {

    private ClauseMatcherService service;

    @BeforeEach
    void setUp() {
        service = new ClauseMatcherService();
    }

    @Test
    void matchClauses_shouldMatchExactByNumber() {
        List<ClauseDto> tender = List.of(
            new ClauseDto("3.1", "技术要求", "内容", false, null, 1, 1, null)
        );
        List<ClauseDto> bidding = List.of(
            new ClauseDto("3.1", "技术要求", "内容", false, null, 1, 1, null)
        );

        List<MatchResultDto> results = service.matchClauses(tender, bidding);

        assertEquals(1, results.size());
        assertEquals(MatchType.EXACT, results.get(0).matchType());
        assertEquals(1.0, results.get(0).similarityScore());
    }

    @Test
    void matchClauses_shouldReportMissing() {
        List<ClauseDto> tender = List.of(
            new ClauseDto("3.1", "技术要求", "内容", false, null, 1, 1, null)
        );
        List<ClauseDto> bidding = List.of();

        List<MatchResultDto> results = service.matchClauses(tender, bidding);

        assertEquals(1, results.size());
        assertEquals(MatchType.MISSING, results.get(0).matchType());
    }
}
```

- [ ] **Step 3: FormatCheckerServiceTest**

```java
@ExtendWith(MockitoExtension.class)
class FormatCheckerServiceTest {

    private FormatCheckerService service;

    @BeforeEach
    void setUp() {
        service = new FormatCheckerService();
    }

    @Test
    void checkFormat_shouldDetectMissingSignature() {
        ParsedDocumentDto doc = new ParsedDocumentDto(
            "doc1", "test.pdf", "PDF", 10, "正文内容",
            List.of(), List.of(), List.of()
        );

        FormatCheckDto result = service.checkFormat(doc);

        assertTrue(result.signatures().issues().stream()
            .anyMatch(i -> i.type().equals("LEGAL_SIGNATURE")));
    }
}
```

- [ ] **Step 4: ReportGeneratorServiceTest**

```java
@ExtendWith(MockitoExtension.class)
class ReportGeneratorServiceTest {

    private ReportGeneratorService service;

    @BeforeEach
    void setUp() {
        service = new ReportGeneratorService();
    }

    @Test
    void generateReport_shouldCalculateCorrectSummary() {
        List<MatchResultDto> matches = List.of(
            new MatchResultDto(null, null, MatchType.EXACT, 1.0, List.of()),
            new MatchResultDto(null, null, MatchType.EXACT, 1.0, List.of()),
            new MatchResultDto(null, null, MatchType.MISSING, 0.0, List.of())
        );
        FormatCheckDto format = new FormatCheckDto(null, null, null, 100);

        BiddingCheckReportDto report = service.generateReport(matches, format, null);

        assertEquals(66.67, report.summary().coverageRate(), 0.01);
        assertEquals(83.33, report.summary().totalScore(), 0.01);
    }
}
```

- [ ] **Step 5: 提交测试代码**

```bash
git add bidding-service/src/test/java/com/aiplatform/bidding/service/ClauseExtractorServiceTest.java
git add bidding-service/src/test/java/com/aiplatform/bidding/service/ClauseMatcherServiceTest.java
git add bidding-service/src/test/java/com/aiplatform/bidding/service/FormatCheckerServiceTest.java
git add bidding-service/src/test/java/com/aiplatform/bidding/service/ReportGeneratorServiceTest.java
git commit -m "test(bidding): add unit tests for refactored services"
```

---

### Task 10: 数据库迁移（最终）

**Files:**
- Create: `bidding-service/src/main/resources/db/migration/V2__add_bidding_check_fields.sql`

- [ ] **Step 1: 创建迁移脚本**

```sql
-- V2__add_bidding_check_fields.sql

-- 修改 clauses 表增加字段
ALTER TABLE clauses ADD COLUMN title VARCHAR(255);
ALTER TABLE clauses ADD COLUMN is_starred BOOLEAN DEFAULT FALSE;
ALTER TABLE clauses ADD COLUMN type VARCHAR(20);
ALTER TABLE clauses ADD COLUMN start_page INT;
ALTER TABLE clauses ADD COLUMN end_page INT;

-- 创建检查记录表
CREATE TABLE bidding_check_records (
    id VARCHAR(50) PRIMARY KEY,
    tender_document_id VARCHAR(50) REFERENCES bidding_documents(id),
    bidding_document_id VARCHAR(50) REFERENCES bidding_documents(id),
    report_json TEXT,
    total_score DECIMAL(5,2),
    coverage_rate DECIMAL(5,2),
    format_score DECIMAL(5,2),
    risk_level VARCHAR(20),
    elimination_risk BOOLEAN,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX idx_check_records_tender ON bidding_check_records(tender_document_id);
CREATE INDEX idx_check_records_bidding ON bidding_check_records(bidding_document_id);
```

- [ ] **Step 2: 提交迁移**

```bash
git add bidding-service/src/main/resources/db/migration/V2__add_bidding_check_fields.sql
git commit -m "db(bidding): add check records table and clause extensions"
```

---

## 验收标准

1. ✅ `POST /api/v1/bidding/check` 支持文件上传
2. ✅ 条款提取准确识别编号格式（中文/点分/星号）
3. ✅ 条款匹配支持精确匹配和语义匹配
4. ✅ 格式检查覆盖完整性、签章、日期
5. ✅ 报告生成 JSON + 可视化数据结构
6. ✅ 所有新服务有单元测试覆盖
7. ✅ 数据库迁移脚本可正常执行
