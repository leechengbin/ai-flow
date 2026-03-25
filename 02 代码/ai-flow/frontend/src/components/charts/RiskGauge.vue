<template>
  <div class="risk-gauge">
    <div ref="chartRef" class="chart-container"></div>
    <div v-if="!hasData" class="no-data">暂无数据</div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, onUnmounted } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  data: Object,
  score: Number
})

const chartRef = ref(null)
let chart = null
const hasData = ref(false)

function initChart() {
  if (!chartRef.value) return

  chart = echarts.init(chartRef.value)
  updateChart()
}

function updateChart() {
  if (!chart) return

  let gaugeValue = 0
  let thresholds = []

  if (props.data) {
    hasData.value = true
    gaugeValue = props.data.value || 0
    thresholds = (props.data.thresholds || []).map(t => ({
      min: t.from,
      max: t.to,
      color: t.color,
      label: `${t.from}-${t.to}`
    }))
  } else if (props.score !== undefined && props.score !== null) {
    hasData.value = true
    gaugeValue = props.score * 100

    thresholds = [
      { min: 0, max: 40, color: '#e53e3e' },
      { min: 40, max: 70, color: '#d69e2e' },
      { min: 70, max: 100, color: '#38a169' }
    ]
  } else {
    hasData.value = false
    return
  }

  const option = {
    series: [
      {
        type: 'gauge',
        startAngle: 180,
        endAngle: 0,
        min: 0,
        max: 100,
        splitNumber: 5,
        radius: '100%',
        center: ['50%', '75%'],
        axisLine: {
          lineStyle: {
            width: 20,
            color: thresholds.map((t, i) => {
              const ratio = i / thresholds.length
              return [ratio, t.color]
            }).concat([[1, thresholds[thresholds.length - 1]?.color || '#38a169']])
          }
        },
        pointer: {
          icon: 'path://M12.8,0.7l12,40.1H0.7L12.8,0.7z',
          length: '50%',
          width: 10,
          offsetCenter: [0, '-10%'],
          itemStyle: {
            color: '#1a365d'
          }
        },
        axisTick: {
          length: 6,
          lineStyle: {
            color: '#718096',
            width: 1
          }
        },
        splitLine: {
          length: 10,
          lineStyle: {
            color: '#718096',
            width: 1
          }
        },
        axisLabel: {
          show: false
        },
        title: {
          show: false
        },
        detail: {
          valueAnimation: true,
          formatter: function(value) {
            return '{value|' + value.toFixed(0) + '}\n{label|风险指数}'
          },
          rich: {
            value: {
              fontSize: 28,
              fontWeight: 'bold',
              color: '#1a365d',
              lineHeight: 36
            },
            label: {
              fontSize: 12,
              color: '#718096',
              lineHeight: 18
            }
          },
          offsetCenter: [0, '0%']
        },
        data: [
          {
            value: gaugeValue
          }
        ]
      }
    ]
  }

  chart.setOption(option)
}

watch(() => [props.data, props.score], () => {
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
.risk-gauge {
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
