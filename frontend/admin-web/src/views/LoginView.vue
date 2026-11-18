<template>
  <div class="login-page">
    <el-card class="login-card">
      <template #header>
        <h2>迎新数字人 · 管理后台</h2>
      </template>
      <el-form :model="form" label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="管理员用户名 / 学生学号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password />
        </el-form-item>
        <el-form-item label="身份">
          <el-radio-group v-model="form.loginType">
            <el-radio :value="2">管理员</el-radio>
            <el-radio :value="1">学生</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-button type="primary" native-type="submit" style="width: 100%" :loading="loading">
          登录
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
// 登录页：调用 auth-service 登录接口，token 双写 Pinia 与 localStorage
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const form = reactive({ username: '', password: '', loginType: 2 })
const loading = ref(false)

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const res = await login({ ...form })
    const data = res.data
    userStore.setLogin(data.accessToken, data.realName || form.username, data)
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/dashboard')
  } catch {
    // 失败提示由拦截器统一弹出
  } finally {
    loading.value = false
  }
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
  width: 380px;
}
.login-card h2 {
  margin: 0;
  font-size: 18px;
  text-align: center;
}
</style>
