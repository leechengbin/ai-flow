# AGENTS.md — AI智能化全流程管理系统

## 项目概述

基于AI的智能化全流程管理系统，覆盖项目立项 → 执行 → 监控 → 结项，融合财务数据管理与智能分析。架构：Spring Boot + 微服务（user/project/finance/ai/audit/document-service），RESTful API 通信，RAG + Function Calling。

---

## 构建 / 测试 / Lint 命令

Maven 多模块结构：

```bash
# 构建全部模块
mvn clean package -DskipTests

# 运行测试
mvn test
mvn test -Dtest=FinanceServiceTest                    # 单测试类
mvn test -Dtest=FinanceServiceTest#testCalculateProfit # 单测试方法
mvn test -pl finance-service                           # 指定模块

# 代码检查与格式化
mvn checkstyle:check
mvn spotbugs:check
mvn spotless:apply

# 启动单个微服务
mvn spring-boot:run -pl finance-service

# Docker
docker compose build && docker compose up -d
```

前端（如有）：
```bash
npm install && npm run lint && npm run build
npm test -- --testPathPattern=Finance
```

---

## 代码风格指南

### 包结构

```
com.company.module
├── controller/   # REST 控制器
├── service/impl/ # 业务实现
├── mapper/       # MyBatis Mapper
├── entity/       # 数据库实体
├── dto/          # 数据传输对象
├── vo/           # 视图对象
├── config/       # 配置类
├── exception/    # 自定义异常
└── util/         # 工具类
```

### 命名规范

| 元素 | 风格 | 示例 |
|------|------|------|
| 类名 | PascalCase | `FinanceService` |
| 方法/变量 | camelCase | `calculateCost()` |
| 常量 | UPPER_SNAKE | `MAX_RETRY_COUNT` |
| 数据库表/列 | snake_case | `project_cost` |
| 枚举类 | PascalCase + Enum | `StatusEnum` |

### 导入顺序

```java
// 1. Java 标准库
import java.util.List;

// 2. 第三方库
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

// 3. 项目内部
import com.company.finance.entity.ProjectCost;
```
禁止 `import *`，每个类单独导入。

### 类型与格式

- `var` 仅限局部变量且类型显而易见时。
- 字符串比较：`"value".equals(variable)`。
- 集合判空：`CollectionUtils.isEmpty(list)`。
- 缩进 4 空格，行宽 120 字符，K&R 大括号。

### 异常处理

- 捕获具体异常，不用 `Exception`。
- 使用 `@ControllerAdvice` 全局异常处理器。
- 自定义异常继承 `BusinessException`，带错误码。
- 日志记录异常栈：`log.error("msg, id={}", id, e)`。
- 禁止循环中 try-catch。

### 日志

- 使用 Lombok `@Slf4j`，禁止 `System.out.println`。
- 格式：`[服务名][TraceID] 操作描述, 参数={}`。
- 级别：DEBUG / INFO / WARN / ERROR。

### Spring Boot 约定

- Controller 返回 `Result<T>` 统一响应体。
- 参数校验：`@Valid` + `@NotNull`/`@NotBlank`。
- 事务注解 `@Transactional` 放在 Service 层。
- 配置值使用 `@Value` 或 `@ConfigurationProperties`。

### AI / Agent 模块

- Agent 调用必须携带 `traceId` 贯穿全链路。
- Function Calling 入参用 POJO，返回用 `FunctionCallResult`。
- 向量查询 Top-K 默认 5，最大 20。
- Token 使用量需记录并计入成本核算。

---

## Git 分支策略（Git Flow）

```
main ← 生产发布
├── develop ← 集成分支
│   ├── feature/* ← 功能开发
│   └── release/* ← 发布准备
└── hotfix/* ← 紧急修复
```

提交信息：`<type>(<scope>): <subject>`
- type: `feat` / `fix` / `refactor` / `docs` / `test` / `chore`

---

## 安全检查清单（提交前）

- [ ] 无硬编码密钥 / 密码
- [ ] SQL 使用参数化查询
- [ ] 接口有权限校验（RBAC）
- [ ] 敏感数据已脱敏
- [ ] 审计日志已记录

---

## 性能基准

| 指标 | 目标 |
|------|------|
| API 响应（P99） | ≤ 2s |
| AI 识别准确率 | ≥ 90% |
| 成本核算 | 天级 → 小时级 |
| 并发用户 | ≥ 100 |
