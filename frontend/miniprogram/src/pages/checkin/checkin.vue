<template>
  <view class="page">
    <!-- 进度总览 -->
    <view class="card">
      <view class="row-between">
        <view class="title">报到进度</view>
        <view class="muted">{{ progress.doneCount }}/{{ progress.totalCount }}</view>
      </view>
      <view class="bar">
        <view class="bar-inner" :style="{ width: (progress.percent || 0) + '%' }" />
      </view>
      <view class="percent">{{ progress.percent || 0 }}%</view>
    </view>

    <!-- 步骤列表 -->
    <view v-for="step in steps" :key="step.stepCode" class="card">
      <view class="row-between">
        <view class="step-name">{{ step.seq }}. {{ step.stepName }}</view>
        <text :class="['badge', statusClass(step.status)]">{{ statusText(step.status) }}</text>
      </view>

      <view class="info">
        <view v-if="step.location">地点：{{ step.location }}</view>
        <view v-if="step.officeHours">时间：{{ step.officeHours }}</view>
        <view v-if="step.contact">联系：{{ step.contact }}</view>
      </view>

      <view v-if="step.checkinTime" class="muted">打卡于 {{ formatTime(step.checkinTime) }}</view>

      <button
        v-if="step.status !== 2"
        class="btn"
        type="primary"
        size="mini"
        :loading="checking === step.stepCode"
        @click="doCheckin(step.stepCode)"
      >
        完成打卡
      </button>
    </view>

    <!-- 凭证 -->
    <view class="card">
      <view class="row-between">
        <view class="title">报到凭证</view>
        <view class="muted">{{ voucher ? '已签发' : '全部完成后可领取' }}</view>
      </view>
      <view v-if="voucher" class="voucher">
        <view>凭证号：{{ voucher.certNo }}</view>
        <view class="muted">有效期至 {{ formatTime(voucher.expireTime) }}</view>
        <view class="muted mono">核销码：{{ voucher.qrToken }}</view>
      </view>
      <button v-else class="btn" type="default" size="mini" :disabled="progress.percent !== 100" @click="loadVoucher">
        领取凭证
      </button>
    </view>

    <view class="foot-hint">进度与打卡记录实时来自 checkin-service</view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { listSteps, fetchProgress, checkinStep, fetchVoucher } from '@/api/checkin'
import { requireLogin } from '@/composables/requireLogin'

const steps = ref([])
const progress = ref({ steps: [], doneCount: 0, totalCount: 0, percent: 0 })
const voucher = ref(null)
const checking = ref('')

onShow(async () => {
  if (!requireLogin({ relaunch: true })) return
  await load()
})

async function load() {
  try {
    const [templates, prog] = await Promise.all([listSteps(), fetchProgress()])
    // 进度里只有状态，办理信息在模板里，按 stepCode 合并
    const statusMap = {}
    for (const s of prog.steps || []) statusMap[s.stepCode] = s
    steps.value = (templates || []).map((t) => ({ ...t, ...(statusMap[t.stepCode] || {}) }))
    progress.value = {
      doneCount: prog.doneCount || 0,
      totalCount: prog.totalCount || 0,
      percent: prog.percent || 0,
    }
    if (progress.value.percent === 100) await loadVoucher()
  } catch (e) {
    // request 已提示
  }
}

async function doCheckin(stepCode) {
  checking.value = stepCode
  try {
    const res = await checkinStep(stepCode)
    uni.showToast({ title: res && res.isDuplicated ? '该步骤已完成' : '打卡成功', icon: 'success' })
    await load()
  } catch (e) {
    // request 已提示
  } finally {
    checking.value = ''
  }
}

async function loadVoucher() {
  try {
    voucher.value = await fetchVoucher()
  } catch (e) {
    voucher.value = null
  }
}

const STATUS = { 1: ['待完成', 'pending'], 2: ['已完成', 'done'], 3: ['不适用', 'na'] }
function statusText(s) {
  return (STATUS[s] || ['待完成', 'pending'])[0]
}
function statusClass(s) {
  return (STATUS[s] || ['', 'pending'])[1]
}
function formatTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(0, 16)
}
</script>

<style scoped>
.page {
  padding: 24rpx;
  padding-bottom: 60rpx;
}
.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
}
.row-between {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.title {
  font-weight: bold;
  font-size: 30rpx;
}
.muted {
  color: #909399;
  font-size: 24rpx;
}
.bar {
  height: 16rpx;
  background: #ebeef5;
  border-radius: 8rpx;
  margin-top: 20rpx;
  overflow: hidden;
}
.bar-inner {
  height: 100%;
  background: #3a8ee6;
  transition: width 0.3s;
}
.percent {
  margin-top: 10rpx;
  text-align: right;
  color: #3a8ee6;
  font-size: 24rpx;
}
.step-name {
  font-size: 28rpx;
  font-weight: bold;
}
.badge {
  font-size: 22rpx;
  padding: 4rpx 14rpx;
  border-radius: 20rpx;
}
.badge.pending {
  color: #e6a23c;
  background: #fdf6ec;
}
.badge.done {
  color: #67c23a;
  background: #f0f9eb;
}
.badge.na {
  color: #909399;
  background: #f4f4f5;
}
.info {
  margin: 14rpx 0;
  color: #606266;
  font-size: 24rpx;
  line-height: 1.7;
}
.btn {
  margin-top: 16rpx;
}
.voucher {
  margin-top: 16rpx;
  padding: 20rpx;
  background: #f5f7fa;
  border-radius: 10rpx;
  font-size: 24rpx;
  line-height: 1.8;
}
.mono {
  word-break: break-all;
}
.foot-hint {
  text-align: center;
  color: #c0c4cc;
  font-size: 22rpx;
  margin-top: 20rpx;
}
</style>
