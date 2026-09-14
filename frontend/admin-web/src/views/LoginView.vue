<template>
  <div class="login-page">
    <el-card class="login-card">
      <template #header>
        <h2>迎新数字人 · 管理后台</h2>
      </template>
      <el-form :model="form" @submit.prevent="handleLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" show-password />
        </el-form-item>
        <el-button type="primary" native-type="submit" style="width: 100%">登录</el-button>
      </el-form>
      <p class="tip">骨架阶段为占位登录，接入 auth-service 后替换为真实鉴权。</p>
    </el-card>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const form = reactive({ username: '', password: '' })

function handleLogin() {
  userStore.setLogin('placeholder-token', form.username || 'guest')
  router.push(route.query.redirect || '/dashboard')
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
}
.login-card {
  width: 360px;
}
.tip {
  color: #909399;
  font-size: 12px;
  margin: 12px 0 0;
}
</style>
