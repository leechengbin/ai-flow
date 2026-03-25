<template>
  <div class="issue-bar-chart">
    <div ref="chartRef" class="chart-container"></div>
    <div v-if="!hasData" class="no-data">暂无数据</div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, onUnmounted } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  data: Object,
  issues: Array
})

const chartRef = ref(null)
let chart = null
const hasData = ref(false)

function getChartData() {
  if (props.issues && props.issues.length > 0) {
    const typeCount = {}
    props.issues.forEach(issue => {
      const type = issue.type || '未知'
      typeCount[type] = (typeCount[type] || 0) + 1
    })

    return {
      labels: Object.keys(typeCount),
      values: Object.values(typeCount)
    }
  }

  if (props.data?.labels?.length > 0) {
    return {
      labels: props.data.labels,
      values: props.data.data
    }
  }

  return null
}

function initChart() {
  if (!chartRef.value) return

  chart = echarts.init(chartRef.value)
  updateChart()
}

function updateChart() {
  if (!chart) return

  const chartData = getChartData()

  if (chartData && chartData.labels.length > 0) {
    hasData.value = true
    const option = {
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow'
        }
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: chartData.labels,
        axisLabel: {
          fontSize: 11,
          rotate: 30
        }
      },
      yAxis: {
        type: 'value',
        minInterval: 1
      },
      series: [
        {
          type: 'bar',
          data: chartData.values,
          itemStyle: {
            color: function(params) {
              const colors = ['#e53e3e', '#d69e2e', '#3182ce', '#38a169', '#805ad5']
              return colors[params.dataIndex % colors.length]
            },
            borderRadius: [4, 4, 0, 0]
          },
          barWidth: '50%'
        }
      ]
    }
    chart.setOption(option)
  } else {
    hasData.value = false
  }
}

watch(() => [props.data, props.issues], () => {
  updateChart()
}, { deep: true })

onMounted(() => {
  initChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (chart) {
    chart.dispose()
  }
})

function handleResize() {
  if (chart) {
    chart.resize()
  }
}
</script>

<style scoped>
.issue-bar-chart {
  position: relative;
  width: 100%;
  height: 250px;
}

.chart-container {
  width: 100%;
  height: 100%;
}

.no-data {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: var(--text-secondary);
  font-size: 14px;
}
</style>
