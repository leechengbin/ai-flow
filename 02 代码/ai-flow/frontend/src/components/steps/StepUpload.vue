<template>
  <div class="step-upload">
    <div class="card">
      <h2 class="card-title">上传标书文件</h2>

      <el-row :gutter="24">
        <el-col :span="12">
          <div class="upload-section">
            <h3 class="upload-title">招标文件</h3>
            <el-upload
              ref="tenderUpload"
              class="tender-upload"
              :auto-upload="false"
              :limit="1"
              accept=".pdf,.docx"
              :on-change="handleTenderChange"
              :on-remove="handleTenderRemove"
              drag
            >
              <div class="upload-zone">
                <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
                <p>将招标文件拖拽到此处，或 <em>点击上传</em></p>
                <p class="upload-hint">支持 PDF、DOCX 格式</p>
              </div>
            </el-upload>
            <div v-if="tenderFile" class="file-info">
              <el-tag size="small">{{ tenderFile.name }}</el-tag>
            </div>
          </div>
        </el-col>

        <el-col :span="12">
          <div class="upload-section">
            <h3 class="upload-title">投标文件</h3>
            <el-upload
              ref="biddingUpload"
              class="bidding-upload"
              :auto-upload="false"
              :limit="1"
              accept=".pdf,.docx"
              :on-change="handleBiddingChange"
              :on-remove="handleBiddingRemove"
              drag
            >
              <div class="upload-zone">
                <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
                <p>将投标文件拖拽到此处，或 <em>点击上传</em></p>
                <p class="upload-hint">支持 PDF、DOCX 格式</p>
              </div>
            </el-upload>
            <div v-if="biddingFile" class="file-info">
              <el-tag size="small">{{ biddingFile.name }}</el-tag>
            </div>
          </div>
        </el-col>
      </el-row>

      <div class="options-section">
        <h3 class="options-title">检查选项</h3>
        <el-checkbox v-model="options.checkCoverage">条款覆盖率检查</el-checkbox>
        <el-checkbox v-model="options.checkFormat">格式完整性检查</el-checkbox>
        <el-checkbox v-model="options.checkStarred">星标条款检查</el-checkbox>
      </div>

      <div class="action-section">
        <el-button
          type="primary"
          size="large"
          :disabled="!canStartCheck"
          :loading="uploading"
          @click="startCheck"
        >
          开始检查
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { UploadFilled } from '@element-plus/icons-vue'
import { checkBidding } from '../../api/bidding'

const emit = defineEmits(['start-check'])

const tenderFile = ref(null)
const biddingFile = ref(null)
const uploading = ref(false)

const options = ref({
  checkCoverage: true,
  checkFormat: true,
  checkStarred: true
})

const canStartCheck = computed(() => {
  return tenderFile.value && biddingFile.value && !uploading.value
})

function handleTenderChange(file) {
  tenderFile.value = file.raw
}

function handleTenderRemove() {
  tenderFile.value = null
}

function handleBiddingChange(file) {
  biddingFile.value = file.raw
}

function handleBiddingRemove() {
  biddingFile.value = null
}

async function startCheck() {
  if (!canStartCheck.value) return

  uploading.value = true

  try {
    const formData = new FormData()
    formData.append('tenderFile', tenderFile.value)
    formData.append('biddingFile', biddingFile.value)
    formData.append('checkCoverage', options.value.checkCoverage)
    formData.append('checkFormat', options.value.checkFormat)
    formData.append('checkStarred', options.value.checkStarred)

    const response = await checkBidding(formData)

    if (response.data.success) {
      emit('start-check', response.data.data)
    } else {
      ElMessage.error(response.data.error || '检查启动失败')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.error || '网络错误，请稍后重试')
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.step-upload {
  padding: 24px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.card-title {
  margin: 0 0 24px 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.upload-section {
  margin-bottom: 24px;
}

.upload-title {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.file-info {
  margin-top: 8px;
}

.upload-hint {
  font-size: 12px;
  color: var(--text-secondary);
}

.options-section {
  margin-top: 24px;
  padding: 20px;
  background: var(--bg-color);
  border-radius: 8px;
}

.options-title {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.action-section {
  margin-top: 32px;
  text-align: center;
}
</style>
