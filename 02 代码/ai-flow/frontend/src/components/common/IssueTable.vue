<template>
  <div class="issue-table">
    <el-table
      v-if="issues.length > 0"
      :data="issues"
      stripe
      border
      :expand-row-keys="expandedRows"
      row-key="issueId"
      @expand-change="handleExpandChange"
    >
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="issue-detail">
            <div class="detail-row">
              <span class="detail-label">条款编号:</span>
              <span class="detail-value">{{ row.clauseNumber || '-' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">问题类型:</span>
              <span class="detail-value">{{ row.type || '-' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">严重程度:</span>
              <el-tag :type="getSeverityType(row.severity)" size="small">
                {{ row.severity || '-' }}
              </el-tag>
            </div>
            <div class="detail-row">
              <span class="detail-label">是否星标:</span>
              <el-tag :type="row.isStarred ? 'warning' : 'info'" size="small">
                {{ row.isStarred ? '是' : '否' }}
              </el-tag>
            </div>
            <div class="detail-row">
              <span class="detail-label">消项风险:</span>
              <el-tag :type="row.eliminationRisk ? 'danger' : 'success'" size="small">
                {{ row.eliminationRisk ? '是' : '否' }}
              </el-tag>
            </div>
            <div class="detail-row full-width">
              <span class="detail-label">问题描述:</span>
              <p class="detail-value description">{{ row.description || '-' }}</p>
            </div>
            <div class="detail-row full-width">
              <span class="detail-label">上下文:</span>
              <p class="detail-value context">{{ row.contextText || '-' }}</p>
            </div>
            <div class="detail-row full-width" v-if="row.suggestion">
              <span class="detail-label">修改建议:</span>
              <p class="detail-value suggestion">{{ row.suggestion }}</p>
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="clauseNumber" label="条款编号" width="120" />
      <el-table-column prop="title" label="标题" min-width="150" />
      <el-table-column prop="type" label="类型" width="100" />
      <el-table-column prop="severity" label="严重程度" width="100">
        <template #default="{ row }">
          <el-tag :type="getSeverityType(row.severity)" size="small">
            {{ row.severity || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="星标" width="70">
        <template #default="{ row }">
          <el-tag v-if="row.isStarred" type="warning" size="small">★</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="消项风险" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.eliminationRisk" type="danger" size="small">是</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-else description="未发现问题" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { getSeverityType } from '../../utils/formatters'

defineProps({
  issues: {
    type: Array,
    default: () => []
  }
})

const expandedRows = ref([])

function handleExpandChange(row, expanded) {
  if (expanded) {
    if (!expandedRows.value.includes(row.issueId)) {
      expandedRows.value.push(row.issueId)
    }
  } else {
    expandedRows.value = expandedRows.value.filter(id => id !== row.issueId)
  }
}
</script>

<style scoped>
.issue-table {
  width: 100%;
}

.issue-detail {
  padding: 16px 24px;
  background: #f8fafc;
  border-radius: 4px;
}

.detail-row {
  display: inline-flex;
  align-items: center;
  margin-right: 24px;
  margin-bottom: 8px;
}

.detail-row.full-width {
  display: flex;
  width: 100%;
  flex-direction: column;
}

.detail-label {
  font-weight: 600;
  color: #4a5568;
  margin-right: 8px;
  min-width: 80px;
}

.detail-value {
  margin: 0;
  color: #2d3748;
}

.detail-value.description,
.detail-value.context,
.detail-value.suggestion {
  padding: 12px;
  background: white;
  border-radius: 4px;
  margin-top: 4px;
  line-height: 1.6;
  font-size: 13px;
}

.detail-value.suggestion {
  border-left: 3px solid #38a169;
  background: #f0fff4;
}
</style>
