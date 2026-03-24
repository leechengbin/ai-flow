# 标书审查系统设计文档

> bidding-service 微服务完整设计
> 版本：V1.0
> 架构：Spring Boot + PostgreSQL + pgvector + MinIO
> 编制日期：2026年3月

---

## 一、功能概述

### 1.1 核心能力

| 能力 | 说明 |
|------|------|
| **标书合规性检查** | 对比招标文件与投标文件，返回匹配度评分和具体问题 |
| **历史中标案例检索** | 基于 RAG 语义检索相似历史案例，生成差异化分析 |
| **多版本标书对比** | 对比不同版本差异，支持列表视图和原文对照视图 |
| **智能修改建议生成** | 基于 LLM 生成针对性修改建议，支持条款级/段落级/文档级粒度 |
| **人机协同复核流程** | 状态机驱动的串行复核流程：AI初筛→人工二查→终审 |

### 1.2 用户场景

- **投前自查**：投标前检查标书与招标要求的匹配度，识别问题
- **历史参考**：检索同类项目中标方案，优化当前标书
- **版本追踪**：追踪标书修改历史，确保不遗漏任何变更
- **合规复核**：满足企业/政府招标的合规审核要求

---

## 二、系统架构

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                         ai-service (8084)                       │
│  RAG检索 │ Agent编排 │ Function Calling                         │
│                    ↓ ↑                                          │
│         ┌─────────────────────────────────────┐                 │
│         │  Function Calling: bidding-service   │                 │
│         │  - check_bidding()                  │                 │
│         │  - retrieve_similar_cases()          │                 │
│         │  - compare_versions()               │                 │
│         │  - generate_suggestions()           │                 │
│         │  - track_review()                   │                 │
│         └─────────────────────────────────────┘                 │
└──────────────────────────────┬──────────────────────────────────┘
                               │ REST API
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                    bidding-service (8087)                        │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐│
│  │BiddingController │  │ ReviewController │  │ ClauseController ││
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘│
│           │                    │                    │          │
│  ┌────────┴────────────────────┴────────────────────┴────────┐│
│  │                    StateMachineEngine                      ││
│  │              (轻量级状态机：草稿→AI审核→人工审核→终审)      ││
│  └────────────────────────────────────────────────────────────┘│
│           │                    │                    │          │
│  ┌────────┴────────┐  ┌────────┴────────┐  ┌────────┴────────┐│
│  │ClauseExtractSvc  │  │ CaseRetrievalSvc │  │ VersionDiffSvc  ││
│  │  条款提取解析    │  │  案例检索(RAG)   │  │  版本对比       ││
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘│
│           │                    │                    │          │
│  ┌────────┴────────────────────┴────────────────────┴────────┐│
│  │                    SuggestionService                       ││
│  │              (LLM生成修改建议 + 粒度自适应)                ││
│  └───────────────────────────────────────────────────────────┘│
└──────────────────────────────┬──────────────────────────────────┘
                               │
       ┌───────────────────────┼───────────────────────┐
       ▼                       ▼                       ▼
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ PostgreSQL  │     │   pgvector  │     │   MinIO     │
│  (结构化)   │     │  (向量嵌入)  │     │  (文档存储)  │
└─────────────┘     └─────────────┘     └─────────────┘
```

### 2.2 bidding-service 内部模块

| 模块 | 职责 | 边界 |
|------|------|------|
| `BiddingController` | 标书检查 REST API | 处理 HTTP 请求/响应 |
| `ReviewController` | 复核流程管理 API | 处理状态转换请求 |
| `StateMachineEngine` | 状态转换规则引擎 | 仅处理状态流转逻辑 |
| `ClauseExtractionService` | 条款提取、分类、匹配 | 纯业务逻辑，无外部依赖 |
| `CaseRetrievalService` | RAG 语义检索 + pgvector | 仅负责向量检索 |
| `VersionDiffService` | 多版本 Diff 算法 | 纯文本对比逻辑 |
| `SuggestionService` | LLM 调用生成建议 | 仅负责建议生成 |

---

## 三、复核状态机

### 3.1 状态定义

```java
public enum ReviewState {
    DRAFT,              // 草稿（初始状态）
    AI_REVIEWING,      // AI审查中
    HUMAN_REVIEWING,   // 人工审查中
    REVISION_REQUESTED,// 打回修改
    FINAL_APPROVED,    // 最终通过
    REJECTED           // 驳回（终态）
}
```

### 3.2 状态转换图

```
                     ┌─────────────────────────────────────┐
                     │                                     │
                     ▼                                     │
┌──────────┐   ┌─────────────┐   ┌─────────────────┐   ┌───────────────┐
│  DRAFT   │──►│AI_REVIEWING │──►│HUMAN_REVIEWING  │──►│FINAL_APPROVED │
└──────────┘   └─────────────┘   └────────┬────────┘   └───────────────┘
     ▲              │                     │                    │
     │              │                     │ 有问题              │ 终态
     │              │                     ▼                    │
     │              │              ┌────────────────┐            │
     │              │              │REVISION_REQUESTED          │
     │              │              └────────┬───────┘            │
     │              │                       │                    │
     │              │                    修改后                  │
     │              │                       │                    │
     └──────────────┴───────────────────────┘                    │
     │              (重新提交)                                    │
     │                                                         │
     │                                                         │
     ▼                                                         │
┌──────────┐                                                   │
│ REJECTED │◄──────────────────────────────────────────────────┘
└──────────┘  (终审拒绝)
```

### 3.3 状态转换规则表

| 当前状态 | 事件 | 下一状态 | 触发条件 |
|----------|------|----------|----------|
| DRAFT | submit | AI_REVIEWING | 用户提交审核 |
| AI_REVIEWING | ai_complete | HUMAN_REVIEWING | AI检查完成 |
| HUMAN_REVIEWING | approve | FINAL_APPROVED | 人工通过 |
| HUMAN_REVIEWING | reject | REJECTED | 终审拒绝 |
| HUMAN_REVIEWING | request_revision | REVISION_REQUESTED | 打回修改 |
| REVISION_REQUESTED | resubmit | AI_REVIEWING | 修改后重新提交 |

### 3.4 复核角色

| 角色 | 职责 | 操作权限 |
|------|------|----------|
| 投标撰写人 | 根据建议修改标书 | 创建、编辑、提交审核 |
| 初审人员 | AI初筛结果的复核 | 通过、打回、补充问题 |
| 终审人员 | 最终质量把关 | 终审通过、拒绝 |

---

## 四、核心数据模型

### 4.1 实体关系图

```
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│BiddingDocument │       │    Clause       │       │  ReviewProcess  │
├─────────────────┤       ├─────────────────┤       ├─────────────────┤
│ id (PK)         │──┐   │ id (PK)         │──┐   │ id (PK)         │
│ title           │  │   │ documentId (FK) │  │   │ documentId (FK) │
│ projectId       │  │   │ clauseNumber   │  │   │ currentState    │
│ uploaderId      │  │   │ content        │  │   │ submitterId     │
│ status          │  │   │ isStarred      │  │   │ reviewerId      │
│ currentVersion  │  └──► │ clauseType     │  │   │ deadline        │
│ createdAt       │       │ responseStatus │  └──►│ createdAt       │
└─────────────────┘       └─────────────────┘       └────────┬────────┘
                                                              │
       ┌─────────────────┐       ┌─────────────────┐          │
       │  ClauseIssue    │       │   Suggestion    │          │
       ├─────────────────┤       ├─────────────────┤          │
       │ id (PK)         │       │ id (PK)         │          │
       │ clauseId (FK)   │       │ issueId (FK)    │          │
       │ issueType       │       │ content         │          │
       │ originalText    │       │ granularity     │          │
       │ suggestionText  │       │ status          │          │
       │ severity        │       │ applied         │          │
       └─────────────────┘       └─────────────────┘          │
                                                              │
       ┌─────────────────┐       ┌─────────────────┐          │
       │  BiddingCase    │       │DocumentVersion  │          │
       ├─────────────────┤       ├─────────────────┤          │
       │ id (PK)         │       │ id (PK)         │          │
       │ tenderTitle     │       │ documentId (FK) │          │
       │ industry        │       │ versionNumber   │          │
       │ region          │       │ filePath (MinIO)│          │
       │ winningBidder   │       │ diffFromPrev   │          │
       │ bidAmount       │       │ createdAt       │          │
       │ winningDate     │       └─────────────────┘          │
       │ embedding(pgvector)                                       │
       └─────────────────┘                                      │
                                                              │
       ┌─────────────────┐                                     │
       │StateTransition  │◄────────────────────────────────────┘
       ├─────────────────┤
       │ id (PK)         │
       │ reviewId (FK)   │
       │ fromState       │
       │ toState         │
       │ actor           │
       │ action          │
       │ timestamp       │
       └─────────────────┘
```

### 4.2 核心 Entity 类

```java
// 标书文档
@Entity
@Table(name = "bidding_documents")
public class BiddingDocument {
    @Id
    private String id;                    // DOC-{uuid}
    private String title;                 // 标书标题
    private String projectId;            // 关联项目
    private String uploaderId;           // 上传人
    @Enumerated(EnumType.STRING)
    private DocumentStatus status;        // DRAFT/SUBMITTED/APPROVED/REJECTED
    private String currentVersion;       // 当前版本号 v1.0
    private String tenderFileId;         // 招标文件ID（可选）
    @Column(columnDefinition = "text")
    private String tenderRequirements;   // 招标文件内容（全文）
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// 条款实体
@Entity
@Table(name = "clauses")
public class Clause {
    @Id
    private String id;                   // CLAUSE-{uuid}
    private String documentId;           // 关联文档
    private String clauseNumber;         // 条款编号 3.1
    @Column(columnDefinition = "text")
    private String content;              // 条款内容
    private Boolean isStarred;           // 是否为星号条款 ★
    @Enumerated(EnumType.STRING)
    private ClauseType clauseType;      // QUALIFICATION/TECHNICAL/COMMERCIAL/OTHER
    @Enumerated(EnumType.STRING)
    private ResponseStatus responseStatus;// FULL/PARTIAL/NONE
    private Integer pageNumber;          // 页码
}

// 条款问题
@Entity
@Table(name = "clause_issues")
public class ClauseIssue {
    @Id
    private String id;                   // ISSUE-{uuid}
    private String clauseId;             // 关联条款
    private String clauseNumber;         // 条款编号（冗余便于查询）
    @Enumerated(EnumType.STRING)
    private IssueType issueType;         // MISSING/INCOMPLETE/NON_COMPLIANT/FORMAT
    @Column(columnDefinition = "text")
    private String originalText;         // 问题原文
    @Column(columnDefinition = "text")
    private String requirementText;      // 招标要求原文
    @Column(columnDefinition = "text")
    private String suggestionText;       // 修改建议
    @Enumerated(EnumType.STRING)
    private Severity severity;           // LOW/MEDIUM/HIGH/CRITICAL
    private Boolean eliminationRisk;     // 是否为废标风险
}

// 历史中标案例
@Entity
@Table(name = "bidding_cases")
public class BiddingCase {
    @Id
    private String id;                   // CASE-{uuid}
    private String tenderTitle;           // 招标项目名
    private String industry;              // 行业
    private String region;                // 区域
    private String winningBidder;         // 中标方
    @Column(precision = 15, scale = 2)
    private BigDecimal bidAmount;        // 中标金额
    private LocalDate winningDate;        // 中标日期
    private String tenderFilePath;       // 招标文件存储路径
    // pgvector 存储向量嵌入
    @Column(columnDefinition = "vector(1536)")
    private String embedding;
    private LocalDateTime createdAt;
}

// 文档版本
@Entity
@Table(name = "document_versions")
public class DocumentVersion {
    @Id
    private String id;                   // VER-{uuid}
    private String documentId;           // 关联文档
    private String versionNumber;         // 版本号 v1.0, v2.1
    private String filePath;             // MinIO 存储路径
    private String diffFromPrevious;     // 与上一版本的差异摘要
    private String createdBy;            // 创建人
    private LocalDateTime createdAt;
}

// 复核流程
@Entity
@Table(name = "review_processes")
public class ReviewProcess {
    @Id
    private String id;                   // REV-{uuid}
    private String documentId;           // 关联标书
    @Enumerated(EnumType.STRING)
    private ReviewState currentState;
    private String submitterId;          // 提交人
    private String reviewerId;           // 当前复核人
    private LocalDateTime deadline;      // 截止时间
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// 状态转换历史
@Entity
@Table(name = "state_transitions")
public class StateTransition {
    @Id
    private String id;                   // TRANS-{uuid}
    private String reviewId;             // 关联复核流程
    @Enumerated(EnumType.STRING)
    private ReviewState fromState;
    @Enumerated(EnumType.STRING)
    private ReviewState toState;
    private String actor;                // 执行者（userId 或 system/ai-agent）
    private String action;               // 操作描述
    @Column(columnDefinition = "text")
    private String comment;             // 备注/意见
    private LocalDateTime timestamp;
}

// 修改建议
@Entity
@Table(name = "suggestions")
public class Suggestion {
    @Id
    private String id;                   // SUG-{uuid}
    private String issueId;              // 关联问题ID
    private String documentId;            // 关联文档
    @Enumerated(EnumType.STRING)
    private Granularity granularity;     // CLAUSE/PARAGRAPH/DOCUMENT
    @Column(columnDefinition = "text")
    private String originalContent;     // 原文内容
    @Column(columnDefinition = "text")
    private String suggestedContent;     // 建议内容
    @Column(columnDefinition = "text")
    private String explanation;          // 生成说明
    @Enumerated(EnumType.STRING)
    private SuggestionStatus status;     // PENDING/APPLIED/REJECTED
    private String generatedBy;          // 生成模型
    private LocalDateTime createdAt;
}
```

### 4.3 枚举定义

```java
// 条款类型
public enum ClauseType {
    QUALIFICATION,  // 资格性条款
    TECHNICAL,     // 技术条款
    COMMERCIAL,    // 商务条款（工期、报价、质保）
    OTHER          // 其他
}

// 问题类型
public enum IssueType {
    MISSING,       // 缺失
    INCOMPLETE,    // 不完整
    NON_COMPLIANT, // 不合规
    FORMAT         // 格式错误
}

// 严重程度
public enum Severity {
    LOW,          // 低
    MEDIUM,       // 中
    HIGH,         // 高
    CRITICAL      // 紧急（废标风险）
}

// 建议粒度
public enum Granularity {
    CLAUSE,       // 条款级
    PARAGRAPH,    // 段落级
    DOCUMENT      // 文档级
}

// 建议状态
public enum SuggestionStatus {
    PENDING,      // 待确认
    APPLIED,      // 已应用
    REJECTED      // 已拒绝
}
```

---

## 五、API 接口设计

### 5.1 标书检查 API

```yaml
# 标书合规性检查
POST /api/v1/bidding/check
Content-Type: application/json

Request:
{
  "documentId": "DOC-xxx",              # 投标文档ID
  "tenderRequirements": "招标文件全文..."  # 或通过 tenderFileId 引用
}

Response (200 OK):
{
  "code": 200,
  "message": "success",
  "data": {
    "checkId": "CHECK-xxx",
    "documentId": "DOC-xxx",
    "totalClauses": 10,
    "matchedClauses": 7,
    "partiallyMatched": 2,
    "unmatched": 1,
    "score": 85.0,
    "issues": [
      {
        "issueId": "ISSUE-001",
        "clauseNumber": "3.1",
        "issueType": "INCOMPLETE",
        "originalText": "投标人提供一年质保期",
        "requirementText": "★质保期≥2年",
        "suggestionText": "建议修改为：投标人提供两年质保期服务...",
        "severity": "HIGH",
        "eliminationRisk": true
      }
    ],
    "eliminationRisk": true,
    "riskReasons": ["资质证书过期"],
    "checkedAt": "2026-03-23T10:00:00Z"
  }
}
```

### 5.2 历史案例检索 API

```yaml
# 检索相似历史中标案例
POST /api/v1/cases/retrieve
Content-Type: application/json

Request:
{
  "query": "政府信息化系统建设",         # 语义检索query
  "industry": "政府信息化",               # 可选过滤
  "region": "华东",                      # 可选过滤
  "dateFrom": "2025-01-01",             # 可选过滤
  "dateTo": "2025-12-31",               # 可选过滤
  "topK": 5
}

Response (200 OK):
{
  "code": 200,
  "message": "success",
  "data": {
    "cases": [
      {
        "caseId": "CASE-001",
        "tenderTitle": "XX区政务云平台建设",
        "industry": "政府信息化",
        "region": "华东",
        "winningBidder": "XX科技公司",
        "bidAmount": 5000000.00,
        "winningDate": "2025-03-15",
        "similarityScore": 0.85
      }
    ],
    "total": 5
  }
}
```

### 5.3 版本对比 API

```yaml
# 多版本标书对比
POST /api/v1/diff/compare
Content-Type: application/json

Request:
{
  "documentId": "DOC-xxx",
  "versionA": "v1.0",
  "versionB": "v2.1"
}

Response (200 OK):
{
  "code": 200,
  "message": "success",
  "data": {
    "documentId": "DOC-xxx",
    "versionA": "v1.0",
    "versionB": "v2.1",
    "summary": {
      "totalChanges": 15,
      "added": 5,
      "modified": 8,
      "deleted": 2
    },
    "details": [
      {
        "clauseNumber": "3.1",
        "changeType": "MODIFIED",
        "oldContent": "质保期：1年",
        "newContent": "质保期：2年",
        "pageNumber": 5
      }
    ],
    "viewType": "list"  # list / side_by_side
  }
}
```

### 5.4 建议生成 API

```yaml
# 生成修改建议
POST /api/v1/suggestions/generate
Content-Type: application/json

Request:
{
  "documentId": "DOC-xxx",
  "issueIds": ["ISSUE-001", "ISSUE-002"]
}

Response (200 OK):
{
  "code": 200,
  "message": "success",
  "data": {
    "suggestions": [
      {
        "suggestionId": "SUG-001",
        "issueId": "ISSUE-001",
        "granularity": "CLAUSE",
        "originalContent": "投标人提供一年质保期",
        "suggestedContent": "投标人提供两年质保期服务，并提供免费上门维护。",
        "explanation": "根据招标要求★条款，质保期需≥2年。建议提升至2年以提高竞争力。"
      }
    ],
    "generatedBy": "minimax",
    "generatedAt": "2026-03-23T10:00:00Z"
  }
}
```

### 5.5 复核流程 API

```yaml
# 提交标书审核
POST /api/v1/review/{documentId}/submit
Content-Type: application/json

Request:
{
  "reviewerId": "user-xxx",
  "deadline": "2026-03-25T17:00:00Z"
}

Response (200 OK):
{
  "code": 200,
  "message": "success",
  "data": {
    "reviewId": "REV-xxx",
    "documentId": "DOC-xxx",
    "state": "AI_REVIEWING",
    "message": "标书已提交，AI审查中..."
  }
}

---

# 获取复核状态
GET /api/v1/review/{reviewId}/status

Response (200 OK):
{
  "code": 200,
  "message": "success",
  "data": {
    "reviewId": "REV-xxx",
    "documentId": "DOC-xxx",
    "currentState": "HUMAN_REVIEWING",
    "submitterId": "user-xxx",
    "reviewerId": "reviewer-xxx",
    "deadline": "2026-03-25T17:00:00Z",
    "history": [
      {
        "fromState": null,
        "toState": "DRAFT",
        "actor": "system",
        "action": "创建",
        "timestamp": "2026-03-20T09:00:00Z"
      },
      {
        "fromState": "DRAFT",
        "toState": "AI_REVIEWING",
        "actor": "system",
        "action": "提交审核",
        "timestamp": "2026-03-20T09:05:00Z"
      },
      {
        "fromState": "AI_REVIEWING",
        "toState": "HUMAN_REVIEWING",
        "actor": "ai-agent",
        "action": "AI审查完成",
        "timestamp": "2026-03-20T09:10:00Z"
      }
    ],
    "pendingIssues": ["ISSUE-001"],
    "approvedIssues": ["ISSUE-002"],
    "rejectedIssues": []
  }
}

---

# 人工复核操作
POST /api/v1/review/{reviewId}/human-action
Content-Type: application/json

Request:
{
  "action": "APPROVE",       # APPROVE / REJECT / REQUEST_REVISION
  "comment": "条款3.1需修改后重新提交",
  "approvedIssueIds": ["ISSUE-002"],
  "rejectedIssueIds": [],
  "issueIdsToRequestRevision": ["ISSUE-001"]
}

Response (200 OK):
{
  "code": 200,
  "message": "success",
  "data": {
    "reviewId": "REV-xxx",
    "previousState": "HUMAN_REVIEWING",
    "currentState": "REVISION_REQUESTED",
    "message": "已打回修改，等待提交人重新提交"
  }
}
```

---

## 六、Function Calling 接口定义

### 6.1 ai-service 调用 bidding-service

```json
{
  "name": "bidding_service",
  "description": "标书检查相关能力：合规性检查、历史案例检索、版本对比、智能建议、复核追踪",
  "parameters": {
    "type": "object",
    "properties": {
      "action": {
        "type": "string",
        "enum": [
          "check_bidding",
          "retrieve_similar_cases",
          "compare_versions",
          "generate_suggestions",
          "track_review"
        ],
        "description": "执行的标书相关操作"
      },
      "document_id": {
        "type": "string",
        "description": "标书文档ID"
      },
      "tender_requirements": {
        "type": "string",
        "description": "招标文件内容（当 action=check_bidding 时）"
      },
      "query": {
        "type": "string",
        "description": "语义检索query（当 action=retrieve_similar_cases 时）"
      },
      "industry": {
        "type": "string",
        "description": "行业过滤（可选）"
      },
      "version_a": {
        "type": "string",
        "description": "版本A（当 action=compare_versions 时）"
      },
      "version_b": {
        "type": "string",
        "description": "版本B（当 action=compare_versions 时）"
      },
      "issue_ids": {
        "type": "array",
        "items": {"type": "string"},
        "description": "问题ID列表（当 action=generate_suggestions 时）"
      }
    },
    "required": ["action", "document_id"]
  }
}
```

### 6.2 Action 对应关系

| Action | HTTP Method | Endpoint |
|--------|-------------|----------|
| check_bidding | POST | /api/v1/bidding/check |
| retrieve_similar_cases | POST | /api/v1/cases/retrieve |
| compare_versions | POST | /api/v1/diff/compare |
| generate_suggestions | POST | /api/v1/suggestions/generate |
| track_review | GET | /api/v1/review/{reviewId}/status |

---

## 七、AI Agent 与 bidding-service 交互流程

### 7.1 典型对话流程

```
用户: "帮我检查这份标书，找出和招标要求不符的地方"

           │
           ▼
┌──────────────────────────────────────────────────────────────────┐
│                      ai-service (8084)                            │
│                                                                   │
│  1. 意图识别                                                       │
│     → intent = "bidding_check"                                    │
│                                                                   │
│  2. 解析参数                                                       │
│     → documentId = "DOC-xxx"                                      │
│     → tenderRequirements = 用户上传的招标文件                       │
│                                                                   │
│  3. Function Calling                                               │
│     → POST /api/v1/bidding/check                                  │
│     → { documentId, tenderRequirements }                           │
│                                                                   │
│  4. 接收结果                                                       │
│     ← { score: 85, issues: [...], eliminationRisk: true }        │
│                                                                   │
│  5. 判断严重程度                                                   │
│     → 发现 HIGH/CRITICAL 问题                                       │
│     → 展示问题详情                                                  │
│     → 询问用户："是否需要我生成修改建议？"                           │
│                                                                   │
│  6. 用户确认                                                       │
│     → "生成建议"                                                   │
│                                                                   │
│  7. 再次 Function Calling                                          │
│     → POST /api/v1/suggestions/generate                           │
│     → { documentId, issueIds: [...] }                              │
│                                                                   │
│  8. 组装最终回复                                                   │
│     → Markdown 格式展示检查报告 + 建议                              │
└──────────────────────────────────────────────────────────────────┘
```

### 7.2 AI Agent Prompt 片段

```markdown
# Role
你是一名招投标审核专家，负责调用 bidding-service 检查标书合规性。

# Tools (Function Calling)
- check_bidding: 检查标书与招标要求的匹配度
- retrieve_similar_cases: 检索相似历史中标案例
- compare_versions: 对比不同版本差异
- generate_suggestions: 生成修改建议
- track_review: 追踪复核状态

# Workflow
1. 解析用户请求中的 documentId
2. 如果用户提供了招标文件内容，调用 check_bidding
3. 分析返回结果：
   - score < 70：标书存在严重问题
   - eliminationRisk = true：存在废标风险
   - issues 中有 severity = HIGH/CRITICAL：需要立即修改
4. 如果发现高危问题，询问用户是否需要生成修改建议
5. 调用 generate_suggestions 生成建议
6. 汇总所有信息，生成完整的审查报告（Markdown格式）

# Output Format
必须使用 Markdown 表格展示问题列表，风险等级使用 [低]/[中]/[高]/[紧急]
```

---

## 八、错误处理与降级策略

| 场景 | HTTP Status | 错误码 | 处理策略 |
|------|-------------|--------|----------|
| bidding-service 不可用 | 503 | SERVICE_UNAVAILABLE | 返回"标书检查服务暂时不可用" |
| 文档不存在 | 404 | DOCUMENT_NOT_FOUND | 返回具体文档ID |
| 文档解析失败 | 422 | PARSE_ERROR | 返回解析错误详情，提示检查文件格式 |
| 案例库为空 | 200 | - | 返回空列表，提示"暂无相似案例" |
| 版本对比无数据 | 200 | - | 返回空，提示"暂无版本对比数据" |
| LLM 生成建议失败 | 500 | SUGGESTION_ERROR | 降级为模板填充 |
| 复核超时 | 200 | - | 自动提醒复核人，超时3天升级通知 |

### 8.1 统一错误响应格式

```json
{
  "code": 404,
  "message": "DOCUMENT_NOT_FOUND",
  "data": null,
  "error": {
    "detail": "标书文档 DOC-xxx 不存在",
    "documentId": "DOC-xxx",
    "timestamp": "2026-03-23T10:00:00Z"
  }
}
```

---

## 九、技术栈

### 9.1 bidding-service 依赖

```groovy
// Spring Boot 3.x
implementation 'org.springframework.boot:spring-boot-starter-web'
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
implementation 'org.springframework.boot:spring-boot-starter-validation'

// 数据库
runtimeOnly 'org.postgresql:postgresql'
implementation 'io.hypersistence:hypersistence-utils-hibernate-63:3.7.0'  // pgvector 支持

// 文档处理
implementation 'org.apache.poi:poi-ooxml:5.2.5'  // Word
implementation 'org.apache.pdfbox:pdfbox:3.0.1'   // PDF

// AI 调用
implementation 'org.springframework.cloud:spring-cloud-starter-openfeign:4.1.0'

// 存储
implementation 'io.minio:minio:8.5.7'

// 测试
testImplementation 'org.springframework.boot:spring-boot-starter-test'
testImplementation 'org.testcontainers:junit-jupiter:1.19.5'
testImplementation 'org.testcontainers:postgresql:1.19.5'
```

### 9.2 数据库选型

| 组件 | 选型 | 原因 |
|------|------|------|
| 关系数据库 | PostgreSQL 15+ | 结构化数据存储，pgvector 原生支持向量 |
| 向量数据库 | pgvector (PostgreSQL 扩展) | 混合检索 (BM25 + 向量)，一体化方案 |
| 文件存储 | MinIO | S3 兼容协议，私有化部署方便 |

---

## 十、项目结构

```
bidding-service/
├── src/main/java/com/aiplatform/bidding/
│   ├── BiddingServiceApplication.java
│   │
│   ├── controller/
│   │   ├── BiddingController.java      # 标书检查 API
│   │   ├── CaseController.java         # 案例检索 API
│   │   ├── DiffController.java         # 版本对比 API
│   │   ├── SuggestionController.java   # 建议生成 API
│   │   └── ReviewController.java       # 复核流程 API
│   │
│   ├── service/
│   │   ├── BiddingCheckService.java    # 标书检查业务逻辑
│   │   ├── ClauseExtractionService.java # 条款提取
│   │   ├── CaseRetrievalService.java   # RAG 案例检索
│   │   ├── VersionDiffService.java      # 版本对比
│   │   ├── SuggestionService.java       # 建议生成
│   │   ├── ReviewService.java          # 复核流程
│   │   └── StateMachineEngine.java      # 状态机引擎
│   │
│   ├── domain/
│   │   ├── entity/                     # JPA Entity
│   │   │   ├── BiddingDocument.java
│   │   │   ├── Clause.java
│   │   │   ├── ClauseIssue.java
│   │   │   ├── BiddingCase.java
│   │   │   ├── DocumentVersion.java
│   │   │   ├── ReviewProcess.java
│   │   │   ├── StateTransition.java
│   │   │   └── Suggestion.java
│   │   │
│   │   └── enums/
│   │       ├── ReviewState.java
│   │       ├── ClauseType.java
│   │       ├── IssueType.java
│   │       ├── Severity.java
│   │       ├── Granularity.java
│   │       └── SuggestionStatus.java
│   │
│   ├── repository/
│   │   ├── BiddingDocumentRepository.java
│   │   ├── ClauseRepository.java
│   │   ├── ClauseIssueRepository.java
│   │   ├── BiddingCaseRepository.java
│   │   ├── DocumentVersionRepository.java
│   │   ├── ReviewProcessRepository.java
│   │   ├── StateTransitionRepository.java
│   │   └── SuggestionRepository.java
│   │
│   ├── dto/
│   │   ├── request/                     # 请求 DTO
│   │   └── response/                    # 响应 DTO
│   │
│   ├── config/
│   │   ├── JpaConfig.java
│   │   ├── MinioConfig.java
│   │   └── FeignConfig.java
│   │
│   └── exception/
│       ├── GlobalExceptionHandler.java
│       ├── DocumentNotFoundException.java
│       └── ServiceUnavailableException.java
│
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/                    # Flyway 迁移脚本
│       └── V1__init_schema.sql
│
├── src/test/java/
│   └── com/aiplatform/bidding/
│       ├── service/
│       │   ├── StateMachineEngineTest.java
│       │   ├── ClauseExtractionServiceTest.java
│       │   └── VersionDiffServiceTest.java
│       └── controller/
│           └── BiddingControllerTest.java
│
├── build.gradle
└── README.md
```

---

## 十一、数据库表结构

```sql
-- 标书文档表
CREATE TABLE bidding_documents (
    id VARCHAR(50) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    project_id VARCHAR(50),
    uploader_id VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    current_version VARCHAR(10),
    tender_requirements TEXT,
    tender_file_id VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 条款表
CREATE TABLE clauses (
    id VARCHAR(50) PRIMARY KEY,
    document_id VARCHAR(50) NOT NULL REFERENCES bidding_documents(id),
    clause_number VARCHAR(20),
    content TEXT NOT NULL,
    is_starred BOOLEAN DEFAULT FALSE,
    clause_type VARCHAR(20),
    response_status VARCHAR(20),
    page_number INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 条款问题表
CREATE TABLE clause_issues (
    id VARCHAR(50) PRIMARY KEY,
    clause_id VARCHAR(50) REFERENCES clauses(id),
    clause_number VARCHAR(20),
    issue_type VARCHAR(20) NOT NULL,
    original_text TEXT,
    requirement_text TEXT,
    suggestion_text TEXT,
    severity VARCHAR(20) NOT NULL,
    elimination_risk BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 历史中标案例表
CREATE TABLE bidding_cases (
    id VARCHAR(50) PRIMARY KEY,
    tender_title VARCHAR(255) NOT NULL,
    industry VARCHAR(50),
    region VARCHAR(50),
    winning_bidder VARCHAR(255),
    bid_amount DECIMAL(15,2),
    winning_date DATE,
    tender_file_path VARCHAR(500),
    embedding VECTOR(1536),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 文档版本表
CREATE TABLE document_versions (
    id VARCHAR(50) PRIMARY KEY,
    document_id VARCHAR(50) NOT NULL REFERENCES bidding_documents(id),
    version_number VARCHAR(10) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    diff_from_previous TEXT,
    created_by VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 复核流程表
CREATE TABLE review_processes (
    id VARCHAR(50) PRIMARY KEY,
    document_id VARCHAR(50) NOT NULL REFERENCES bidding_documents(id),
    current_state VARCHAR(30) NOT NULL,
    submitter_id VARCHAR(50) NOT NULL,
    reviewer_id VARCHAR(50),
    deadline TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 状态转换历史表
CREATE TABLE state_transitions (
    id VARCHAR(50) PRIMARY KEY,
    review_id VARCHAR(50) NOT NULL REFERENCES review_processes(id),
    from_state VARCHAR(30),
    to_state VARCHAR(30) NOT NULL,
    actor VARCHAR(50) NOT NULL,
    action VARCHAR(100),
    comment TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 修改建议表
CREATE TABLE suggestions (
    id VARCHAR(50) PRIMARY KEY,
    issue_id VARCHAR(50) REFERENCES clause_issues(id),
    document_id VARCHAR(50) NOT NULL REFERENCES bidding_documents(id),
    granularity VARCHAR(20) NOT NULL,
    original_content TEXT,
    suggested_content TEXT,
    explanation TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    generated_by VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_clauses_document_id ON clauses(document_id);
CREATE INDEX idx_clause_issues_clause_id ON clause_issues(clause_id);
CREATE INDEX idx_bidding_cases_industry ON bidding_cases(industry);
CREATE INDEX idx_review_processes_document_id ON review_processes(document_id);
CREATE INDEX idx_state_transitions_review_id ON state_transitions(review_id);
CREATE INDEX idx_suggestions_document_id ON suggestions(document_id);
```

---

## 十二、架构决策记录

| 决策项 | 选择 | 理由 |
|--------|------|------|
| 服务归属 | 独立 bidding-service (8087) | 独立部署，便于扩展和维护 |
| 存储方案 | PostgreSQL + pgvector + MinIO | 一体化方案，减少系统复杂度 |
| 复核模式 | 串行模式 | 符合合规审核要求，流程清晰 |
| 核心能力 | 完整能力 | 充分利用 AI + RAG 优势 |
| 状态机 | 轻量级自定义状态机 | 流程固定，不需要 BPMN 复杂度 |
| AI 集成 | Function Calling | 通过 ai-service 调用 |

---

## 十三、后续扩展

| 功能 | 描述 | 优先级 |
|------|------|--------|
| 增量检查 | 只检查本次修改的章节 | P2 |
| 多语言支持 | 中/英/其他语言标书混审 | P2 |
| 竞品分析 | 基于外部数据分析竞争对手 | P3 |
| 自动排版 | 标书格式自动规范化 | P3 |
