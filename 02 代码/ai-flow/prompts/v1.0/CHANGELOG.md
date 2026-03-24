# Changelog

All notable changes to agent prompts will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [1.0.0] - 2026-03-21

### Added
- Initial version with 5 agent prompts:
  - Project Monitor Agent (项目监控Agent)
  - Bidding Check Agent (标书检查Agent)
  - Compliance Audit Agent (合规性审计Agent)
  - Risk Monitor Agent (风险监控Agent)
  - Knowledge Base Agent (知识库Agent)

### Agent Specifications

#### Project Monitor Agent
- Health score (0-100)
- Deviation analysis with planned vs actual
- Risk early warnings with [低]/[中]/[高]/[紧急] levels
- Trend analysis

#### Bidding Check Agent
- Qualification verification
- Compliance checking (★ marked clauses)
- Format review
- Pricing validation
- Comprehensive scoring (0-100)

#### Compliance Audit Agent
- Legal citation requirements
- Chain of Thought reasoning
- Remediation tracking
- ISO9001/公司制度 compliance

#### Risk Monitor Agent
- Risk radar across 3 dimensions (技术/管理/外部)
- Chain of Thought analysis
- Probability and impact assessment
- 2-week trend prediction

#### Knowledge Base Agent
- Source attribution mandatory
- No fabrication allowed
- Related reference linking
- Extended learning suggestions
