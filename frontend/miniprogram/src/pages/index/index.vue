<template>
  <view class="page">
    <view class="card hero">
      <view class="title">迎新数字人</view>
      <view class="subtitle">大学生迎新导引服务</view>
    </view>

    <!-- 登录态 -->
    <view class="card">
      <view v-if="user" class="row-between">
        <view>
          <view class="who">你好，{{ user.realName || user.account }}</view>
          <view class="muted">学号 {{ user.account }} · 角色 {{ user.role }}</view>
        </view>
        <button class="mini" size="mini" @click="goProfile">我的</button>
      </view>
      <view v-else class="row-between">
        <view>
          <view class="who">还未登录</view>
          <view class="muted">登录后可查看报到进度、使用智能问答</view>
        </view>
        <button class="mini" size="mini" type="primary" @click="goLogin">去登录</button>
      </view>
    </view>

    <!-- 入口 -->
    <view class="grid">
      <view class="entry" @click="go('/pages/checkin/checkin')">
        <view class="entry-title">报到进度</view>
        <view class="entry-sub">清单 / 打卡 / 凭证</view>
      </view>
      <view class="entry" @click="go('/pages/qa/qa')">
        <view class="entry-title">智能问答</view>
        <view class="entry-sub">热门问题 / 在线提问</view>
      </view>
    </view>

    <!-- 自检（保留原骨架能力，便于排障） -->
    <view class="card">
      <view class="section-title">服务连通性自检</view>
      <button class="btn" size="mini" :loading="loading" @click="check">调用网关 /api/qa/ping</button>
      <view v-if="result" class="result">{{ result }}</view>
    </view>

    <view class="foot">一期只做网页端（H5），移动浏览器优先</view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getUser } from '@/api/request'
import { pingQa } from '@/api/qa'
import { requireLogin } from '@/composables/requireLogin'

const loading = ref(false)
const result = ref('')
const user = ref(getUser())

onShow(() => {
  user.value = getUser()
})

async function check() {
  loading.value = true
  try {
    const data = await pingQa()
    result.value = JSON.stringify(data)
  } catch (e) {
    result.value = '调用失败，确认网关 8080 已启动'
  } finally {
    loading.value = false
  }
}

function go(url) {
  if (!requireLogin()) return
  uni.switchTab({ url })
}

function goLogin() {
  uni.navigateTo({ url: '/pages/login/login' })
}

function goProfile() {
  uni.switchTab({ url: '/pages/profile/profile' })
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
  padding: 32rpx;
  margin-bottom: 24rpx;
}
.hero {
  background: linear-gradient(135deg, #3a8ee6, #6aa9ee);
}
.title {
  font-size: 44rpx;
  font-weight: bold;
  color: #fff;
}
.subtitle {
  margin-top: 12rpx;
  color: rgba(255, 255, 255, 0.85);
  font-size: 26rpx;
}
.row-between {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.who {
  font-size: 30rpx;
  font-weight: bold;
  margin-bottom: 8rpx;
}
.muted {
  color: #909399;
  font-size: 24rpx;
}
.grid {
  display: flex;
  gap: 20rpx;
  margin-bottom: 24rpx;
}
.entry {
  flex: 1;
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx 24rpx;
}
.entry-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #303133;
}
.entry-sub {
  margin-top: 10rpx;
  color: #909399;
  font-size: 22rpx;
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
.foot {
  text-align: center;
  color: #c0c4cc;
  font-size: 22rpx;
}
</style>
