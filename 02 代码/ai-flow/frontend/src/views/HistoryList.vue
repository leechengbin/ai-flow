<template>
  <div class="history-list">
    <header class="app-header">
      <h1>标书合规性检查系统</h1>
      <nav class="header-nav">
        <el-button type="primary" @click="$router.push('/')">
          <el-icon><Plus /></el-icon> 新建检查
        </el-button>
      </nav>
    </header>

    <main class="history-content">
      <div class="search-bar">
        <el-input
          v-model="reportId"
          placeholder="请输入报告ID查询"
          style="width: 400px;"
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button :icon="Search" @click="handleSearch" />
          </template>
        </el-input>
      </div>

      <div v-if="loading" class="loading-container">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <div v-else-if="error" class="error-container">
        <el-alert type="error" :title="error" show-icon :closable="false" />
      </div>

      <div v-else-if="report" class="report-container">
        <StepReport :report="report" />
      </div>

      <div v-else class="empty-container">
        <el-empty description="请输入报告ID进行查询" />
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { Search, Plus, Loading } from '@element-plus/icons-vue'
import { getReport } from '../api/bidding'
import StepReport from '../components/steps/StepReport.vue'

const route = useRoute()
const reportId = ref(route.query.id || '')
const report = ref(null)
const loading = ref(false)
const error = ref(null)

async function handleSearch() {
  if (!reportId.value.trim()) {
    error.value = '请输入报告ID'
    return
  }

  loading.value = true
  error.value = null
  report.value = null

  try {
    const response = await getReport(reportId.value)
    if (response.data.success) {
      report.value = response.data.data
    } else {
      error.value = response.data.error || '获取报告失败'
    }
  } catch (err) {
    error.value = err.response?.data?.error || '网络错误，请稍后重试'
  } finally {
    loading.value = false
  }
}

if (reportId.value) {
  handleSearch()
}
</script>

<style scoped>
.history-list {
  min-height: 100vh;
}

.history-content {
  max-width: 1200px;
  margin: 32px auto;
  padding: 0 24px;
}

.search-bar {
  margin-bottom: 24px;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.loading-container,
.error-container,
.empty-container {
  padding: 60px 24px;
  background: white;
  border-radius: 8px;
  text-align: center;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: var(--text-secondary);
}

.report-container {
  background: white;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}
</style>
