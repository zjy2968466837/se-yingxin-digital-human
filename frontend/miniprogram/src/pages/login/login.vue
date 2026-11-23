<template>
  <view class="page">
    <view class="hero">
      <view class="hero-title">迎新数字人</view>
      <view class="hero-sub">学号登录后可使用报到进度与智能问答</view>
    </view>

    <view class="card">
      <view class="field">
        <text class="label">学号</text>
        <input v-model="account" class="input" placeholder="例如 ST2026001" :disabled="loading" />
      </view>
      <view class="field">
        <text class="label">密码</text>
        <input v-model="password" class="input" password placeholder="演示账号密码 123456" :disabled="loading" />
      </view>
      <button class="btn" type="primary" :loading="loading" @click="submit">登录</button>
      <view class="hint">演示账号：ST2026001 / 123456</view>
    </view>

    <view class="footer">登录后令牌保存在本机，退出登录即失效</view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { login } from '@/api/auth'
import { setAuth } from '@/api/request'

const account = ref('')
const password = ref('')
const loading = ref(false)

async function submit() {
  const username = account.value.trim()
  if (!username) {
    uni.showToast({ title: '请输入学号', icon: 'none' })
    return
  }
  if (!password.value) {
    uni.showToast({ title: '请输入密码', icon: 'none' })
    return
  }
  loading.value = true
  try {
    // loginType=1 表示学生/教职工（2 是后台管理员）
    const data = await login({ username, password: password.value, loginType: 1 })
    setAuth(data.accessToken, {
      userId: data.userId,
      account: username,
      realName: data.realName,
      role: data.role,
      expiresAt: Date.now() + Number(data.expiresIn || 0) * 1000,
    })
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/checkin/checkin' })
    }, 300)
  } catch (e) {
    // 错误提示已由 request 统一弹出
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page {
  padding: 24rpx;
}
.hero {
  padding: 48rpx 32rpx 24rpx;
}
.hero-title {
  font-size: 48rpx;
  font-weight: bold;
  color: #3a8ee6;
}
.hero-sub {
  margin-top: 12rpx;
  color: #909399;
  font-size: 26rpx;
}
.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx;
}
.field {
  margin-bottom: 28rpx;
}
.label {
  display: block;
  color: #606266;
  font-size: 26rpx;
  margin-bottom: 12rpx;
}
.input {
  height: 84rpx;
  padding: 0 20rpx;
  background: #f5f7fa;
  border-radius: 10rpx;
  font-size: 30rpx;
}
.btn {
  margin-top: 8rpx;
}
.hint {
  margin-top: 20rpx;
  color: #909399;
  font-size: 24rpx;
  text-align: center;
}
.footer {
  margin-top: 32rpx;
  color: #c0c4cc;
  font-size: 22rpx;
  text-align: center;
}
</style>
