<template>
  <div class="coverage-pie-chart">
    <div ref="chartRef" class="chart-container"></div>
    <div v-if="!hasData" class="no-data">暂无数据</div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, onUnmounted } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  data: Object,
  coverage: Object
})

const chartRef = ref(null)
let chart = null

const hasData = ref(false)

function getChartData() {
  if (props.coverage) {
    return {
      matched: props.coverage.matchedClauses || 0,
      partial: props.coverage.partialClauses || 0,
      missing: props.coverage.missingClauses || 0
    }
  }

  if (props.data?.labels && props.data?.data) {
    hasData.value = props.data.labels.length > 0
    return null
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

  const coverageData = getChartData()

  let option
  if (coverageData) {
    hasData.value = true
    option = {
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {c} ({d}%)'
      },
      legend: {
        orient: 'vertical',
        left: 'left',
        top: 'middle',
        textStyle: {
          fontSize: 12
        }
      },
      series: [
        {
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 4,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: {
            show: false
          },
          emphasis: {
            label: {
              show: true,
              fontSize: 14,
              fontWeight: 'bold'
            }
          },
          data: [
            { value: coverageData.matched, name: '已匹配', itemStyle: { color: '#38a169' } },
            { value: coverageData.partial, name: '部分匹配', itemStyle: { color: '#d69e2e' } },
            { value: coverageData.missing, name: '缺失', itemStyle: { color: '#e53e3e' } }
          ]
        }
      ]
    }
  } else if (props.data?.labels?.length > 0) {
    hasData.value = true
    option = {
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {c} ({d}%)'
      },
      legend: {
        orient: 'vertical',
        left: 'left',
        top: 'middle',
        textStyle: {
          fontSize: 12
        }
      },
      series: [
        {
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 4,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: {
            show: false
          },
          emphasis: {
            label: {
              show: true,
              fontSize: 14,
              fontWeight: 'bold'
            }
          },
          data: props.data.labels.map((label, index) => ({
            name: label,
            value: props.data.data[index]
          }))
        }
      ]
    }
  } else {
    hasData.value = false
  }

  if (hasData.value && option) {
    chart.setOption(option)
  }
}

watch(() => [props.data, props.coverage], () => {
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
.coverage-pie-chart {
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
