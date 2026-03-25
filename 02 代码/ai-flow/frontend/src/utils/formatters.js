export function formatDate(dateStr) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

export function formatPercent(value) {
  if (value === null || value === undefined) return '-'
  return `${(value * 100).toFixed(1)}%`
}

export function getRiskLevelLabel(level) {
  const map = {
    'LOW': '低风险',
    'MEDIUM': '中等风险',
    'HIGH': '高风险',
    'CRITICAL': '严重风险'
  }
  return map[level] || level
}

export function getRiskLevelType(level) {
  const map = {
    'LOW': 'success',
    'MEDIUM': 'warning',
    'HIGH': 'danger',
    'CRITICAL': 'danger'
  }
  return map[level] || 'info'
}

export function getSeverityType(severity) {
  const map = {
    'ERROR': 'danger',
    'WARNING': 'warning',
    'INFO': 'info'
  }
  return map[severity] || 'info'
}
