<template>
  <span class="risk-badge" :class="levelClass">
    <el-icon v-if="showIcon" class="risk-icon">
      <WarningFilled v-if="level === 'HIGH' || level === 'CRITICAL'" />
      <SuccessFilled v-else-if="level === 'LOW'" />
      <InfoFilled v-else />
    </el-icon>
    {{ label }}
  </span>
</template>

<script setup>
import { computed } from 'vue'
import { WarningFilled, SuccessFilled, InfoFilled } from '@element-plus/icons-vue'
import { getRiskLevelLabel } from '../../utils/formatters'

const props = defineProps({
  level: {
    type: String,
    required: true
  },
  showIcon: {
    type: Boolean,
    default: true
  }
})

const levelClass = computed(() => {
  const map = {
    'LOW': 'low',
    'MEDIUM': 'medium',
    'HIGH': 'high',
    'CRITICAL': 'critical'
  }
  return map[props.level] || 'low'
})

const label = computed(() => getRiskLevelLabel(props.level))
</script>

<style scoped>
.risk-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 600;
}

.risk-badge.low {
  background-color: #c6f6d5;
  color: #22543d;
}

.risk-badge.medium {
  background-color: #fefcbf;
  color: #744210;
}

.risk-badge.high {
  background-color: #fed7d7;
  color: #822727;
}

.risk-badge.critical {
  background-color: #feb2b2;
  color: #742a2a;
}

.risk-icon {
  font-size: 14px;
}
</style>
