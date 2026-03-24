# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AI Flow is an enterprise multi-agent AI collaboration system for project management. It orchestrates 5 specialized agents via a routing layer with support for serial/parallel execution patterns.

### 5 Specialized Agents

| Agent | Function |
|-------|----------|
| Project Monitoring Agent | 进度、成本、质量三维分析 |
| Bidding Document Check Agent | 标书合规性审查 |
| Compliance Audit Agent | 合规性审计 |
| Risk Monitoring Agent | 风险预测 |
| Knowledge Base Agent | 知识库问答 |

## Commands

```bash
# Build
./gradlew build                              # Build all services
./gradlew :bidding-service:build             # Build specific service

# Development
./gradlew :bidding-service:bootRun           # Run bidding-service
./gradlew :user-service:bootRun             # Run user-service
./gradlew :ai-service:bootRun              # Run ai-service

# Testing
./gradlew test                               # Run all tests
./gradlew :bidding-service:test             # Run specific service tests
./gradlew test --tests "*BiddingCheckTest"  # Run specific test class

# Code Quality
./gradlew check                              # Run all checks (test, lint, style)
./gradlew spotlessApply                      # Auto-fix code formatting
./gradlew spotlessCheck                      # Check code formatting
./gradlew detekt                             # Kotlin static analysis (if used)

# Database (Flyway)
./gradlew flywayMigrate                      # Run database migrations
./gradlew flywayClean                        # Clean database (dev only!)

# Docker
docker-compose up -d                         # Start infrastructure (PostgreSQL, MinIO)
docker-compose down                           # Stop infrastructure

# Service Ports
# 8080 - Gateway
# 8081 - user-service
# 8082 - project-service
# 8083 - finance-service
# 8084 - ai-service
# 8085 - audit-service
# 8086 - document-service
# 8087 - bidding-service
```

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      客户端层 (Web/移动端)                   │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│              Spring Cloud Gateway (8080)                     │
│              统一入口、路由、限流、鉴权                        │
└────────────────────────────┬────────────────────────────────┘
                             │
       ┌─────────────────────┼─────────────────────┐
       ▼                     ▼                     ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│ user-service  │   │project-service│   │finance-service│
│   (8081)      │   │   (8082)     │   │   (8083)     │
│ ┣ 用户认证    │   │ ┣ 项目管理   │   │ ┣ 成本核算   │
│ ┣ 权限管理   │   │ ┣ 进度跟踪   │   │ ┣ 利润分析   │
│ ┗ 组织架构   │   │ ┗ 审批流程   │   │ ┗ 报表生成   │
└───────────────┘   └───────────────┘   └───────────────┘
       │                     │                     │
       ▼                     ▼                     ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│  ai-service   │   │ audit-service │   │document-service│
│   (8084)      │   │   (8085)     │   │   (8086)     │
│ ┣ RAG检索     │   │ ┣ 规则引擎   │   │ ┣ OCR识别    │
│ ┣ Agent编排   │   │ ┣ 风险预警   │   │ ┣ 文档解析   │
│ ┗ Function    │   │ ┗ 审计报告   │   │ ┗ 版本管理   │
│   Calling      │   │               │   │               │
└───────────────┘   └───────────────┘   └───────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│              数据层 (PostgreSQL + Redis + pgvector)         │
└─────────────────────────────────────────────────────────────┘
```

### Microservices

| Service | Port | Responsibilities |
|---------|------|------------------|
| `gateway` | 8080 | API Gateway, routing, rate limiting, auth |
| `user-service` | 8081 | Authentication, RBAC, organization |
| `project-service` | 8082 | Project lifecycle, progress tracking |
| `finance-service` | 8083 | Cost accounting, profit analysis |
| `ai-service` | 8084 | RAG retrieval, Agent orchestration, Function Calling |
| `audit-service` | 8085 | Compliance rules, risk alerts |
| `document-service` | 8086 | OCR, document parsing, versioning |
| `bidding-service` | 8087 | Bidding check, case retrieval, version diff, suggestions, review workflow |

### AI Layer (ai-service)

```
┌──────────────────────────────────────────────────────────────┐
│                    AI Service (8084)                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │ RAG检索引擎  │  │  Agent编排  │  │Function Calling│        │
│  │ BM25+语义   │  │ 任务路由    │  │ LLM工具调用  │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
└──────────────────────────────────────────────────────────────┘
                             │
                             ▼
┌──────────────────────────────────────────────────────────────┐
│                    5 Specialized Agents                      │
│  Project Monitor │ Bidding Check │ Compliance Audit │        │
│  Risk Monitor │ Knowledge Base                               │
└──────────────────────────────────────────────────────────────┘
```

### Key Design Patterns

| Pattern | Implementation |
|---------|----------------|
| **微服务架构** | Spring Boot 3.x, Nacos registry, RESTful API |
| **RAG** | Hybrid search (BM25 + vector similarity), pgvector/ChromaDB |
| **Function Calling** | LLM → structured tool calls → business APIs |
| **Agent编排** | Serial/Parallel execution, ResultAggregator |

## Agent Collaboration Patterns

| Pattern | Flow | Use Case |
|---------|------|----------|
| **Serial** | Monitor → Risk → Aggregate | 周报生成 |
| **Parallel** | 各自检查 → 聚合 → 评分 | 标书审查 |
| **Single** | 独立处理 | 简单问答 |

## Agent Output Standards

All AI agents must follow these output constraints:

| Constraint | Specification |
|------------|---------------|
| Format | Markdown (tables + lists + headers), UTF-8 |
| Risk levels | [低]/[中]/[高]/[紧急] |
| Health scoring | 0-100 |
| Source attribution | Required for knowledge base agent |
| Legal citations | Required for compliance audit agent |
| Trend notation | ↑ (恶化), ↓ (改善), → (持平) |

## Function Calling Tools

| Tool | Function | Parameters |
|------|----------|------------|
| `get_project_cost` | 获取项目成本 | `project_id`, `cost_type` |
| `get_project_profit` | 获取项目利润 | `project_id` |
| `query_knowledge` | 知识检索 | `query`, `kb_code`, `topK` |
| `check_budget` | 预算检查 | `project_id` |
| `detect_risks` | 风险检测 | `project_id`, `risk_types` |

## Configuration

Environment variables (copy `.env.example` to `.env`):

| Variable | Default | Description |
|----------|---------|-------------|
| `LLM_PROVIDER` | `minimax` | LLM provider: `minimax`, `anthropic`, `openai` |
| `MINIMAX_API_KEY` | - | MiniMax API key |
| `ANTHROPIC_API_KEY` | - | Anthropic API key |
| `OPENAI_API_KEY` | - | OpenAI API key |
| `VECTOR_DB_TYPE` | `chroma` | Vector DB: `chroma` or `qdrant` |
| `RAG_TOP_K` | `5` | Number of similar documents to retrieve |
| `RAG_SIMILARITY_THRESHOLD` | `0.7` | Minimum similarity score |

## Project Structure

```
ai-flow/
├── bidding-service/              # 标书审查服务 (8087)
│   ├── src/main/java/com/aiplatform/bidding/
│   │   ├── controller/           # REST API Controllers
│   │   ├── service/              # Business Logic
│   │   ├── domain/entity/        # JPA Entities
│   │   ├── domain/enums/         # Enums
│   │   ├── repository/           # Data Repositories
│   │   ├── dto/                  # Request/Response DTOs
│   │   ├── config/               # Configuration
│   │   └── exception/             # Exception Handling
│   └── src/main/resources/
│       └── db/migration/         # Flyway Migrations
├── user-service/                  # 用户认证服务 (8081)
├── project-service/              # 项目管理服务 (8082)
├── finance-service/              # 财务服务 (8083)
├── ai-service/                   # AI服务 (8084)
│   └── src/main/resources/
│       └── prompts/v1.0/         # Agent Prompts
├── audit-service/                # 审计服务 (8085)
├── document-service/            # 文档服务 (8086)
├── docs/
│   └── superpowers/              # Design specs and plans
│       ├── specs/
│       └── plans/
├── .claude/
│   ├── skills/                   # Claude Code skills
│   └── agents/                   # Custom agent definitions
├── CLAUDE.md
└── README.md
```
