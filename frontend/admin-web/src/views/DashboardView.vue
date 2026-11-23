<template>
  <div v-loading="loading">
    <!-- KPI 卡片区 -->
    <el-row :gutter="16">
      <el-col v-for="k in kpiList" :key="k.label" :span="4">
        <el-card shadow="never" class="kpi-card">
          <div class="kpi-label">{{ k.label }}</div>
          <div class="kpi-value">{{ k.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 自动刷新开关 -->
    <div class="toolbar">
      <span class="date">数据日期：{{ overview?.date || '-' }}</span>
      <el-switch v-model="autoRefresh" active-text="自动刷新（5s）" />
    </div>

    <!-- 图表区 -->
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never" header="分时报到趋势">
          <EChart :option="trendOption" height="300px" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" header="学院报到进度">
          <EChart :option="collegeOption" height="300px" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" header="问答命中分布">
          <EChart :option="hitOption" height="300px" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" header="热门问题 TOP5">
          <EChart :option="hotOption" height="300px" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
// 数据看板：KPI 卡 + ECharts 图表，支持 5 秒自动刷新
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { getOverview } from '@/api/dashboard'
import EChart from '@/components/EChart.vue'

const overview = ref(null)
const loading = ref(false)
const autoRefresh = ref(false)
let timer = null

// KPI 卡：报到人数/总人数、报到率、问答次数、缓存命中率、平均响应、未解答
const kpiList = computed(() => {
  const k = overview.value?.kpi || {}
  return [
    { label: '今日报到人数', value: `${k.checkinCount ?? 0} / ${k.totalStudents ?? 0}` },
    { label: '报到率', value: pct(k.checkinRate) },
    { label: '问答次数', value: k.qaCount ?? 0 },
    { label: '缓存命中率', value: pct(k.cacheHitRate) },
    { label: '平均响应 (ms)', value: k.avgCostMs ?? 0 },
    { label: '未解答数', value: k.unanswered ?? 0 },
  ]
})

function pct(v) {
  if (v === undefined || v === null) return '-'
  return `${(v * 100).toFixed(1)}%`
}

// 分时报到趋势：补全 0..23 小时
const trendOption = computed(() => {
  const trend = overview.value?.trend || []
  const countMap = Object.fromEntries(trend.map((t) => [t.hour, t.count]))
  const hours = Array.from({ length: 24 }, (_, i) => i)
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: hours.map((h) => `${h}时`) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '报到人数',
        type: 'line',
        smooth: true,
        areaStyle: { opacity: 0.2 },
        data: hours.map((h) => countMap[h] ?? 0),
      },
    ],
  }
})

// 学院报到进度：横向条形图
const collegeOption = computed(() => {
  const colleges = overview.value?.colleges || []
  const sorted = [...colleges].sort((a, b) => a.rate - b.rate)
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      valueFormatter: (v) => `${v}%`,
    },
    grid: { left: 100, right: 40, top: 20, bottom: 30 },
    xAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } },
    yAxis: { type: 'category', data: sorted.map((c) => c.college) },
    series: [
      {
        name: '报到率',
        type: 'bar',
        data: sorted.map((c) => +(c.rate * 100).toFixed(1)),
        label: { show: true, position: 'right', formatter: '{c}%' },
      },
    ],
  }
})

// 问答命中分布：环形饼图（缓存/知识库/未命中）
const hitOption = computed(() => {
  const h = overview.value?.qaHit || {}
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        name: '命中来源',
        type: 'pie',
        radius: ['45%', '70%'],
        itemStyle: { borderRadius: 6 },
        label: { formatter: '{b}: {c}' },
        data: [
          { name: '缓存命中', value: h.cache ?? 0 },
          { name: '知识库命中', value: h.lib ?? 0 },
          { name: '未命中', value: h.miss ?? 0 },
        ],
      },
    ],
  }
})

// 热门问题 TOP5：柱状图
const hotOption = computed(() => {
  const hot = (overview.value?.hotQuestions || []).slice(0, 5)
  const items = [...hot].reverse()
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 160, right: 40, top: 20, bottom: 30 },
    xAxis: { type: 'value', minInterval: 1 },
    yAxis: {
      type: 'category',
      data: items.map((q) => (q.question.length > 12 ? `${q.question.slice(0, 12)}…` : q.question)),
    },
    series: [{ name: '命中次数', type: 'bar', data: items.map((q) => q.hitCount) }],
  }
})

async function load() {
  loading.value = true
  try {
    const res = await getOverview()
    overview.value = res.data
  } catch {
    // 错误提示由拦截器统一处理
  } finally {
    loading.value = false
  }
}

// 自动刷新开关：开启后每 5 秒拉一次 overview
watch(autoRefresh, (on) => {
  stopTimer()
  if (on) {
    timer = setInterval(load, 5000)
  }
})

function stopTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

onMounted(load)

// 切页面时清理定时器
onBeforeUnmount(stopTimer)
</script>

<style scoped>
.kpi-card {
  text-align: center;
}
.kpi-label {
  color: #909399;
  font-size: 13px;
  margin-bottom: 8px;
}
.kpi-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 16px 0;
}
.date {
  color: #909399;
  font-size: 13px;
}
.el-card {
  margin-bottom: 16px;
}
</style>
