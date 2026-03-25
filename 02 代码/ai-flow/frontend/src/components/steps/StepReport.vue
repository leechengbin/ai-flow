<template>
  <div class="step-report">
    <div class="report-header">
      <h2 class="report-title">检查报告</h2>
      <div class="report-meta">
        <span>报告ID: {{ report.reportId }}</span>
        <span>生成时间: {{ formatDate(report.generatedAt) }}</span>
      </div>
    </div>

    <el-row :gutter="16" class="summary-cards">
      <el-col :span="6">
        <div class="stat-card">
          <RiskBadge :level="report.summary?.riskLevel" />
          <div class="stat-label">风险等级</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-value">{{ ((report.summary?.totalScore || 0) * 100).toFixed(0) }}</div>
          <div class="stat-label">综合得分</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-value">{{ ((report.summary?.coverageRate || 0) * 100).toFixed(1) }}%</div>
          <div class="stat-label">条款覆盖率</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-value">{{ ((report.summary?.formatScore || 0) * 100).toFixed(0) }}%</div>
          <div class="stat-label">格式得分</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :span="8">
        <div class="card chart-card">
          <h3 class="card-title">条款覆盖率</h3>
          <CoveragePieChart :data="report.visualization?.coverageChart" :coverage="report.coverage" />
        </div>
      </el-col>
      <el-col :span="8">
        <div class="card chart-card">
          <h3 class="card-title">问题分布</h3>
          <IssueBarChart :data="report.visualization?.issueDistribution" :issues="report.issues" />
        </div>
      </el-col>
      <el-col :span="8">
        <div class="card chart-card">
          <h3 class="card-title">风险指数</h3>
          <RiskGauge :data="report.visualization?.riskGauge" :score="report.summary?.totalScore" />
        </div>
      </el-col>
    </el-row>

    <div class="card issue-section">
      <h3 class="card-title">问题详情</h3>
      <IssueTable :issues="report.issues || []" />
    </div>

    <div v-if="report.format" class="card format-section">
      <h3 class="card-title">格式检查结果</h3>
      <FormatCheckSummary :format="report.format" />
    </div>
  </div>
</template>

<script setup>
import { formatDate } from '../../utils/formatters'
import RiskBadge from '../common/RiskBadge.vue'
import IssueTable from '../common/IssueTable.vue'
import CoveragePieChart from '../charts/CoveragePieChart.vue'
import IssueBarChart from '../charts/IssueBarChart.vue'
import RiskGauge from '../charts/RiskGauge.vue'
import FormatCheckSummary from '../common/FormatCheckSummary.vue'

defineProps({
  report: {
    type: Object,
    required: true
  }
})
</script>

<style scoped>
.step-report {
  padding: 24px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color);
}

.report-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
}

.report-meta {
  display: flex;
  gap: 24px;
  font-size: 13px;
  color: var(--text-secondary);
}

.summary-cards {
  margin-bottom: 24px;
}

.stat-card {
  background: linear-gradient(135deg, #f8fafc 0%, #edf2f7 100%);
  border-radius: 8px;
  padding: 20px;
  text-align: center;
}

.stat-card .stat-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--primary-color);
}

.stat-card .stat-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.chart-row {
  margin-bottom: 24px;
}

.chart-card {
  height: 100%;
}

.chart-card .card-title {
  margin: 0 0 16px 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.issue-section,
.format-section {
  margin-bottom: 24px;
}

.issue-section .card-title,
.format-section .card-title {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}
</style>
