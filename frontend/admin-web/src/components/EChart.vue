<template>
  <div ref="chartRef" class="echart" :style="{ height }"></div>
</template>

<script setup>
// ECharts 通用封装：负责 init / setOption / resize / dispose
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'

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
  chart = echarts.init(chartRef.value)
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
