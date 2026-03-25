<template>
  <div class="report-detail">
    <header class="app-header">
      <h1>标书合规性检查系统</h1>
      <nav class="header-nav">
        <el-button type="info" plain @click="$router.push('/history')">
          <el-icon><Clock /></el-icon> 返回列表
        </el-button>
        <el-button type="primary" @click="$router.push('/')">
          <el-icon><Plus /></el-icon> 新建检查
        </el-button>
      </nav>
    </header>

    <main class="detail-content">
      <div v-if="loading" class="loading-container">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <div v-else-if="error" class="error-container">
        <el-alert type="error" :title="error" show-icon :closable="false" />
        <el-button type="primary" style="margin-top: 16px;" @click="fetchReport">重试</el-button>
      </div>

      <StepReport v-else-if="report" :report="report" />
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Loading, Clock, Plus } from '@element-plus/icons-vue'
import { getReport } from '../api/bidding'
import StepReport from '../components/steps/StepReport.vue'

const route = useRoute()
const report = ref(null)
const loading = ref(false)
const error = ref(null)

async function fetchReport() {
  const id = route.params.id
  if (!id) {
    error.value = '报告ID不能为空'
    return
  }

  loading.value = true
  error.value = null

  try {
    const response = await getReport(id)
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

onMounted(() => {
  fetchReport()
})
</script>

<style scoped>
.report-detail {
  min-height: 100vh;
}

.detail-content {
  max-width: 1200px;
  margin: 32px auto;
  padding: 0 24px;
}

.loading-container {
  padding: 60px 24px;
  background: white;
  border-radius: 8px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: var(--text-secondary);
}

.error-container {
  padding: 24px;
  background: white;
  border-radius: 8px;
}
</style>
