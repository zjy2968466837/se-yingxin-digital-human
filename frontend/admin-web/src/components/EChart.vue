<template>
  <div ref="chartRef" class="echart" :style="{ height }"></div>
</template>

<script setup>
// ECharts 通用封装：负责 init / setOption / resize / dispose
// 按需注册：只引入实际用到的图表与组件，避免把整个 echarts（约 1MB）打进包里
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { init, use } from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([BarChart, LineChart, PieChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const props = defineProps({
  option: { type: Object, required: true },
  height: { type: String, default: '320px' },
})

const chartRef = ref(null)
let chart = null

function handleResize() {
  chart && chart.resize()
}

function render() {
  if (chart && props.option) {
    chart.setOption(props.option, true)
  }
}

onMounted(() => {
  chart = init(chartRef.value)
  render()
  window.addEventListener('resize', handleResize)
})

watch(() => props.option, render, { deep: true })

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (chart) {
    chart.dispose()
    chart = null
  }
})
</script>

<style scoped>
.echart {
  width: 100%;
}
</style>
