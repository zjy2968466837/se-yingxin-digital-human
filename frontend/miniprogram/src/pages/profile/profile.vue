<template>
  <view class="page">
    <view class="card">
      <view class="row">身份认证：待接入学校统一身份认证</view>
      <view class="row">报到进度：待接入 checkin-service</view>
      <view class="row">我的资助：待接入 aid-service</view>
    </view>
  </view>
</template>

<script setup>
// 骨架占位页
</script>

<style scoped>
.page {
  padding: 24rpx;
}
.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx;
}
.row {
  padding: 24rpx 0;
  border-bottom: 2rpx solid #f0f2f5;
}
.row:last-child {
  border-bottom: none;
}
</style>
