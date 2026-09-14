<template>
  <view class="page">
    <view class="card">
      <view class="title">迎新数字人</view>
      <view class="subtitle">大学生迎新导引服务</view>
    </view>

    <view class="card">
      <view class="section-title">服务连通性自检</view>
      <button class="btn" type="primary" :loading="loading" @click="check">调用网关 /api/qa/ping</button>
      <view v-if="result" class="result">{{ result }}</view>
    </view>

    <view class="card">
      <view class="section-title">数字人交互区</view>
      <view class="placeholder">骨架占位：数字人形象 + 语音/文字问答待接入</view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { request } from '@/api/request'

const loading = ref(false)
const result = ref('')

async function check() {
  loading.value = true
  try {
    const data = await request({ url: '/qa/ping' })
    result.value = JSON.stringify(data)
  } catch (e) {
    result.value = String(e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page {
  padding: 24rpx;
}
.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
}
.title {
  font-size: 44rpx;
  font-weight: bold;
  color: #3a8ee6;
}
.subtitle {
  margin-top: 12rpx;
  color: #909399;
}
.section-title {
  font-weight: bold;
  margin-bottom: 20rpx;
}
.result {
  margin-top: 20rpx;
  padding: 16rpx;
  background: #f5f7fa;
  border-radius: 8rpx;
  font-size: 24rpx;
  word-break: break-all;
}
.placeholder {
  color: #c0c4cc;
  text-align: center;
  padding: 60rpx 0;
  border: 2rpx dashed #dcdfe6;
  border-radius: 12rpx;
}
</style>
