<template>
  <view class="page">
    <view v-if="me" class="card">
      <view class="name">{{ me.realName || me.account }}</view>
      <view class="muted">学号 {{ me.account }}</view>
      <view class="muted">角色 {{ me.role }}</view>
      <view v-if="groups.length" class="groups">
        <text v-for="g in groups" :key="g.code" class="tag">{{ g.name || g.code }}</text>
      </view>
    </view>

    <view v-else class="card">
      <view class="muted">未登录或登录已过期</view>
      <button class="btn" type="primary" size="mini" @click="goLogin">去登录</button>
    </view>

    <view class="card">
      <view class="row clickable" @click="goTab('/pages/checkin/checkin')">报到进度 <text class="arrow">›</text></view>
      <view class="row clickable" @click="goTab('/pages/qa/qa')">智能问答 <text class="arrow">›</text></view>
      <view class="row disabled">我的资助：aid-service 待实现</view>
      <view class="row disabled">安全知识：content-service 待实现</view>
    </view>

    <button v-if="me" class="logout" size="mini" :loading="loggingOut" @click="doLogout">退出登录</button>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { fetchMe, logout } from '@/api/auth'
import { getToken, clearAuth } from '@/api/request'
import { requireLogin } from '@/composables/requireLogin'

const me = ref(null)
const groups = ref([])
const loggingOut = ref(false)

onShow(async () => {
  if (!getToken()) {
    me.value = null
    return
  }
  try {
    const data = await fetchMe()
    me.value = data
    groups.value = data.groups || []
  } catch (e) {
    me.value = null
  }
})

function goLogin() {
  uni.navigateTo({ url: '/pages/login/login' })
}

function goTab(url) {
  if (!requireLogin()) return
  uni.switchTab({ url })
}

async function doLogout() {
  loggingOut.value = true
  try {
    // 服务端删 Redis 会话，旧令牌立即失效；失败也照样清本地
    await logout()
  } catch (e) {
    // 忽略
  } finally {
    clearAuth()
    loggingOut.value = false
    me.value = null
    uni.reLaunch({ url: '/pages/login/login' })
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
.name {
  font-size: 36rpx;
  font-weight: bold;
  margin-bottom: 12rpx;
}
.muted {
  color: #909399;
  font-size: 24rpx;
  line-height: 1.8;
}
.groups {
  margin-top: 16rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}
.tag {
  font-size: 22rpx;
  color: #3a8ee6;
  background: #ecf5ff;
  border-radius: 20rpx;
  padding: 6rpx 18rpx;
}
.row {
  display: flex;
  justify-content: space-between;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f2f3f5;
  font-size: 28rpx;
}
.row:last-child {
  border-bottom: none;
}
.clickable {
  color: #303133;
}
.disabled {
  color: #c0c4cc;
}
.arrow {
  color: #c0c4cc;
}
.btn {
  margin-top: 20rpx;
}
.logout {
  width: 100%;
  color: #f56c6c;
  background: #fff;
  border-radius: 12rpx;
}
</style>
