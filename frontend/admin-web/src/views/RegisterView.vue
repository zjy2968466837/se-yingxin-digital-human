<template>
  <div class="register-page">
    <el-card class="register-card" shadow="always">
      <div class="brand">
        <h2>迎新数字人 · 管理后台</h2>
        <p class="sub">管理员注册</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleRegister"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="4–32 位字母、数字或下划线"
            maxlength="32"
            autocomplete="username"
            clearable
          />
        </el-form-item>

        <el-form-item label="姓名" prop="realName">
          <el-input
            v-model="form.realName"
            placeholder="真实姓名，2–32 个字"
            maxlength="32"
            autocomplete="name"
            clearable
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="8–64 位"
            maxlength="64"
            autocomplete="new-password"
            show-password
          />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            maxlength="64"
            autocomplete="new-password"
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
              @keyup.enter="handleRegister"
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
          {{ submitting ? '注册中…' : '注册并登录' }}
        </el-button>
      </el-form>

      <p class="switch">
        已有账号？
        <router-link to="/login">去登录</router-link>
      </p>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getCaptcha, register } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const submitting = ref(false)
const captchaId = ref('')
const captchaImage = ref('')

const form = reactive({
  username: '',
  realName: '',
  password: '',
  confirmPassword: '',
  captchaCode: '',
})

/** 两次密码一致性校验（前端先挡一道，后端仍会校验） */
function validateConfirm(rule, value, callback) {
  if (!value) {
    callback(new Error('请再次输入密码'))
    return
  }
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 32, message: '用户名长度为 4–32 位', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]+$/, message: '只能包含字母、数字和下划线', trigger: 'blur' },
  ],
  realName: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 32, message: '姓名长度为 2–32 位', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 64, message: '密码长度为 8–64 位', trigger: 'blur' },
  ],
  confirmPassword: [{ validator: validateConfirm, trigger: 'blur' }],
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

/** 401 已被 request.js 静默，需要调用方自己把后端文案展示出来 */
function failureText(e, fallback) {
  if (!e?.response) return '网络异常，请检查网络后重试'
  return e.response.data?.message || fallback
}

/** 失败后统一换码并清空密码，避免用户拿旧验证码反复提交 */
async function resetCaptchaAndPassword() {
  form.password = ''
  form.confirmPassword = ''
  await loadCaptcha()
}

async function handleRegister() {
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
    const res = await register(
      {
        username: form.username.trim(),
        realName: form.realName.trim(),
        password: form.password,
        confirmPassword: form.confirmPassword,
        captchaId: captchaId.value,
        captchaCode: form.captchaCode.trim(),
      },
      // 失败文案由本页展示，禁止拦截器再弹一次
      { silent: true },
    )
    if (res.code !== 0) {
      // 2802 验证码错误 / 2804 用户名已存在：HTTP 200 + 业务码
      ElMessage.error(res.message || '注册失败')
      await resetCaptchaAndPassword()
      return
    }

    // 注册即建会话（后端 Set-Cookie），前端只记录展示用的用户信息
    userStore.setUser({
      username: res.data?.username || form.username.trim(),
      realName: res.data?.realName || form.realName.trim(),
    })
    ElMessage.success('注册成功')
    router.replace('/dashboard')
  } catch (e) {
    ElMessage.error(failureText(e, '注册失败，请稍后重试'))
    await resetCaptchaAndPassword()
  } finally {
    submitting.value = false
  }
}

onMounted(loadCaptcha)
</script>

<style scoped>
.register-page {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 0;
  box-sizing: border-box;
  background: linear-gradient(135deg, #eef3fb 0%, #f7f9fc 55%, #eef7f3 100%);
}

.register-card {
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
