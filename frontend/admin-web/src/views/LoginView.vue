<template>
  <div class="login-page">
    <el-card class="login-card" shadow="always">
      <div class="brand">
        <h2>迎新数字人 · 管理后台</h2>
        <p class="sub">FR-9.1 管理员登录</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            maxlength="32"
            autocomplete="username"
            clearable
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            maxlength="64"
            autocomplete="current-password"
            show-password
          />
        </el-form-item>

        <el-form-item label="验证码" prop="captchaCode">
          <div class="captcha-row">
            <el-input
              v-model="form.captchaCode"
              placeholder="4 位字符"
              maxlength="4"
              autocomplete="off"
              @keyup.enter="handleLogin"
            />
            <img
              v-if="captchaImage"
              :src="captchaImage"
              class="captcha-img"
              alt="验证码"
              title="点击刷新"
              @click="loadCaptcha"
            />
            <div v-else class="captcha-img captcha-placeholder" title="点击刷新" @click="loadCaptcha">
              加载中
            </div>
          </div>
        </el-form-item>

        <el-button type="primary" native-type="submit" class="submit" :loading="submitting">
          {{ submitting ? '登录中…' : '登录' }}
        </el-button>
      </el-form>

      <p class="switch">
        没有账号？
        <router-link to="/register">去注册</router-link>
      </p>

      <p v-if="isDev" class="tip">演示账号：由课程另行提供（仅开发环境显示）</p>
      <p v-else class="tip">账号由学校信息中心统一分配</p>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCaptcha, login } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const submitting = ref(false)
const captchaId = ref('')
const captchaImage = ref('')
const isDev = import.meta.env.DEV

const form = reactive({
  username: '',
  password: '',
  captchaCode: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 4, message: '验证码为 4 位字符', trigger: 'blur' },
  ],
}

/** 验证码一次性：每次加载/失败都换新的，避免重复提交同一个码 */
async function loadCaptcha() {
  try {
    const res = await getCaptcha()
    if (res.code !== 0 || !res.data) {
      ElMessage.error(res.message || '验证码加载失败，请稍后重试')
      return
    }
    captchaId.value = res.data.captchaId
    captchaImage.value = res.data.image
    form.captchaCode = ''
  } catch (e) {
    // HTTP 错误已由 request.js 统一提示
  }
}

async function handleLogin() {
  if (submitting.value || !formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (!captchaId.value) {
    ElMessage.warning('验证码已失效，请刷新后重试')
    await loadCaptcha()
    return
  }

  submitting.value = true
  try {
    const res = await login(
      {
        username: form.username.trim(),
        password: form.password,
        captchaId: captchaId.value,
        captchaCode: form.captchaCode.trim(),
      },
      // 失败文案由本页展示，禁止拦截器再弹一次
      { silent: true },
    )
    if (res.code !== 0) {
      // 2802 验证码错误 / 2803 锁定：HTTP 200 + 业务码，这里负责提示
      ElMessage.error(res.message || '登录失败')
      form.password = ''
      await loadCaptcha()
      return
    }

    // 会话凭证是后端 Set-Cookie 下来的 HttpOnly Cookie，前端只记录展示用的用户信息
    userStore.setUser({
      username: res.data?.username || form.username.trim(),
      realName: res.data?.realName || '',
    })
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    router.replace(redirect)
  } catch (e) {
    // 1003 账号或密码错误 / 1004 禁用 走 HTTP 401，拦截器已静音，文案在这里展示
    ElMessage.error(failureText(e, '登录失败，请检查账号与密码'))
    form.password = ''
    await loadCaptcha()
  } finally {
    submitting.value = false
  }
}

/** 401 已被 request.js 静默，需要调用方自己把后端文案展示出来 */
function failureText(e, fallback) {
  if (!e?.response) return '网络异常，请检查网络后重试'
  return e.response.data?.message || fallback
}

onMounted(loadCaptcha)
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #eef3fb 0%, #f7f9fc 55%, #eef7f3 100%);
}

.login-card {
  width: 380px;
  border-radius: 12px;
}

.brand {
  text-align: center;
  margin-bottom: 8px;
}

.brand h2 {
  margin: 4px 0 6px;
  font-size: 20px;
  color: #1f2d3d;
}

.sub {
  margin: 0 0 18px;
  font-size: 12px;
  color: #909399;
}

.captcha-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.captcha-img {
  width: 132px;
  height: 44px;
  flex: 0 0 132px;
  border-radius: 6px;
  border: 1px solid #dcdfe6;
  cursor: pointer;
  background: #f6f8fa;
}

.captcha-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #909399;
}

.submit {
  width: 100%;
  margin-top: 4px;
}

.tip {
  margin: 14px 0 0;
  text-align: center;
  font-size: 12px;
  color: #909399;
}

.switch {
  margin: 14px 0 0;
  text-align: center;
  font-size: 13px;
  color: #606266;
}

.switch a {
  color: var(--el-color-primary);
  text-decoration: none;
}

.switch a:hover {
  text-decoration: underline;
}
</style>
