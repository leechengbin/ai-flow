<template>
  <div class="format-check-summary">
    <el-row :gutter="16">
      <el-col :span="8">
        <div class="check-item">
          <h4>完整性检查</h4>
          <div class="check-result">
            <el-tag :type="format.completeness?.passed ? 'success' : 'danger'" size="small">
              {{ format.completeness?.passed ? '通过' : '未通过' }}
            </el-tag>
          </div>
          <ul v-if="format.completeness?.issues?.length" class="issue-list">
            <li v-for="(issue, idx) in format.completeness.issues" :key="idx">
              {{ issue }}
            </li>
          </ul>
        </div>
      </el-col>

      <el-col :span="8">
        <div class="check-item">
          <h4>签名检查</h4>
          <div class="check-result">
            <el-tag :type="format.signature?.passed ? 'success' : 'danger'" size="small">
              {{ format.signature?.passed ? '通过' : '未通过' }}
            </el-tag>
          </div>
          <ul v-if="format.signature?.issues?.length" class="issue-list">
            <li v-for="(issue, idx) in format.signature.issues" :key="idx">
              {{ issue }}
            </li>
          </ul>
        </div>
      </el-col>

      <el-col :span="8">
        <div class="check-item">
          <h4>日期检查</h4>
          <div class="check-result">
            <el-tag :type="format.date?.passed ? 'success' : 'danger'" size="small">
              {{ format.date?.passed ? '通过' : '未通过' }}
            </el-tag>
          </div>
          <ul v-if="format.date?.issues?.length" class="issue-list">
            <li v-for="(issue, idx) in format.date.issues" :key="idx">
              {{ issue }}
            </li>
          </ul>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
defineProps({
  format: {
    type: Object,
    required: true
  }
})
</script>

<style scoped>
.format-check-summary {
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
}

.check-item {
  padding: 16px;
  background: white;
  border-radius: 6px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.check-item h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #2d3748;
}

.check-result {
  margin-bottom: 12px;
}

.issue-list {
  margin: 0;
  padding-left: 20px;
  font-size: 13px;
  color: #e53e3e;
}

.issue-list li {
  margin-bottom: 4px;
}
</style>
