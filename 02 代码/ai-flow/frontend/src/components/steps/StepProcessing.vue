<template>
  <div class="step-processing">
    <div class="card">
      <div class="processing-content">
        <div class="processing-animation">
          <el-icon class="is-loading" :size="64"><Loading /></el-icon>
        </div>

        <h2 class="processing-title">正在检查标书...</h2>
        <p class="processing-status">{{ statusMessage }}</p>

        <div class="progress-container">
          <el-progress
            :percentage="progress"
            :status="progressStatus"
            :stroke-width="12"
          />
        </div>

        <div class="stage-list">
          <div
            v-for="(stage, index) in stages"
            :key="stage.name"
            class="stage-item"
            :class="{ active: stage.active, completed: stage.completed }"
          >
            <div class="stage-indicator">
              <el-icon v-if="stage.completed"><Check /></el-icon>
              <el-icon v-else-if="stage.active"><Loading /></el-icon>
              <span v-else>{{ index + 1 }}</span>
            </div>
            <span class="stage-name">{{ stage.label }}</span>
          </div>
        </div>

        <div v-if="error" class="error-section">
          <el-alert type="error" :title="error" show-icon :closable="false" />
          <el-button type="primary" style="margin-top: 16px;" @click="retry">
            重试
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { Loading, Check } from '@element-plus/icons-vue'
import { getReport } from '../../api/bidding'

const props = defineProps({
  checkId: {
    type: String,
    required: true
  }
})

const emit = defineEmits(['check-complete', 'check-error'])

const progress = ref(0)
const progressStatus = ref('')
const statusMessage = ref('准备开始...')
const error = ref(null)
const pollingInterval = ref(null)

const stages = ref([
  { name: 'parse', label: '解析文档', active: false, completed: false },
  { name: 'extract', label: '提取条款', active: false, completed: false },
  { name: 'match', label: '条款匹配', active: false, completed: false },
  { name: 'format', label: '格式检查', active: false, completed: false },
  { name: 'report', label: '生成报告', active: false, completed: false }
])

const stageProgress = {
  parse: { start: 0, end: 20 },
  extract: { start: 20, end: 40 },
  match: { start: 40, end: 70 },
  format: { start: 70, end: 85 },
  report: { start: 85, end: 100 }
}

function updateStage(stageName) {
  stages.value.forEach(stage => {
    if (stage.name === stageName) {
      stage.active = true
      stage.completed = false
    } else if (stage.name !== stageName) {
      stage.active = false
    }
  })

  const stage = stages.value.find(s => s.name === stageName)
  if (stage) {
    statusMessage.value = `正在${stage.label}...`
    progress.value = stageProgress[stageName].start
  }
}

function completeStage(stageName) {
  const stage = stages.value.find(s => s.name === stageName)
  if (stage) {
    stage.completed = true
    stage.active = false
    progress.value = stageProgress[stageName].end
  }
}

async function pollForResult() {
  try {
    const response = await getReport(props.checkId)

    if (response.data.success) {
      completeStage('report')
      progress.value = 100
      progressStatus.value = 'success'
      statusMessage.value = '检查完成！'

      setTimeout(() => {
        emit('check-complete', response.data.data)
      }, 500)
    }
  } catch (err) {
    if (err.response?.status === 404) {
      return
    }
    error.value = err.response?.data?.error || '检查失败，请重试'
    progressStatus.value = 'exception'
    emit('check-error', error.value)
    stopPolling()
  }
}

function startPolling() {
  let currentStageIndex = 0
  const stageNames = ['parse', 'extract', 'match', 'format', 'report']

  function animateStage() {
    if (currentStageIndex < stageNames.length) {
      const stageName = stageNames[currentStageIndex]
      updateStage(stageName)

      setTimeout(() => {
        completeStage(stageName)
        currentStageIndex++

        if (currentStageIndex < stageNames.length) {
          setTimeout(animateStage, 800)
        }
      }, 1500)
    }
  }

  animateStage()

  pollingInterval.value = setInterval(pollForResult, 2000)
}

function stopPolling() {
  if (pollingInterval.value) {
    clearInterval(pollingInterval.value)
    pollingInterval.value = null
  }
}

function retry() {
  error.value = null
  progressStatus.value = ''
  progress.value = 0
  statusMessage.value = '准备开始...'
  stages.value.forEach(s => {
    s.active = false
    s.completed = false
  })
  startPolling()
}

onMounted(() => {
  startPolling()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.step-processing {
  padding: 24px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.processing-content {
  max-width: 600px;
  margin: 0 auto;
  text-align: center;
  padding: 40px 0;
}

.processing-animation {
  margin-bottom: 24px;
  color: var(--accent-color);
}

.processing-title {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
}

.processing-status {
  margin: 0 0 32px 0;
  font-size: 14px;
  color: var(--text-secondary);
}

.progress-container {
  margin-bottom: 40px;
  padding: 0 40px;
}

.stage-list {
  display: flex;
  justify-content: center;
  gap: 32px;
  margin-bottom: 40px;
}

.stage-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.stage-indicator {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--border-color);
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s ease;
}

.stage-item.active .stage-indicator {
  background: var(--accent-color);
  color: white;
}

.stage-item.completed .stage-indicator {
  background: var(--success-color);
  color: white;
}

.stage-name {
  font-size: 12px;
  color: var(--text-secondary);
}

.stage-item.active .stage-name {
  color: var(--accent-color);
  font-weight: 600;
}

.stage-item.completed .stage-name {
  color: var(--success-color);
}

.error-section {
  margin-top: 24px;
}
</style>
