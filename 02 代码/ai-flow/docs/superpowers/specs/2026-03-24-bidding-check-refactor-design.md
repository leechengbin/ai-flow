# 标书检查系统重构设计文档

> **项目：** AI Flow - 标书检查模块重构
> **版本：** V2.0
> **日期：** 2026-03-24
> **架构：** Spring Boot + PostgreSQL + pgvector + MinIO

---

## 一、需求概述

### 1.1 核心功能

用户上传**招标要求文档**和**投标文件**，系统自动检查并输出：
- 投标文件是否覆盖招标要求
- 具体问题清单及修改建议
- 格式规范性检查结果
- 可视化报告

### 1.2 检查维度

| 维度 | 说明 |
|------|------|
| **条款覆盖度** | 逐条检查投标文件是否包含招标要求的每一项条款 |
| **星号条款（废标项）** | 重点标识带★的条款，这些是必须满足的废标项 |
| **格式规范性** | 文件完整性、签章合规、日期逻辑 |

### 1.3 输出形式

- **结构化 JSON** - 评分、问题列表、严重程度、修改建议
- **可视化数据** - 饼图/仪表盘展示覆盖度、问题分布

---

## 二、系统架构

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                      BiddingCheckController                       │
│                   (统一入口：POST /api/v1/bidding/check)          │
└─────────────────────────────┬───────────────────────────────────┘
                              │
         ┌────────────────────┼────────────────────┐
         ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│ DocumentParser   │  │  ClauseMatcher  │  │  FormatChecker  │
│  - PDF/Word解析  │  │  - 编号匹配     │  │  - 完整性检查   │
│  - 文本结构化    │  │  - 语义相似度   │  │  - 签章检查     │
│  - 签章位置识别  │  │  - 覆盖度计算   │  │  - 日期检查     │
└────────┬────────┘  └────────┬────────┘  └────────┬────────┘
         │                    │                    │
         └────────────────────┼────────────────────┘
                              ▼
                   ┌─────────────────────┐
                   │   ReportGenerator   │
                   │  - JSON 结构化报告  │
                   │  - 可视化数据       │
                   └─────────────────────┘
```

### 2.2 组件职责

| 组件 | 职责范围 |
|------|----------|
| `DocumentParser` | PDF/Word 文档解析，提取文本、段落结构、签章位置 |
| `ClauseExtractor` | 从结构化文本中提取条款（编号、标题、内容、是否星号） |
| `ClauseMatcher` | 基于条款编号 + 语义相似度进行匹配 |
| `FormatChecker` | 检查完整性、签章、日期等格式问题 |
| `ReportGenerator` | 生成结构化 JSON 和可视化数据 |

---

## 三、功能详细设计

### 3.1 文档解析 (DocumentParser)

#### 3.1.1 支持格式

| 格式 | 库 | 说明 |
|------|-----|------|
| PDF | Apache PDFBox | 提取文本和签章位置 |
| Word (.docx) | Apache POI | 提取文本、表格、段落结构 |

#### 3.1.2 输出结构

```java
public class ParsedDocument {
    private String documentId;
    private String fileName;
    private String fileType;  // PDF, DOCX
    private int totalPages;
    private List<PageContent> pages;

    // 提取的文本内容
    private String fullText;

    // 签章位置信息
    private List<SignatureInfo> signatures;

    // 关键日期信息
    private List<DateInfo> dates;
}

public class PageContent {
    private int pageNumber;
    private String text;
    private List<Paragraph> paragraphs;
}

public class SignatureInfo {
    private int pageNumber;
    private float x;
    private float y;
    private float width;
    private float height;
    private String type;  // COMPANY_SEAL, LEGAL_SIGNATURE, etc.
}

public class DateInfo {
    private String rawText;
    private LocalDate date;
    private String context;  // 出现在什么上下文中
}
```

### 3.2 条款提取 (ClauseExtractor)

#### 3.2.1 条款识别规则

条款编号识别模式（支持多种格式）：
- `第X条` - 中文数字条款
- `X.Y.Z` - 点分层次编号
- `条款X` / `Clause X` - 英文条款
- `★` 或 `☆` - 星号标记

#### 3.2.2 输出结构

```java
public class Clause {
    private String clauseNumber;    // "3.2.1"
    private String title;           // "项目业绩要求"
    private String content;         // 条款正文
    private boolean isStarred;      // 是否星号条款
    private ClauseType type;        // TENDER_REQUIREMENT, BID_SUBMISSION, GENERAL
    private int startPage;
    private int endPage;
    private String rawText;         // 原始文本
}

public enum ClauseType {
    TENDER_REQUIREMENT,  // 招标要求
    BID_SUBMISSION,      // 投标文件格式要求
    GENERAL,            // 一般条款
    TECHNICAL,          // 技术条款
    COMMERCIAL          // 商务条款
}
```

### 3.3 条款匹配 (ClauseMatcher)

#### 3.3.1 匹配策略

**两级匹配：**

1. **精确匹配（Primary）** - 条款编号完全一致
2. **语义匹配（Secondary）** - 编号不同但语义相似

#### 3.3.2 语义相似度计算

使用 pgvector 存储条款嵌入向量：

```sql
-- 条款向量表
CREATE TABLE clause_embeddings (
    id VARCHAR(50) PRIMARY KEY,
    document_id VARCHAR(50),
    clause_number VARCHAR(50),
    content_vector VECTOR(1536),
    FOREIGN KEY (document_id) REFERENCES bidding_documents(id)
);

-- 相似度查询
SELECT clause_number, 1 - (content_vector <=> query_vector) AS similarity
FROM clause_embeddings
WHERE document_id = ?
ORDER BY content_vector <=> query_vector
LIMIT 5;
```

#### 3.3.3 匹配结果

```java
public class MatchResult {
    private Clause tenderClause;
    private Clause biddingClause;
    private MatchType matchType;  // EXACT, SEMANTIC, PARTIAL, MISSING
    private double similarityScore;
    private List<ContentIssue> contentIssues;
}

public enum MatchType {
    EXACT,           // 完全匹配
    SEMANTIC,        // 语义匹配（编号不同但内容相似）
    PARTIAL,         // 部分匹配（内容有缺失）
    MISSING          // 完全缺失
}

public class ContentIssue {
    private String issueId;
    private IssueType type;
    private String description;
    private String expectedContent;
    private String actualContent;
    private Severity severity;
}
```

### 3.4 格式检查 (FormatChecker)

#### 3.4.1 完整性检查

| 检查项 | 说明 | 严重程度 |
|--------|------|----------|
| 目录对应 | 目录章节与正文对应 | MEDIUM |
| 页数检查 | 正文页数是否符合要求 | LOW |
| 附件完整 | 招标要求的所有附件是否包含 | HIGH |

#### 3.4.2 签章检查

| 检查项 | 说明 | 严重程度 |
|--------|------|----------|
| 法人签字 | 投标函是否法人签字 | CRITICAL |
| 公司公章 | 投标文件是否加盖公章 | CRITICAL |
| 骑缝章 | 多页文件是否加盖骑缝章 | MEDIUM |
| 签字位置 | 签章位置是否正确 | LOW |

#### 3.4.3 日期检查

| 检查项 | 说明 | 严重程度 |
|--------|------|----------|
| 投标日期 | 是否在招标有效期后 | CRITICAL |
| 有效期 | 投标是否在有效期内 | HIGH |
| 日期逻辑 | 日期顺序是否合理 | MEDIUM |

### 3.5 报告生成 (ReportGenerator)

#### 3.5.1 JSON 结构化报告

```java
public class BiddingCheckReport {
    private String reportId;
    private String checkId;
    private LocalDateTime generatedAt;

    // 总体评分
    private Summary summary;

    // 条款覆盖情况
    private Coverage coverage;

    // 问题列表
    private List<Issue> issues;

    // 格式检查结果
    private FormatCheck format;

    // 可视化数据
    private VisualizationData visualization;
}

public class Summary {
    private double totalScore;        // 0-100
    private double coverageRate;       // 条款覆盖度百分比
    private double formatScore;        // 格式规范分数
    private boolean eliminationRisk;   // 是否有废标风险
    private RiskLevel riskLevel;       // LOW, MEDIUM, HIGH, CRITICAL
}

public class Coverage {
    private int totalClauses;         // 招标要求总条款数
    private int matchedClauses;        // 完全匹配数
    private int partialClauses;        // 部分匹配数
    private int missingClauses;       // 缺失条款数
    private int starredMatched;       // 星号条款匹配数
    private int starredMissing;       // 星号条款缺失数
}

public class Issue {
    private String issueId;
    private IssueType type;
    private Severity severity;
    private String clauseNumber;
    private String title;
    private String description;
    private boolean isStarred;        // 是否星号条款
    private boolean eliminationRisk;
    private String suggestion;
    private String contextText;       // 出问题处的原文
}

public enum IssueType {
    MISSING_CLAUSE,          // 缺失条款
    INCOMPLETE_CONTENT,       // 内容不完整
    WRONG_FORMAT,            // 格式错误
    SIGNATURE_MISSING,       // 签章缺失
    DATE_INVALID,            // 日期无效
    SEMANTIC_MISMATCH        // 语义不匹配
}

public enum Severity {
    CRITICAL,    // 废标项
    HIGH,
    MEDIUM,
    LOW
}

public class VisualizationData {
    // 覆盖度饼图
    private PieChartData coverageChart;

    // 问题分布柱状图
    private BarChartData issueDistribution;

    // 严重程度仪表盘
    private GaugeData riskGauge;

    // 问题列表（用于前端渲染）
    private List<Map<String, Object>> issueHeatmap;
}
```

---

## 四、API 设计

### 4.1 检查接口

```
POST /api/v1/bidding/check
Content-Type: multipart/form-data
```

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| tenderFile | File | 是 | 招标要求文档 |
| biddingFile | File | 是 | 投标文件 |
| checkCoverage | Boolean | 否 | 是否检查覆盖度（默认 true） |
| checkFormat | Boolean | 否 | 是否检查格式（默认 true） |
| checkStarred | Boolean | 否 | 是否检查星号条款（默认 true） |

**响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "reportId": "RPT-20260324-001",
    "summary": {
      "totalScore": 85.5,
      "coverageRate": 90.2,
      "formatScore": 80.0,
      "eliminationRisk": true,
      "riskLevel": "HIGH"
    },
    "coverage": {
      "totalClauses": 20,
      "matchedClauses": 16,
      "partialClauses": 2,
      "missingClauses": 2,
      "starredMatched": 3,
      "starredMissing": 1
    },
    "issues": [
      {
        "issueId": "ISSUE-001",
        "type": "MISSING_CLAUSE",
        "severity": "CRITICAL",
        "clauseNumber": "3.2.1",
        "title": "项目业绩要求",
        "description": "缺少近三年内完成的同类项目业绩证明",
        "isStarred": true,
        "eliminationRisk": true,
        "suggestion": "请补充以下业绩材料：\\n1. 项目合同首页和验收报告\\n2. 项目金额证明\\n3. 业主联系方式",
        "contextText": "【招标要求】3.2.1 投标人须提供近三年内..."
      }
    ],
    "format": {
      "completeness": { "score": 90, "issues": [] },
      "signatures": {
        "score": 60,
        "issues": [
          { "type": "LEGAL_SIGNATURE", "page": 1, "status": "MISSING" }
        ]
      },
      "dates": { "score": 100, "issues": [] }
    },
    "visualization": {
      "coverageChart": {
        "labels": ["完全匹配", "部分匹配", "缺失"],
        "data": [16, 2, 2]
      },
      "riskGauge": {
        "value": 85.5,
        "min": 0,
        "max": 100,
        "thresholds": [
          { "from": 0, "to": 60, "color": "red" },
          { "from": 60, "to": 80, "color": "orange" },
          { "from": 80, "to": 100, "color": "green" }
        ]
      }
    }
  }
}
```

### 4.2 报告查询接口

```
GET /api/v1/bidding/report/{reportId}
```

---

## 五、数据模型

### 5.1 实体设计

#### BiddingDocument（投标文档）

```java
@Entity
@Table(name = "bidding_documents")
public class BiddingDocument {
    @Id
    private String id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileType;  // PDF, DOCX

    @Column(name = "file_path")
    private String filePath;  // MinIO 存储路径

    private Long fileSize;

    // 文档解析后的关键信息
    @Column(columnDefinition = "TEXT")
    private String extractedText;

    private Integer totalPages;

    // 解析状态
    @Enumerated(EnumType.STRING)
    private DocumentStatus status;

    private LocalDateTime uploadedAt;
    private LocalDateTime parsedAt;
}
```

#### Clause（条款）

```java
@Entity
@Table(name = "clauses")
public class Clause {
    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "document_id")
    private BiddingDocument document;

    @Column(name = "clause_number")
    private String clauseNumber;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_starred")
    private boolean isStarred;

    @Enumerated(EnumType.STRING)
    private ClauseType type;

    private Integer startPage;
    private Integer endPage;

    @Column(name = "clause_order")
    private Integer order;

    private LocalDateTime createdAt;
}
```

#### BiddingCheckRecord（检查记录）

```java
@Entity
@Table(name = "bidding_check_records")
public class BiddingCheckRecord {
    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "tender_document_id")
    private BiddingDocument tenderDocument;

    @ManyToOne
    @JoinColumn(name = "bidding_document_id")
    private BiddingDocument biddingDocument;

    @Column(name = "report_json", columnDefinition = "TEXT")
    private String reportJson;

    private Double totalScore;
    private Double coverageRate;
    private Double formatScore;

    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    private Boolean eliminationRisk;

    private LocalDateTime createdAt;
}
```

### 5.2 数据库表

```sql
-- 投标文档表
CREATE TABLE bidding_documents (
    id VARCHAR(50) PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(20) NOT NULL,
    file_path VARCHAR(500),
    file_size BIGINT,
    extracted_text TEXT,
    total_pages INT,
    status VARCHAR(20) NOT NULL DEFAULT 'UPLOADED',
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    parsed_at TIMESTAMP
);

-- 条款表
CREATE TABLE clauses (
    id VARCHAR(50) PRIMARY KEY,
    document_id VARCHAR(50) REFERENCES bidding_documents(id),
    clause_number VARCHAR(50),
    title VARCHAR(255),
    content TEXT,
    is_starred BOOLEAN DEFAULT FALSE,
    type VARCHAR(20),
    start_page INT,
    end_page INT,
    clause_order INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 条款向量表（用于语义匹配）
CREATE TABLE clause_embeddings (
    id VARCHAR(50) PRIMARY KEY,
    clause_id VARCHAR(50) REFERENCES clauses(id),
    document_id VARCHAR(50) REFERENCES bidding_documents(id),
    content_vector VECTOR(1536),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 检查记录表
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

-- 索引
CREATE INDEX idx_documents_status ON bidding_documents(status);
CREATE INDEX idx_clauses_document ON clauses(document_id);
CREATE INDEX idx_clauses_number ON clauses(clause_number);
CREATE INDEX idx_clauses_starred ON clauses(is_starred);
CREATE INDEX idx_embeddings_document ON clause_embeddings(document_id);
CREATE INDEX idx_check_records_tender ON bidding_check_records(tender_document_id);
CREATE INDEX idx_check_records_bidding ON bidding_check_records(bidding_document_id);
```

---

## 六、技术实现

### 6.1 依赖

```groovy
dependencies {
    // 文档解析
    implementation 'org.apache.pdfbox:pdfbox:3.0.1'
    implementation 'org.apache.poi:poi-ooxml:5.2.5'

    // 向量数据库
    implementation 'org.postgresql:postgresql'
    implementation 'io.hypersistence:hypersistence-utils-hibernate-63:3.7.0'

    // JSON
    implementation 'com.fasterxml.jackson.core:jackson-databind'
    implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310'

    // MinIO
    implementation 'io.minio:minio:8.5.7'
}
```

### 6.2 核心服务

| 服务类 | 职责 |
|--------|------|
| `DocumentParserService` | 统一文档解析入口 |
| `PdfParserService` | PDF 解析实现 |
| `DocxParserService` | Word 解析实现 |
| `ClauseExtractorService` | 条款提取 |
| `ClauseMatcherService` | 条款匹配（编号 + 语义） |
| `EmbeddingService` | 向量嵌入生成 |
| `FormatCheckerService` | 格式检查 |
| `ReportGeneratorService` | 报告生成 |
| `SuggestionService` | 智能建议生成 |

---

## 七、实施计划

### Phase 1: 基础能力
1. DocumentParser 实现（PDF + Word）
2. ClauseExtractor 实现
3. 基础匹配逻辑（编号匹配）

### Phase 2: 增强能力
4. 语义匹配（pgvector）
5. FormatChecker 实现

### Phase 3: 报告与优化
6. ReportGenerator 实现
7. 可视化数据生成
8. 智能建议优化

---

## 八、验收标准

1. **覆盖度检查** - 条款匹配准确率 ≥ 90%
2. **星号条款** - 100% 识别并标记
3. **格式检查** - 签章检测准确率 ≥ 85%
4. **报告生成** - JSON 响应时间 < 5s
5. **可视化** - 图表数据正确呈现
