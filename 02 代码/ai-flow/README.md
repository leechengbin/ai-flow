# AI Flow

> 基于 Spring Boot 微服务架构的企业级多 Agent AI 协作系统

[![Java Version](https://img.shields.io/badge/Java-21-blue.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 系统概述

AI Flow 是一款面向企业级项目管理的智能协作平台，通过 AI Agent 技术实现项目全生命周期的智能化管理。

### 核心能力

| Agent | 功能 | 说明 |
|-------|------|------|
| **项目监控 Agent** | 进度、成本、质量三维分析 | 实时监控项目健康度，预警偏差 |
| **标书审查 Agent** | 标书合规性审查 | RAG 检索 + 智能检查 |
| **合规审计 Agent** | 合规性审计 | 规则引擎 + 风险预警 |
| **风险监控 Agent** | 风险预测 | Chain of Thought 分析 |
| **知识库 Agent** | 知识库问答 | 混合检索 + 引用溯源 |

### 技术架构

```
Spring Cloud Gateway (8080)
         │
         ├── user-service (8081)       # 用户认证、权限、组织架构
         ├── project-service (8082)     # 项目管理、进度跟踪
         ├── finance-service (8083)     # 成本核算、利润分析
         ├── ai-service (8084)          # RAG 检索、Agent 编排
         ├── audit-service (8085)        # 合规规则、风险预警
         ├── document-service (8086)     # OCR 识别、文档解析
         └── bidding-service (8087)      # 标书审查、案例检索

数据层：PostgreSQL + pgvector + Redis + MinIO
```

---

## 快速开始

### 环境要求

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 21+ | 推荐 Adoptium Temurin |
| Gradle | 8.x | Java 构建工具 |
| Docker | 24.x | 本地开发容器 |

### 1. 克隆项目

```bash
git clone <repository-url>
cd ai-flow
```

### 2. 启动基础设施

```bash
docker-compose up -d
```

这将启动：
- PostgreSQL (5432) - 主数据库
- Redis (6379) - 缓存
- MinIO (9000) - 对象存储

### 3. 构建项目

```bash
./gradlew build
```

### 4. 启动服务

```bash
# 启动所有服务（开发模式）
./gradlew bootRun

# 或启动单个服务
./gradlew :bidding-service:bootRun
```

### 5. 验证服务

```bash
# 检查服务健康状态
curl http://localhost:8087/api/v1/health

# 访问 API 文档
# - Gateway: http://localhost:8080
# - bidding-service: http://localhost:8087/swagger-ui.html
```

---

## 微服务说明

### bidding-service (8087)

标书审查核心服务，提供以下能力：

| 能力 | API 端点 | 说明 |
|------|----------|------|
| 标书合规性检查 | `POST /api/v1/bidding/check` | 对比招标文件，返回匹配度评分 |
| 历史案例检索 | `POST /api/v1/cases/retrieve` | RAG 语义检索相似中标案例 |
| 多版本对比 | `POST /api/v1/diff/compare` | 对比不同版本差异 |
| 智能建议生成 | `POST /api/v1/suggestions/generate` | LLM 生成修改建议 |
| 复核流程 | `POST /api/v1/review/{docId}/submit` | 人机协同复核状态机 |

**技术栈：**
- Spring Boot 3.2
- Spring Data JPA + Hibernate
- PostgreSQL 15 + pgvector (向量检索)
- MinIO (文档存储)
- Apache POI + PDFBox (文档解析)

### ai-service (8084)

AI 能力服务，通过 Function Calling 调用各业务服务：

- **RAG 检索引擎**：Hybrid Search (BM25 + 向量)
- **Agent 编排**：串行/并行任务调度
- **Function Calling**：统一工具调用接口

---

## 项目结构

```
ai-flow/
├── bidding-service/              # 标书审查服务
│   └── src/main/java/com/aiplatform/bidding/
│       ├── controller/           # REST API
│       ├── service/              # 业务逻辑
│       ├── domain/entity/        # JPA 实体
│       ├── domain/enums/         # 枚举类型
│       ├── repository/           # 数据访问层
│       ├── dto/                  # 数据传输对象
│       └── config/               # 配置类
│
├── ai-service/                   # AI 能力服务
├── user-service/                # 用户服务
├── project-service/             # 项目服务
├── finance-service/             # 财务服务
├── audit-service/               # 审计服务
├── document-service/            # 文档服务
│
├── docs/                        # 设计文档
│   └── superpowers/
│       ├── specs/               # 设计规格
│       └── plans/               # 实现计划
│
├── docker-compose.yml            # 基础设施编排
├── CLAUDE.md                    # Claude Code 指南
└── README.md                    # 本文件
```

---

## API 文档

### 标书检查

```bash
# 提交标书检查
curl -X POST http://localhost:8087/api/v1/bidding/check \
  -H "Content-Type: application/json" \
  -d '{
    "documentId": "DOC-xxx",
    "tenderRequirements": "招标文件内容..."
  }'
```

### 检索相似案例

```bash
# 检索历史中标案例
curl -X POST http://localhost:8087/api/v1/cases/retrieve \
  -H "Content-Type: application/json" \
  -d '{
    "query": "政府信息化系统建设",
    "industry": "政府信息化",
    "topK": 5
  }'
```

### 提交复核

```bash
# 提交标书复核
curl -X POST http://localhost:8087/api/v1/review/DOC-xxx/submit \
  -H "Content-Type: application/json" \
  -d '{
    "reviewerId": "user-xxx",
    "deadline": "2026-03-25T17:00:00Z"
  }'
```

---

## 设计文档

| 文档 | 位置 | 说明 |
|------|------|------|
| 标书审查系统设计 | [docs/superpowers/specs/2026-03-23-bidding-service-design.md](docs/superpowers/specs/2026-03-23-bidding-service-design.md) | bidding-service 完整设计 |
| 标书自动建议 | [docs/superpowers/specs/2026-03-23-bidding-check-auto-suggestion-design.md](docs/superpowers/specs/2026-03-23-bidding-check-auto-suggestion-design.md) | 智能建议生成设计 |
| 历史案例检索 | [docs/superpowers/specs/2026-03-23-bidding-check-history-case-retrieval-design.md](docs/superpowers/specs/2026-03-23-bidding-check-history-case-retrieval-design.md) | RAG 案例检索设计 |
| OA 集成设计 | [docs/superpowers/specs/2026-03-22-oa-integration-design.md](docs/superpowers/specs/2026-03-22-oa-integration-design.md) | OA 系统集成方案 |

---

## 开发指南

### 代码规范

```bash
# 代码格式化
./gradlew spotlessApply

# 静态检查
./gradlew detekt

# 运行测试
./gradlew test
```

### 数据库迁移

```bash
# 执行迁移
./gradlew flywayMigrate

# 回滚（开发环境）
./gradlew flywayClean
```

### 测试

```bash
# 运行所有测试
./gradlew test

# 运行单个服务测试
./gradlew :bidding-service:test

# 生成覆盖率报告
./gradlew jacocoTestReport
```

---

## 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'feat: add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

---

## 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件
