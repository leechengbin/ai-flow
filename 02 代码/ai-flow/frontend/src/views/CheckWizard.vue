<template>
  <div class="check-wizard">
    <header class="app-header">
      <h1>标书合规性检查系统</h1>
      <nav class="header-nav">
        <el-button type="info" plain @click="$router.push('/history')">
          <el-icon><Clock /></el-icon> 历史报告
        </el-button>
      </nav>
    </header>

    <main class="wizard-content">
      <el-steps :active="currentStep" finish-status="success" class="wizard-steps">
        <el-step title="上传文件" description="上传招标文件和投标文件" />
        <el-step title="检查中" description="正在解析和匹配条款" />
        <el-step title="查看报告" description="查看检查结果和建议" />
      </el-steps>

      <div class="step-content">
        <StepUpload v-if="currentStep === 0" @start-check="handleStartCheck" />
        <StepProcessing v-else-if="currentStep === 1" :check-id="checkId" @check-complete="handleCheckComplete" @check-error="handleCheckError" />
        <StepReport v-else-if="currentStep === 2" :report="report" />
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import StepUpload from '../components/steps/StepUpload.vue'
import StepProcessing from '../components/steps/StepProcessing.vue'
import StepReport from '../components/steps/StepReport.vue'

const currentStep = ref(0)
const checkId = ref(null)
const report = ref(null)

function handleStartCheck(result) {
  checkId.value = result.checkId || result.data?.checkId
  report.value = result
  currentStep.value = 1
}

function handleCheckComplete(reportData) {
  report.value = reportData
  currentStep.value = 2
}

function handleCheckError(error) {
  currentStep.value = 0
}
</script>

<style scoped>
.check-wizard {
  min-height: 100vh;
}

.wizard-content {
  max-width: 1200px;
  margin: 32px auto;
  padding: 0 24px;
}

.wizard-steps {
  margin-bottom: 32px;
  padding: 24px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.step-content {
  min-height: 400px;
}
</style>
