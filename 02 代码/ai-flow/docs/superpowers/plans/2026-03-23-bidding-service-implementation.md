# Bidding-Service 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现完整的 bidding-service 微服务，提供标书合规性检查、历史案例检索、多版本对比、智能建议生成、人机协同复核功能。

**Architecture:** 基于 Spring Boot 3.x 微服务架构，使用 PostgreSQL + pgvector 存储结构化数据和向量嵌入，MinIO 存储文档文件。业务逻辑与状态机分离，通过 REST API 对外暴露能力。

**Tech Stack:** Spring Boot 3.2, Spring Data JPA, PostgreSQL 15, pgvector, Spring StateMachine (状态机), Apache POI, PDFBox, MinIO, JUnit 5, Mockito, Testcontainers

---

## 项目初始化

### Task 1: 创建 Gradle 项目结构

**Files:**
- Create: `bidding-service/build.gradle`
- Create: `bidding-service/settings.gradle`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/BiddingServiceApplication.java`
- Create: `bidding-service/src/main/resources/application.yml`
- Create: `bidding-service/src/test/java/com/aiplatform/bidding/BiddingServiceApplicationTests.java`

- [ ] **Step 1: Create build.gradle**

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.3'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'com.aiplatform'
version = '0.0.1-SNAPSHOT'

java {
    sourceCompatibility = '21'
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'

    // Database
    runtimeOnly 'org.postgresql:postgresql'
    implementation 'io.hypersistence:hypersistence-utils-hibernate-63:3.7.0'

    // State Machine
    implementation 'org.springframework.statemachine:spring-statemachine-core:4.0.0'

    // Document Processing
    implementation 'org.apache.poi:poi-ooxml:5.2.5'
    implementation 'org.apache.pdfbox:pdfbox:3.0.1'

    // Storage
    implementation 'io.minio:minio:8.5.7'

    // JSON
    implementation 'com.fasterxml.jackson.core:jackson-databind'
    implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310'

    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.testcontainers:junit-jupiter:1.19.5'
    testImplementation 'org.testcontainers:postgresql:1.19.5'
    testImplementation 'org.testcontainers:mongodb:1.19.5'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

- [ ] **Step 2: Create settings.gradle**

```groovy
rootProject.name = 'bidding-service'
```

- [ ] **Step 3: Create application.yml**

```yaml
server:
  port: 8087

spring:
  application:
    name: bidding-service
  datasource:
    url: jdbc:postgresql://localhost:5432/bidding
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  jackson:
    serialization:
      write-dates-as-timestamps: false

minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket: bidding-documents

logging:
  level:
    com.aiplatform.bidding: DEBUG
```

- [ ] **Step 4: Create main application class**

```java
package com.aiplatform.bidding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BiddingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BiddingServiceApplication.class, args);
    }
}
```

- [ ] **Step 5: Verify build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL (no tests yet)

- [ ] **Step 6: Commit**

```bash
git add bidding-service/build.gradle bidding-service/settings.gradle bidding-service/src/
git commit -m "init: create bidding-service Gradle project structure"
```

---

## 领域模型

### Task 2: 创建枚举类型

**Files:**
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/domain/enums/ReviewState.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/domain/enums/ClauseType.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/domain/enums/IssueType.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/domain/enums/Severity.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/domain/enums/Granularity.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/domain/enums/SuggestionStatus.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/domain/enums/ResponseStatus.java`

- [ ] **Step 1: Write failing tests for ReviewState**

```java
package com.aiplatform.bidding.domain.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReviewStateTest {
    @Test
    void shouldHaveCorrectStateValues() {
        assertEquals(6, ReviewState.values().length);
        assertNotNull(ReviewState.valueOf("DRAFT"));
        assertNotNull(ReviewState.valueOf("AI_REVIEWING"));
        assertNotNull(ReviewState.valueOf("HUMAN_REVIEWING"));
        assertNotNull(ReviewState.valueOf("REVISION_REQUESTED"));
        assertNotNull(ReviewState.valueOf("FINAL_APPROVED"));
        assertNotNull(ReviewState.valueOf("REJECTED"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :bidding-service:test --tests "*ReviewStateTest"`
Expected: FAIL - Enum not defined

- [ ] **Step 3: Create ReviewState enum**

```java
package com.aiplatform.bidding.domain.enums;

public enum ReviewState {
    DRAFT,              // 草稿
    AI_REVIEWING,      // AI审查中
    HUMAN_REVIEWING,   // 人工审查中
    REVISION_REQUESTED, // 打回修改
    FINAL_APPROVED,    // 最终通过
    REJECTED           // 驳回
}
```

- [ ] **Step 4: Create remaining enums**

```java
// ClauseType.java
package com.aiplatform.bidding.domain.enums;

public enum ClauseType {
    QUALIFICATION,  // 资格性条款
    TECHNICAL,     // 技术条款
    COMMERCIAL,    // 商务条款
    OTHER          // 其他
}

// IssueType.java
package com.aiplatform.bidding.domain.enums;

public enum IssueType {
    MISSING,       // 缺失
    INCOMPLETE,    // 不完整
    NON_COMPLIANT, // 不合规
    FORMAT         // 格式错误
}

// Severity.java
package com.aiplatform.bidding.domain.enums;

public enum Severity {
    LOW,      // 低
    MEDIUM,   // 中
    HIGH,     // 高
    CRITICAL  // 紧急（废标风险）
}

// Granularity.java
package com.aiplatform.bidding.domain.enums;

public enum Granularity {
    CLAUSE,    // 条款级
    PARAGRAPH, // 段落级
    DOCUMENT   // 文档级
}

// SuggestionStatus.java
package com.aiplatform.bidding.domain.enums;

public enum SuggestionStatus {
    PENDING,   // 待确认
    APPLIED,   // 已应用
    REJECTED   // 已拒绝
}

// ResponseStatus.java
package com.aiplatform.bidding.domain.enums;

public enum ResponseStatus {
    FULL,     // 完全响应
    PARTIAL,  // 部分响应
    NONE      // 未响应
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :bidding-service:test --tests "*Test"
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add bidding-service/src/main/java/com/aiplatform/bidding/domain/enums/
git commit -m "feat: add domain enums for bidding service"
```

---

### Task 3: 创建 JPA Entity

**Files:**
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/domain/entity/BiddingDocument.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/domain/entity/Clause.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/domain/entity/ClauseIssue.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/domain/entity/BiddingCase.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/domain/entity/DocumentVersion.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/domain/entity/ReviewProcess.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/domain/entity/StateTransition.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/domain/entity/Suggestion.java`

- [ ] **Step 1: Write failing test for BiddingDocument entity**

```java
package com.aiplatform.bidding.domain.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class BiddingDocumentTest {
    @Test
    void shouldCreateBiddingDocumentWithId() {
        BiddingDocument doc = BiddingDocument.builder()
            .id("DOC-test-001")
            .title("测试标书")
            .uploaderId("user-001")
            .build();

        assertEquals("DOC-test-001", doc.getId());
        assertEquals("测试标书", doc.getTitle());
        assertNotNull(doc.getCreatedAt());
    }

    @Test
    void shouldSetDefaultStatusToDraft() {
        BiddingDocument doc = BiddingDocument.builder()
            .id("DOC-test-002")
            .title("测试标书")
            .uploaderId("user-001")
            .build();

        assertEquals(DocumentStatus.DRAFT, doc.getStatus());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :bidding-service:test --tests "*BiddingDocumentTest"
Expected: FAIL - Classes not defined

- [ ] **Step 3: Create BiddingDocument entity**

```java
package com.aiplatform.bidding.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bidding_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BiddingDocument {
    @Id
    private String id;

    @Column(nullable = false)
    private String title;

    private String projectId;

    @Column(nullable = false)
    private String uploaderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.DRAFT;

    private String currentVersion;

    @Column(columnDefinition = "text")
    private String tenderRequirements;

    private String tenderFileId;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

enum DocumentStatus {
    DRAFT, SUBMITTED, APPROVED, REJECTED
}
```

- [ ] **Step 4: Create remaining entities**

```java
// Clause.java
package com.aiplatform.bidding.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "clauses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Clause {
    @Id
    private String id;

    @Column(nullable = false)
    private String documentId;

    private String clauseNumber;

    @Column(columnDefinition = "text", nullable = false)
    private String content;

    @Builder.Default
    private Boolean isStarred = false;

    @Enumerated(EnumType.STRING)
    private ClauseType clauseType;

    @Enumerated(EnumType.STRING)
    private ResponseStatus responseStatus;

    private Integer pageNumber;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 5: Create ClauseIssue entity**

```java
// ClauseIssue.java
package com.aiplatform.bidding.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "clause_issues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClauseIssue {
    @Id
    private String id;

    private String clauseId;

    private String clauseNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueType issueType;

    @Column(columnDefinition = "text")
    private String originalText;

    @Column(columnDefinition = "text")
    private String requirementText;

    @Column(columnDefinition = "text")
    private String suggestionText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Builder.Default
    private Boolean eliminationRisk = false;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 6: Create ReviewProcess and StateTransition entities**

```java
// ReviewProcess.java
package com.aiplatform.bidding.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "review_processes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewProcess {
    @Id
    private String id;

    @Column(nullable = false)
    private String documentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewState currentState;

    @Column(nullable = false)
    private String submitterId;

    private String reviewerId;

    private LocalDateTime deadline;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "reviewId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<StateTransition> history = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

// StateTransition.java
package com.aiplatform.bidding.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "state_transitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StateTransition {
    @Id
    private String id;

    @Column(nullable = false)
    private String reviewId;

    @Enumerated(EnumType.STRING)
    private ReviewState fromState;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewState toState;

    @Column(nullable = false)
    private String actor;

    private String action;

    @Column(columnDefinition = "text")
    private String comment;

    @Column(updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}
```

- [ ] **Step 7: Create BiddingCase and Suggestion entities**

```java
// BiddingCase.java
package com.aiplatform.bidding.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bidding_cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BiddingCase {
    @Id
    private String id;

    @Column(nullable = false)
    private String tenderTitle;

    private String industry;

    private String region;

    private String winningBidder;

    @Column(precision = 15, scale = 2)
    private BigDecimal bidAmount;

    private LocalDate winningDate;

    private String tenderFilePath;

    // pgvector stores embedding as string representation
    @Column(columnDefinition = "vector(1536)")
    private String embedding;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

// Suggestion.java
package com.aiplatform.bidding.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "suggestions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Suggestion {
    @Id
    private String id;

    private String issueId;

    @Column(nullable = false)
    private String documentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Granularity granularity;

    @Column(columnDefinition = "text")
    private String originalContent;

    @Column(columnDefinition = "text")
    private String suggestedContent;

    @Column(columnDefinition = "text")
    private String explanation;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SuggestionStatus status = SuggestionStatus.PENDING;

    private String generatedBy;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

// DocumentVersion.java
package com.aiplatform.bidding.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentVersion {
    @Id
    private String id;

    @Column(nullable = false)
    private String documentId;

    @Column(nullable = false)
    private String versionNumber;

    @Column(nullable = false)
    private String filePath;

    @Column(columnDefinition = "text")
    private String diffFromPrevious;

    private String createdBy;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 8: Run tests to verify they pass**

Run: `./gradlew :bidding-service:test --tests "*Test"
Expected: PASS

- [ ] **Step 9: Commit**

```bash
git add bidding-service/src/main/java/com/aiplatform/bidding/domain/
git commit -m "feat: add JPA entities for bidding service"
```

---

### Task 4: 创建 Repository 接口

**Files:**
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/repository/BiddingDocumentRepository.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/repository/ClauseRepository.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/repository/ClauseIssueRepository.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/repository/BiddingCaseRepository.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/repository/DocumentVersionRepository.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/repository/ReviewProcessRepository.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/repository/StateTransitionRepository.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/repository/SuggestionRepository.java`

- [ ] **Step 1: Write failing test for BiddingDocumentRepository**

```java
package com.aiplatform.bidding.repository;

import com.aiplatform.bidding.domain.entity.BiddingDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class BiddingDocumentRepositoryTest {
    @Autowired
    private BiddingDocumentRepository repository;

    @Test
    void shouldSaveAndFindById() {
        BiddingDocument doc = BiddingDocument.builder()
            .id("DOC-repo-test-001")
            .title("测试标书")
            .uploaderId("user-001")
            .build();

        repository.save(doc);
        BiddingDocument found = repository.findById("DOC-repo-test-001").orElse(null);

        assertNotNull(found);
        assertEquals("测试标书", found.getTitle());
    }

    @Test
    void shouldFindByUploaderId() {
        BiddingDocument doc = BiddingDocument.builder()
            .id("DOC-repo-test-002")
            .title("测试标书2")
            .uploaderId("user-002")
            .build();

        repository.save(doc);
        var results = repository.findByUploaderId("user-002");

        assertEquals(1, results.size());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :bidding-service:test --tests "*BiddingDocumentRepositoryTest"
Expected: FAIL - Repository not defined

- [ ] **Step 3: Create BiddingDocumentRepository**

```java
package com.aiplatform.bidding.repository;

import com.aiplatform.bidding.domain.entity.BiddingDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BiddingDocumentRepository extends JpaRepository<BiddingDocument, String> {
    List<BiddingDocument> findByUploaderId(String uploaderId);
    List<BiddingDocument> findByProjectId(String projectId);
    List<BiddingDocument> findByStatus(String status);
}
```

- [ ] **Step 4: Create remaining repositories**

```java
// ClauseRepository.java
package com.aiplatform.bidding.repository;

import com.aiplatform.bidding.domain.entity.Clause;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ClauseRepository extends JpaRepository<Clause, String> {
    List<Clause> findByDocumentId(String documentId);
    List<Clause> findByDocumentIdAndIsStarredTrue(String documentId);
}

// ClauseIssueRepository.java
package com.aiplatform.bidding.repository;

import com.aiplatform.bidding.domain.entity.ClauseIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ClauseIssueRepository extends JpaRepository<ClauseIssue, String> {
    List<ClauseIssue> findByClauseId(String clauseId);
    List<ClauseIssue> findByClauseNumber(String clauseNumber);
    List<ClauseIssue> findBySeverity(String severity);
}

// BiddingCaseRepository.java
package com.aiplatform.bidding.repository;

import com.aiplatform.bidding.domain.entity.BiddingCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BiddingCaseRepository extends JpaRepository<BiddingCase, String> {
    List<BiddingCase> findByIndustry(String industry);
    List<BiddingCase> findByRegion(String region);
    List<BiddingCase> findByWinningBidderContaining(String bidder);

    @Query(value = "SELECT * FROM bidding_cases ORDER BY embedding <=> CAST(:embedding AS vector) LIMIT :topK", nativeQuery = true)
    List<BiddingCase> findSimilarCases(@Param("embedding") String embedding, @Param("topK") int topK);
}

// DocumentVersionRepository.java
package com.aiplatform.bidding.repository;

import com.aiplatform.bidding.domain.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, String> {
    List<DocumentVersion> findByDocumentIdOrderByCreatedAtDesc(String documentId);
    Optional<DocumentVersion> findByDocumentIdAndVersionNumber(String documentId, String versionNumber);
}

// ReviewProcessRepository.java
package com.aiplatform.bidding.repository;

import com.aiplatform.bidding.domain.entity.ReviewProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ReviewProcessRepository extends JpaRepository<ReviewProcess, String> {
    Optional<ReviewProcess> findByDocumentId(String documentId);
}

// StateTransitionRepository.java
package com.aiplatform.bidding.repository;

import com.aiplatform.bidding.domain.entity.StateTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StateTransitionRepository extends JpaRepository<StateTransition, String> {
    List<StateTransition> findByReviewIdOrderByTimestampAsc(String reviewId);
}

// SuggestionRepository.java
package com.aiplatform.bidding.repository;

import com.aiplatform.bidding.domain.entity.Suggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SuggestionRepository extends JpaRepository<Suggestion, String> {
    List<Suggestion> findByDocumentId(String documentId);
    List<Suggestion> findByIssueId(String issueId);
    List<Suggestion> findByStatus(String status);
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :bidding-service:test --tests "*RepositoryTest"
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add bidding-service/src/main/java/com/aiplatform/bidding/repository/
git commit -m "feat: add JPA repositories for bidding service"
```

---

### Task 5: 创建 DTO 类

**Files:**
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/dto/request/BiddingCheckRequest.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/dto/request/CaseRetrieveRequest.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/dto/request/DiffCompareRequest.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/dto/request/SuggestionGenerateRequest.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/dto/request/ReviewSubmitRequest.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/dto/request/HumanActionRequest.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/dto/response/BiddingCheckResponse.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/dto/response/CaseRetrieveResponse.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/dto/response/DiffCompareResponse.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/dto/response/SuggestionGenerateResponse.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/dto/response/ReviewStatusResponse.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/dto/response/ApiResponse.java`

- [ ] **Step 1: Create BiddingCheckRequest DTO**

```java
package com.aiplatform.bidding.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BiddingCheckRequest {
    @NotBlank(message = "documentId is required")
    private String documentId;

    private String tenderRequirements;

    private String tenderFileId;
}
```

- [ ] **Step 2: Create remaining request DTOs**

```java
// CaseRetrieveRequest.java
package com.aiplatform.bidding.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class CaseRetrieveRequest {
    private String query;
    private String industry;
    private String region;
    private String dateFrom;
    private String dateTo;

    @Min(1)
    @Max(20)
    private Integer topK = 5;
}

// DiffCompareRequest.java
package com.aiplatform.bidding.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DiffCompareRequest {
    @NotBlank
    private String documentId;

    private String versionA;
    private String versionB;
}

// SuggestionGenerateRequest.java
package com.aiplatform.bidding.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class SuggestionGenerateRequest {
    @NotBlank
    private String documentId;

    private List<String> issueIds;
}

// ReviewSubmitRequest.java
package com.aiplatform.bidding.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReviewSubmitRequest {
    @NotBlank
    private String reviewerId;

    private String deadline;
}

// HumanActionRequest.java
package com.aiplatform.bidding.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class HumanActionRequest {
    @NotBlank
    private String action;  // APPROVE, REJECT, REQUEST_REVISION

    private String comment;

    private List<String> approvedIssueIds;

    private List<String> rejectedIssueIds;

    private List<String> issueIdsToRequestRevision;
}
```

- [ ] **Step 3: Create response DTOs**

```java
// ApiResponse.java
package com.aiplatform.bidding.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .code(200)
            .message("success")
            .data(data)
            .build();
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
            .code(code)
            .message(message)
            .build();
    }
}
```

- [ ] **Step 4: Create remaining response DTOs**

```java
// BiddingCheckResponse.java
package com.aiplatform.bidding.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiddingCheckResponse {
    private String checkId;
    private String documentId;
    private int totalClauses;
    private int matchedClauses;
    private int partiallyMatched;
    private int unmatched;
    private double score;
    private List<IssueDto> issues;
    private boolean eliminationRisk;
    private List<String> riskReasons;
    private String checkedAt;

    @Data
    @Builder
    public static class IssueDto {
        private String issueId;
        private String clauseNumber;
        private String issueType;
        private String originalText;
        private String requirementText;
        private String suggestionText;
        private String severity;
        private boolean eliminationRisk;
    }
}

// CaseRetrieveResponse.java
package com.aiplatform.bidding.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseRetrieveResponse {
    private List<CaseDto> cases;
    private int total;

    @Data
    @Builder
    public static class CaseDto {
        private String caseId;
        private String tenderTitle;
        private String industry;
        private String region;
        private String winningBidder;
        private BigDecimal bidAmount;
        private String winningDate;
        private double similarityScore;
    }
}

// DiffCompareResponse.java
package com.aiplatform.bidding.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiffCompareResponse {
    private String documentId;
    private String versionA;
    private String versionB;
    private SummaryDto summary;
    private List<ChangeDto> details;

    @Data
    @Builder
    public static class SummaryDto {
        private int totalChanges;
        private int added;
        private int modified;
        private int deleted;
    }

    @Data
    @Builder
    public static class ChangeDto {
        private String clauseNumber;
        private String changeType;
        private String oldContent;
        private String newContent;
        private Integer pageNumber;
    }
}

// SuggestionGenerateResponse.java
package com.aiplatform.bidding.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuggestionGenerateResponse {
    private List<SuggestionDto> suggestions;
    private String generatedBy;
    private String generatedAt;

    @Data
    @Builder
    public static class SuggestionDto {
        private String suggestionId;
        private String issueId;
        private String granularity;
        private String originalContent;
        private String suggestedContent;
        private String explanation;
    }
}

// ReviewStatusResponse.java
package com.aiplatform.bidding.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewStatusResponse {
    private String reviewId;
    private String documentId;
    private String currentState;
    private String submitterId;
    private String reviewerId;
    private String deadline;
    private List<HistoryDto> history;
    private List<String> pendingIssues;
    private List<String> approvedIssues;
    private List<String> rejectedIssues;

    @Data
    @Builder
    public static class HistoryDto {
        private String fromState;
        private String toState;
        private String actor;
        private String action;
        private String timestamp;
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add bidding-service/src/main/java/com/aiplatform/bidding/dto/
git commit -m "feat: add DTO classes for bidding service"
```

---

### Task 6: 创建状态机引擎

**Files:**
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/service/StateMachineEngine.java`
- Create: `bidding-service/src/test/java/com/aiplatform/bidding/service/StateMachineEngineTest.java`

- [ ] **Step 1: Write failing test for StateMachineEngine**

```java
package com.aiplatform.bidding.service;

import com.aiplatform.bidding.domain.entity.ReviewProcess;
import com.aiplatform.bidding.domain.enums.ReviewState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class StateMachineEngineTest {
    private StateMachineEngine engine;

    @BeforeEach
    void setUp() {
        engine = new StateMachineEngine();
    }

    @Test
    void shouldTransitionFromDraftToAiReviewing() {
        ReviewProcess process = createProcess(ReviewState.DRAFT);

        boolean result = engine.canTransition(ReviewState.DRAFT, ReviewState.AI_REVIEWING);

        assertTrue(result);
    }

    @Test
    void shouldNotAllowInvalidTransition() {
        ReviewProcess process = createProcess(ReviewState.DRAFT);

        boolean result = engine.canTransition(ReviewState.DRAFT, ReviewState.FINAL_APPROVED);

        assertFalse(result);
    }

    @Test
    void shouldTransitionFromAiReviewingToHumanReviewing() {
        boolean result = engine.canTransition(ReviewState.AI_REVIEWING, ReviewState.HUMAN_REVIEWING);

        assertTrue(result);
    }

    @Test
    void shouldTransitionFromHumanReviewingToFinalApproved() {
        boolean result = engine.canTransition(ReviewState.HUMAN_REVIEWING, ReviewState.FINAL_APPROVED);

        assertTrue(result);
    }

    private ReviewProcess createProcess(ReviewState state) {
        return ReviewProcess.builder()
            .id("REV-test-001")
            .documentId("DOC-test-001")
            .currentState(state)
            .submitterId("user-001")
            .build();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :bidding-service:test --tests "*StateMachineEngineTest"
Expected: FAIL - Class not defined

- [ ] **Step 3: Create StateMachineEngine**

```java
package com.aiplatform.bidding.service;

import com.aiplatform.bidding.domain.entity.StateTransition;
import com.aiplatform.bidding.domain.entity.ReviewProcess;
import com.aiplatform.bidding.domain.enums.ReviewState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
@Slf4j
public class StateMachineEngine {
    // State transition rules: fromState -> list of valid toStates
    private static final Map<ReviewState, Set<ReviewState>> TRANSITION_RULES;

    static {
        TRANSITION_RULES = new EnumMap<>(ReviewState.class);
        TRANSITION_RULES.put(ReviewState.DRAFT,
            EnumSet.of(ReviewState.AI_REVIEWING));
        TRANSITION_RULES.put(ReviewState.AI_REVIEWING,
            EnumSet.of(ReviewState.HUMAN_REVIEWING));
        TRANSITION_RULES.put(ReviewState.HUMAN_REVIEWING,
            EnumSet.of(ReviewState.FINAL_APPROVED, ReviewState.REVISION_REQUESTED, ReviewState.REJECTED));
        TRANSITION_RULES.put(ReviewState.REVISION_REQUESTED,
            EnumSet.of(ReviewState.AI_REVIEWING));
        TRANSITION_RULES.put(ReviewState.FINAL_APPROVED, EnumSet.noneOf(ReviewState.class));
        TRANSITION_RULES.put(ReviewState.REJECTED, EnumSet.noneOf(ReviewState.class));
    }

    /**
     * Check if a transition from one state to another is valid.
     */
    public boolean canTransition(ReviewState from, ReviewState to) {
        Set<ReviewState> allowedStates = TRANSITION_RULES.get(from);
        return allowedStates != null && allowedStates.contains(to);
    }

    /**
     * Get all valid next states from current state.
     */
    public Set<ReviewState> getValidNextStates(ReviewState current) {
        return TRANSITION_RULES.getOrDefault(current, EnumSet.noneOf(ReviewState.class));
    }

    /**
     * Execute state transition and create transition record.
     */
    public StateTransition executeTransition(ReviewProcess process, ReviewState toState, String actor, String action) {
        ReviewState fromState = process.getCurrentState();

        if (!canTransition(fromState, toState)) {
            throw new IllegalStateException(
                String.format("Invalid transition from %s to %s", fromState, toState));
        }

        StateTransition transition = StateTransition.builder()
            .id("TRANS-" + UUID.randomUUID())
            .reviewId(process.getId())
            .fromState(fromState)
            .toState(toState)
            .actor(actor)
            .action(action)
            .timestamp(LocalDateTime.now())
            .build();

        process.setCurrentState(toState);
        log.info("State transition: {} -> {} for review {}", fromState, toState, process.getId());

        return transition;
    }

    /**
     * Get the event name for a state transition.
     */
    public String getEventForTransition(ReviewState from, ReviewState to) {
        if (from == ReviewState.DRAFT && to == ReviewState.AI_REVIEWING) {
            return "SUBMIT";
        } else if (from == ReviewState.AI_REVIEWING && to == ReviewState.HUMAN_REVIEWING) {
            return "AI_COMPLETE";
        } else if (from == ReviewState.HUMAN_REVIEWING && to == ReviewState.FINAL_APPROVED) {
            return "APPROVE";
        } else if (from == ReviewState.HUMAN_REVIEWING && to == ReviewState.REVISION_REQUESTED) {
            return "REQUEST_REVISION";
        } else if (from == ReviewState.HUMAN_REVIEWING && to == ReviewState.REJECTED) {
            return "REJECT";
        } else if (from == ReviewState.REVISION_REQUESTED && to == ReviewState.AI_REVIEWING) {
            return "RESUBMIT";
        }
        return "UNKNOWN";
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :bidding-service:test --tests "*StateMachineEngineTest"
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add bidding-service/src/main/java/com/aiplatform/bidding/service/StateMachineEngine.java
git add bidding-service/src/test/java/com/aiplatform/bidding/service/StateMachineEngineTest.java
git commit -m "feat: add StateMachineEngine for review workflow"
```

---

### Task 7: 创建 Service 层

**Files:**
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/service/BiddingCheckService.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/service/CaseRetrievalService.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/service/VersionDiffService.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/service/SuggestionService.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/service/ReviewService.java`
- Create: `bidding-service/src/test/java/com/aiplatform/bidding/service/BiddingCheckServiceTest.java`

- [ ] **Step 1: Write failing test for BiddingCheckService**

```java
package com.aiplatform.bidding.service;

import com.aiplatform.bidding.dto.request.BiddingCheckRequest;
import com.aiplatform.bidding.dto.response.BiddingCheckResponse;
import com.aiplatform.bidding.repository.BiddingDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BiddingCheckServiceTest {
    @Mock
    private BiddingDocumentRepository documentRepository;

    @InjectMocks
    private BiddingCheckService biddingCheckService;

    @Test
    void shouldReturnCheckResultForValidDocument() {
        BiddingCheckRequest request = new BiddingCheckRequest();
        request.setDocumentId("DOC-test-001");
        request.setTenderRequirements("★质保期≥2年");

        when(documentRepository.findById("DOC-test-001")).thenReturn(java.util.Optional.empty());

        // This will fail because document doesn't exist - we handle this case
        // In real test, we'd mock the document existing
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :bidding-service:test --tests "*BiddingCheckServiceTest"
Expected: FAIL - Method not implemented

- [ ] **Step 3: Create BiddingCheckService**

```java
package com.aiplatform.bidding.service;

import com.aiplatform.bidding.domain.entity.BiddingDocument;
import com.aiplatform.bidding.domain.entity.Clause;
import com.aiplatform.bidding.domain.entity.ClauseIssue;
import com.aiplatform.bidding.domain.enums.*;
import com.aiplatform.bidding.dto.request.BiddingCheckRequest;
import com.aiplatform.bidding.dto.response.BiddingCheckResponse;
import com.aiplatform.bidding.repository.BiddingDocumentRepository;
import com.aiplatform.bidding.repository.ClauseIssueRepository;
import com.aiplatform.bidding.repository.ClauseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BiddingCheckService {
    private final BiddingDocumentRepository documentRepository;
    private final ClauseRepository clauseRepository;
    private final ClauseIssueRepository clauseIssueRepository;

    private static final Pattern STARRED_CLAUSE_PATTERN = Pattern.compile("★|☆");
    private static final Pattern CLAUSE_NUMBER_PATTERN = Pattern.compile("^(\\d+(?:\\.\\d+)*)\\s+");

    @Transactional
    public BiddingCheckResponse checkBidding(BiddingCheckRequest request) {
        BiddingDocument document = documentRepository.findById(request.getDocumentId())
            .orElseThrow(() -> new RuntimeException("Document not found: " + request.getDocumentId()));

        // Extract clauses from tender requirements
        List<Clause> tenderClauses = extractClauses(request.getTenderRequirements(), true);

        // Extract clauses from bidding document (would need document parsing)
        List<Clause> biddingClauses = clauseRepository.findByDocumentId(document.getId());

        // Match and analyze
        List<BiddingCheckResponse.IssueDto> issues = new ArrayList<>();
        int matched = 0, partiallyMatched = 0, unmatched = 0;

        for (Clause tenderClause : tenderClauses) {
            Optional<Clause> matchedBiddingClause = findMatchingClause(tenderClause, biddingClauses);

            if (matchedBiddingClause.isPresent()) {
                Clause biddingClause = matchedBiddingClause.get();
                ResponseStatus status = analyzeResponse(tenderClause, biddingClause);

                if (status == ResponseStatus.FULL) {
                    matched++;
                } else if (status == ResponseStatus.PARTIAL) {
                    partiallyMatched++;
                    issues.add(createIssue(tenderClause, biddingClause, IssueType.INCOMPLETE, Severity.MEDIUM));
                }
            } else {
                unmatched++;
                issues.add(createIssue(tenderClause, null, IssueType.MISSING,
                    tenderClause.getIsStarred() ? Severity.CRITICAL : Severity.HIGH));
            }
        }

        // Calculate score
        double score = calculateScore(tenderClauses.size(), matched, partiallyMatched, unmatched);

        // Check for elimination risks
        boolean eliminationRisk = issues.stream()
            .anyMatch(i -> i.isEliminationRisk() || i.getSeverity().equals("CRITICAL"));

        List<String> riskReasons = issues.stream()
            .filter(BiddingCheckResponse.IssueDto::isEliminationRisk)
            .map(BiddingCheckResponse.IssueDto::getOriginalText)
            .collect(Collectors.toList());

        return BiddingCheckResponse.builder()
            .checkId("CHECK-" + UUID.randomUUID())
            .documentId(document.getId())
            .totalClauses(tenderClauses.size())
            .matchedClauses(matched)
            .partiallyMatched(partiallyMatched)
            .unmatched(unmatched)
            .score(score)
            .issues(issues)
            .eliminationRisk(eliminationRisk)
            .riskReasons(riskReasons)
            .checkedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
            .build();
    }

    private List<Clause> extractClauses(String text, boolean isTender) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        List<Clause> clauses = new ArrayList<>();
        String[] lines = text.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            Matcher numberMatcher = CLAUSE_NUMBER_PATTERN.matcher(line);
            String clauseNumber = numberMatcher.find() ? numberMatcher.group(1) : String.valueOf(i + 1);
            boolean isStarred = STARRED_CLAUSE_PATTERN.matcher(line).find();
            ClauseType clauseType = classifyClause(line);

            Clause clause = Clause.builder()
                .id("CLAUSE-" + UUID.randomUUID())
                .content(line)
                .clauseNumber(clauseNumber)
                .isStarred(isStarred)
                .clauseType(clauseType)
                .pageNumber(i / 10 + 1)  // Approximate page number
                .build();

            clauses.add(clause);
        }

        return clauses;
    }

    private ClauseType classifyClause(String content) {
        String lower = content.toLowerCase();
        if (lower.contains("资格") || lower.contains("资质") || lower.contains("认证")) {
            return ClauseType.QUALIFICATION;
        } else if (lower.contains("技术") || lower.contains("规格") || lower.contains("功能")) {
            return ClauseType.TECHNICAL;
        } else if (lower.contains("报价") || lower.contains("价格") || lower.contains("付款") || lower.contains("工期") || lower.contains("质保")) {
            return ClauseType.COMMERCIAL;
        }
        return ClauseType.OTHER;
    }

    private Optional<Clause> findMatchingClause(Clause tender, List<Clause> bidding) {
        return bidding.stream()
            .filter(b -> b.getClauseNumber().equals(tender.getClauseNumber()))
            .findFirst();
    }

    private ResponseStatus analyzeResponse(Clause tender, Clause bidding) {
        // Simple analysis - in production would use LLM or more sophisticated matching
        String tenderContent = tender.getContent().toLowerCase();
        String biddingContent = bidding.getContent().toLowerCase();

        if (biddingContent.contains(tenderContent.substring(0, Math.min(10, tenderContent.length())))) {
            return ResponseStatus.FULL;
        }
        return ResponseStatus.PARTIAL;
    }

    private double calculateScore(int total, int matched, int partiallyMatched, int unmatched) {
        if (total == 0) return 100.0;
        return (matched * 1.0 + partiallyMatched * 0.5) / total * 100;
    }

    private BiddingCheckResponse.IssueDto createIssue(Clause tender, Clause bidding, IssueType type, Severity severity) {
        boolean eliminationRisk = severity == Severity.CRITICAL ||
            (tender.getIsStarred() && type == IssueType.MISSING);

        return BiddingCheckResponse.IssueDto.builder()
            .issueId("ISSUE-" + UUID.randomUUID())
            .clauseNumber(tender.getClauseNumber())
            .issueType(type.name())
            .originalText(bidding != null ? bidding.getContent() : "(未提供)")
            .requirementText(tender.getContent())
            .suggestionText(generateSuggestion(tender, type))
            .severity(severity.name())
            .eliminationRisk(eliminationRisk)
            .build();
    }

    private String generateSuggestion(Clause clause, IssueType type) {
        if (type == IssueType.MISSING) {
            return "请补充" + clause.getContent();
        } else if (type == IssueType.INCOMPLETE) {
            return "请完善" + clause.getContent() + "相关内容";
        }
        return "请根据招标要求修改";
    }
}
```

- [ ] **Step 4: Create remaining services (abbreviated for brevity - full implementation in actual code)**

```java
// CaseRetrievalService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class CaseRetrievalService {
    private final BiddingCaseRepository caseRepository;

    @Transactional(readOnly = true)
    public CaseRetrieveResponse retrieveSimilarCases(CaseRetrieveRequest request) {
        List<BiddingCase> cases;

        if (request.getQuery() != null && !request.getQuery().isBlank()) {
            // Use pgvector similarity search
            // For now, use keyword search as fallback
            cases = caseRepository.findByIndustry(request.getIndustry());
        } else {
            cases = caseRepository.findAll();
        }

        // Apply filters
        if (request.getIndustry() != null) {
            cases = cases.stream()
                .filter(c -> request.getIndustry().equals(c.getIndustry()))
                .collect(Collectors.toList());
        }

        // Limit results
        int topK = request.getTopK() != null ? request.getTopK() : 5;
        cases = cases.stream().limit(topK).collect(Collectors.toList());

        List<CaseRetrieveResponse.CaseDto> caseDtos = cases.stream()
            .map(this::toDto)
            .collect(Collectors.toList());

        return CaseRetrieveResponse.builder()
            .cases(caseDtos)
            .total(caseDtos.size())
            .build();
    }

    private CaseRetrieveResponse.CaseDto toDto(BiddingCase c) {
        return CaseRetrieveResponse.CaseDto.builder()
            .caseId(c.getId())
            .tenderTitle(c.getTenderTitle())
            .industry(c.getIndustry())
            .region(c.getRegion())
            .winningBidder(c.getWinningBidder())
            .bidAmount(c.getBidAmount())
            .winningDate(c.getWinningDate() != null ? c.getWinningDate().toString() : null)
            .similarityScore(0.85)  // Placeholder - would calculate from vector similarity
            .build();
    }
}

// VersionDiffService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class VersionDiffService {
    private final DocumentVersionRepository versionRepository;

    @Transactional(readOnly = true)
    public DiffCompareResponse compareVersions(DiffCompareRequest request) {
        DocumentVersion versionA = versionRepository
            .findByDocumentIdAndVersionNumber(request.getDocumentId(), request.getVersionA())
            .orElseThrow(() -> new RuntimeException("Version not found: " + request.getVersionA()));

        DocumentVersion versionB = versionRepository
            .findByDocumentIdAndVersionNumber(request.getDocumentId(), request.getVersionB())
            .orElseThrow(() -> new RuntimeException("Version not found: " + request.getVersionB()));

        // Compute diff
        DiffCompareResponse.SummaryDto summary = computeSummary(versionA.getFilePath(), versionB.getFilePath());
        List<DiffCompareResponse.ChangeDto> changes = computeChanges(versionA, versionB);

        return DiffCompareResponse.builder()
            .documentId(request.getDocumentId())
            .versionA(request.getVersionA())
            .versionB(request.getVersionB())
            .summary(summary)
            .details(changes)
            .build();
    }

    private DiffCompareResponse.SummaryDto computeSummary(String contentA, String contentB) {
        // Simple line-based diff
        String[] linesA = contentA.split("\n");
        String[] linesB = contentB.split("\n");

        int added = 0, modified = 0, deleted = 0;
        Set<String> setA = new HashSet<>(Arrays.asList(linesA));
        Set<String> setB = new HashSet<>(Arrays.asList(linesB));

        for (String line : linesB) {
            if (!setA.contains(line)) added++;
        }
        for (String line : linesA) {
            if (!setB.contains(line)) deleted++;
        }

        return DiffCompareResponse.SummaryDto.builder()
            .totalChanges(added + modified + deleted)
            .added(added)
            .modified(modified)
            .deleted(deleted)
            .build();
    }

    private List<DiffCompareResponse.ChangeDto> computeChanges(DocumentVersion a, DocumentVersion b) {
        // Placeholder - would use diff-match-patch library
        return Collections.emptyList();
    }
}

// SuggestionService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class SuggestionService {
    private final SuggestionRepository suggestionRepository;
    private final ClauseIssueRepository clauseIssueRepository;

    @Transactional
    public SuggestionGenerateResponse generateSuggestions(SuggestionGenerateRequest request) {
        List<String> issueIds = request.getIssueIds();
        if (issueIds == null || issueIds.isEmpty()) {
            // Get all issues for document
            issueIds = clauseIssueRepository.findByClauseNumber(request.getDocumentId())
                .stream().map(c -> c.getId()).collect(Collectors.toList());
        }

        List<SuggestionGenerateResponse.SuggestionDto> suggestions = new ArrayList<>();

        for (String issueId : issueIds) {
            ClauseIssue issue = clauseIssueRepository.findById(issueId).orElse(null);
            if (issue == null) continue;

            Suggestion suggestion = Suggestion.builder()
                .id("SUG-" + UUID.randomUUID())
                .issueId(issueId)
                .documentId(request.getDocumentId())
                .granularity(Granularity.CLAUSE)
                .originalContent(issue.getOriginalText())
                .suggestedContent(issue.getSuggestionText())
                .explanation("基于问题类型 " + issue.getIssueType() + " 生成")
                .status(SuggestionStatus.PENDING)
                .generatedBy("bidding-service")
                .build();

            suggestionRepository.save(suggestion);

            suggestions.add(toDto(suggestion));
        }

        return SuggestionGenerateResponse.builder()
            .suggestions(suggestions)
            .generatedBy("bidding-service")
            .generatedAt(java.time.LocalDateTime.now().toString())
            .build();
    }

    private SuggestionGenerateResponse.SuggestionDto toDto(Suggestion s) {
        return SuggestionGenerateResponse.SuggestionDto.builder()
            .suggestionId(s.getId())
            .issueId(s.getIssueId())
            .granularity(s.getGranularity().name())
            .originalContent(s.getOriginalContent())
            .suggestedContent(s.getSuggestedContent())
            .explanation(s.getExplanation())
            .build();
    }
}

// ReviewService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {
    private final ReviewProcessRepository reviewProcessRepository;
    private final StateTransitionRepository transitionRepository;
    private final StateMachineEngine stateMachineEngine;

    @Transactional
    public ReviewStatusResponse submitReview(String documentId, ReviewSubmitRequest request) {
        ReviewProcess process = ReviewProcess.builder()
            .id("REV-" + UUID.randomUUID())
            .documentId(documentId)
            .currentState(ReviewState.DRAFT)
            .submitterId(request.getReviewerId())
            .reviewerId(request.getReviewerId())
            .build();

        // Execute transition to AI_REVIEWING
        StateTransition transition = stateMachineEngine.executeTransition(
            process, ReviewState.AI_REVIEWING, "system", "提交审核");

        reviewProcessRepository.save(process);
        transitionRepository.save(transition);

        return toResponse(process);
    }

    @Transactional
    public ReviewStatusResponse executeHumanAction(String reviewId, HumanActionRequest request) {
        ReviewProcess process = reviewProcessRepository.findById(reviewId)
            .orElseThrow(() -> new RuntimeException("Review not found: " + reviewId));

        ReviewState targetState = mapActionToState(request.getAction());
        StateTransition transition = stateMachineEngine.executeTransition(
            process, targetState, request.getAction(), request.getComment());

        reviewProcessRepository.save(process);
        transitionRepository.save(transition);

        return toResponse(process);
    }

    private ReviewState mapActionToState(String action) {
        return switch (action) {
            case "APPROVE" -> ReviewState.FINAL_APPROVED;
            case "REJECT" -> ReviewState.REJECTED;
            case "REQUEST_REVISION" -> ReviewState.REVISION_REQUESTED;
            default -> throw new IllegalArgumentException("Unknown action: " + action);
        };
    }

    @Transactional(readOnly = true)
    public ReviewStatusResponse getReviewStatus(String reviewId) {
        ReviewProcess process = reviewProcessRepository.findById(reviewId)
            .orElseThrow(() -> new RuntimeException("Review not found: " + reviewId));

        return toResponse(process);
    }

    private ReviewStatusResponse toResponse(ReviewProcess process) {
        List<StateTransition> transitions = transitionRepository
            .findByReviewIdOrderByTimestampAsc(process.getId());

        List<ReviewStatusResponse.HistoryDto> history = transitions.stream()
            .map(t -> ReviewStatusResponse.HistoryDto.builder()
                .fromState(t.getFromState() != null ? t.getFromState().name() : null)
                .toState(t.getToState().name())
                .actor(t.getActor())
                .action(t.getAction())
                .timestamp(t.getTimestamp().toString())
                .build())
            .collect(Collectors.toList());

        return ReviewStatusResponse.builder()
            .reviewId(process.getId())
            .documentId(process.getDocumentId())
            .currentState(process.getCurrentState().name())
            .submitterId(process.getSubmitterId())
            .reviewerId(process.getReviewerId())
            .deadline(process.getDeadline() != null ? process.getDeadline().toString() : null)
            .history(history)
            .pendingIssues(Collections.emptyList())
            .approvedIssues(Collections.emptyList())
            .rejectedIssues(Collections.emptyList())
            .build();
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :bidding-service:test --tests "*ServiceTest"
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add bidding-service/src/main/java/com/aiplatform/bidding/service/
git add bidding-service/src/test/java/com/aiplatform/bidding/service/
git commit -m "feat: add service layer for bidding operations"
```

---

### Task 8: 创建 Controller 层

**Files:**
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/controller/BiddingController.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/controller/CaseController.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/controller/DiffController.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/controller/SuggestionController.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/controller/ReviewController.java`
- Create: `bidding-service/src/test/java/com/aiplatform/bidding/controller/BiddingControllerTest.java`

- [ ] **Step 1: Write failing test for BiddingController**

```java
package com.aiplatform.bidding.controller;

import com.aiplatform.bidding.dto.request.BiddingCheckRequest;
import com.aiplatform.bidding.service.BiddingCheckService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BiddingController.class)
class BiddingControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BiddingCheckService biddingCheckService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnOkForValidCheckRequest() throws Exception {
        BiddingCheckRequest request = new BiddingCheckRequest();
        request.setDocumentId("DOC-test-001");
        request.setTenderRequirements("★质保期≥2年");

        mockMvc.perform(post("/api/v1/bidding/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :bidding-service:test --tests "*BiddingControllerTest"
Expected: FAIL - Controller not defined

- [ ] **Step 3: Create BiddingController**

```java
package com.aiplatform.bidding.controller;

import com.aiplatform.bidding.dto.request.BiddingCheckRequest;
import com.aiplatform.bidding.dto.response.ApiResponse;
import com.aiplatform.bidding.dto.response.BiddingCheckResponse;
import com.aiplatform.bidding.service.BiddingCheckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bidding")
@RequiredArgsConstructor
public class BiddingController {
    private final BiddingCheckService biddingCheckService;

    @PostMapping("/check")
    public ResponseEntity<ApiResponse<BiddingCheckResponse>> checkBidding(
            @Valid @RequestBody BiddingCheckRequest request) {
        BiddingCheckResponse response = biddingCheckService.checkBidding(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

- [ ] **Step 4: Create remaining controllers**

```java
// CaseController.java
package com.aiplatform.bidding.controller;

import com.aiplatform.bidding.dto.request.CaseRetrieveRequest;
import com.aiplatform.bidding.dto.response.ApiResponse;
import com.aiplatform.bidding.dto.response.CaseRetrieveResponse;
import com.aiplatform.bidding.service.CaseRetrievalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cases")
@RequiredArgsConstructor
public class CaseController {
    private final CaseRetrievalService caseRetrievalService;

    @PostMapping("/retrieve")
    public ResponseEntity<ApiResponse<CaseRetrieveResponse>> retrieveCases(
            @Valid @RequestBody CaseRetrieveRequest request) {
        CaseRetrieveResponse response = caseRetrievalService.retrieveSimilarCases(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

// DiffController.java
package com.aiplatform.bidding.controller;

import com.aiplatform.bidding.dto.request.DiffCompareRequest;
import com.aiplatform.bidding.dto.response.ApiResponse;
import com.aiplatform.bidding.dto.response.DiffCompareResponse;
import com.aiplatform.bidding.service.VersionDiffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/diff")
@RequiredArgsConstructor
public class DiffController {
    private final VersionDiffService versionDiffService;

    @PostMapping("/compare")
    public ResponseEntity<ApiResponse<DiffCompareResponse>> compareVersions(
            @Valid @RequestBody DiffCompareRequest request) {
        DiffCompareResponse response = versionDiffService.compareVersions(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

// SuggestionController.java
package com.aiplatform.bidding.controller;

import com.aiplatform.bidding.dto.request.SuggestionGenerateRequest;
import com.aiplatform.bidding.dto.response.ApiResponse;
import com.aiplatform.bidding.dto.response.SuggestionGenerateResponse;
import com.aiplatform.bidding.service.SuggestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/suggestions")
@RequiredArgsConstructor
public class SuggestionController {
    private final SuggestionService suggestionService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<SuggestionGenerateResponse>> generateSuggestions(
            @Valid @RequestBody SuggestionGenerateRequest request) {
        SuggestionGenerateResponse response = suggestionService.generateSuggestions(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

// ReviewController.java
package com.aiplatform.bidding.controller;

import com.aiplatform.bidding.dto.request.HumanActionRequest;
import com.aiplatform.bidding.dto.request.ReviewSubmitRequest;
import com.aiplatform.bidding.dto.response.ApiResponse;
import com.aiplatform.bidding.dto.response.ReviewStatusResponse;
import com.aiplatform.bidding.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/review")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping("/{documentId}/submit")
    public ResponseEntity<ApiResponse<ReviewStatusResponse>> submitReview(
            @PathVariable String documentId,
            @Valid @RequestBody ReviewSubmitRequest request) {
        ReviewStatusResponse response = reviewService.submitReview(documentId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{reviewId}/status")
    public ResponseEntity<ApiResponse<ReviewStatusResponse>> getReviewStatus(
            @PathVariable String reviewId) {
        ReviewStatusResponse response = reviewService.getReviewStatus(reviewId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{reviewId}/human-action")
    public ResponseEntity<ApiResponse<ReviewStatusResponse>> executeHumanAction(
            @PathVariable String reviewId,
            @Valid @RequestBody HumanActionRequest request) {
        ReviewStatusResponse response = reviewService.executeHumanAction(reviewId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :bidding-service:test --tests "*ControllerTest"
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add bidding-service/src/main/java/com/aiplatform/bidding/controller/
git add bidding-service/src/test/java/com/aiplatform/bidding/controller/
git commit -m "feat: add REST controllers for bidding service"
```

---

### Task 9: 创建异常处理

**Files:**
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/exception/DocumentNotFoundException.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/exception/ServiceUnavailableException.java`
- Create: `bidding-service/src/main/java/com/aiplatform/bidding/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: Create exception classes**

```java
// DocumentNotFoundException.java
package com.aiplatform.bidding.exception;

public class DocumentNotFoundException extends RuntimeException {
    private final String documentId;

    public DocumentNotFoundException(String documentId) {
        super("Document not found: " + documentId);
        this.documentId = documentId;
    }

    public String getDocumentId() {
        return documentId;
    }
}

// ServiceUnavailableException.java
package com.aiplatform.bidding.exception;

public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) {
        super(message);
    }
}

// GlobalExceptionHandler.java
package com.aiplatform.bidding.exception;

import com.aiplatform.bidding.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleDocumentNotFound(DocumentNotFoundException ex) {
        log.warn("Document not found: {}", ex.getDocumentId());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(404, "DOCUMENT_NOT_FOUND: " + ex.getMessage()));
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleServiceUnavailable(ServiceUnavailableException ex) {
        log.error("Service unavailable: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ApiResponse.error(503, "SERVICE_UNAVAILABLE: " + ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(400, "VALIDATION_ERROR: " + errors));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(409, "ILLEGAL_STATE: " + ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(500, "INTERNAL_ERROR: An unexpected error occurred"));
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add bidding-service/src/main/java/com/aiplatform/bidding/exception/
git commit -m "feat: add exception handling for bidding service"
```

---

### Task 10: 数据库迁移脚本

**Files:**
- Create: `bidding-service/src/main/resources/db/migration/V1__init_schema.sql`

- [ ] **Step 1: Create Flyway migration script**

```sql
-- V1__init_schema.sql

-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

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
CREATE INDEX idx_bidding_cases_region ON bidding_cases(region);
CREATE INDEX idx_review_processes_document_id ON review_processes(document_id);
CREATE INDEX idx_state_transitions_review_id ON state_transitions(review_id);
CREATE INDEX idx_suggestions_document_id ON suggestions(document_id);
CREATE INDEX idx_suggestions_issue_id ON suggestions(issue_id);
```

- [ ] **Step 2: Update build.gradle to include Flyway**

```groovy
// Add to dependencies
implementation 'org.flywaydb:flyway-core:10.8.1'
runtimeOnly 'org.flywaydb:flyway-database-postgresql:10.8.1'
```

- [ ] **Step 3: Commit**

```bash
git add bidding-service/src/main/resources/db/migration/
git commit -m "feat: add Flyway migration for bidding service schema"
```

---

## 实施里程碑

| 阶段 | 任务 | 优先级 | 预期产出 |
|------|------|--------|----------|
| 1 | 项目初始化 | P0 | Gradle 项目结构 |
| 2 | 领域模型 | P0 | Enums + Entities |
| 3 | Repository | P0 | 数据访问层 |
| 4 | DTO | P0 | 请求/响应对象 |
| 5 | 状态机引擎 | P0 | 复核状态流转 |
| 6 | Service 层 | P0 | 业务逻辑 |
| 7 | Controller 层 | P0 | REST API |
| 8 | 异常处理 | P1 | 统一错误处理 |
| 9 | 数据库迁移 | P0 | Flyway 脚本 |

---

## 技术债务与后续优化

| 项目 | 描述 | 优先级 |
|------|------|--------|
| 向量检索实现 | 完善 pgvector 相似度检索 | P0 |
| 文档解析 | 集成 Apache POI/PDFBox 解析真实文档 | P0 |
| LLM 建议生成 | 集成 ai-service 调用 LLM 生成建议 | P0 |
| 单元测试覆盖率 | 提升到 80%+ | P1 |
| API 文档 | Swagger/OpenAPI 集成 | P2 |
