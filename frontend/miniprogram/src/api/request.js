// 学生端请求封装（一期只跑 H5，走 vite 代理，与网关同源）
//
// 鉴权模型：学生在 /api/auth/login 用学号+口令登录，后端返回 Bearer 令牌
// （accessToken，JWT，claims 含 userId/role/jti；服务端在 Redis 存会话，登出即失效）。
// 与管理后台的 HttpOnly Cookie 方案不同：这里的令牌由前端自己保存并逐次携带。
//
// 约定：
//   - 成功时 resolve 统一响应体里的 data（不是整个 {code,message,data}）
//   - 401 → 清本地凭证并跳登录页（会话过期、登出、令牌被删都走这里）
//   - 业务码非 0 → 弹提示并 reject，调用方按需处理

const BASE_URL = import.meta.env.VITE_API_BASE || '/api'

export const TOKEN_KEY = 'yingxin_token'
export const USER_KEY = 'yingxin_user'
export const QA_SESSION_KEY = 'yingxin_qa_session'

export function getToken() {
  return uni.getStorageSync(TOKEN_KEY) || ''
}

export function setAuth(token, user) {
  uni.setStorageSync(TOKEN_KEY, token || '')
  if (user) uni.setStorageSync(USER_KEY, user)
}

export function getUser() {
  return uni.getStorageSync(USER_KEY) || null
}

export function clearAuth() {
  uni.removeStorageSync(TOKEN_KEY)
  uni.removeStorageSync(USER_KEY)
  // 问答会话与账号绑定，必须一起清，否则换号后会复用上一个账号的 sessionId
  uni.removeStorageSync(QA_SESSION_KEY)
}

// 防止并发 401 时反复 reLaunch
let redirecting = false
function redirectToLogin() {
  if (redirecting) return
  redirecting = true
  clearAuth()
  uni.reLaunch({
    url: '/pages/login/login',
    complete: () => {
      redirecting = false
    },
  })
}

export function request(options = {}) {
  return new Promise((resolve, reject) => {
    const token = getToken()
    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      timeout: options.timeout || 15000,
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(options.header || {}),
      },
      success: (res) => {
        const body = res.data
        if (res.statusCode === 401) {
          // 后端把 1001（未登录/会话失效）与 1003（账号或密码错误）都映射成 HTTP 401，
          // 所以不能一律跳登录页：
          //   - 登录接口用 auth:false 声明这是"取凭证"的请求，失败只提示不跳转；
          //   - 其余请求只有在"手里本来就有令牌"时才判定为会话过期。
          if (options.auth !== false && getToken()) {
            redirectToLogin()
            reject(res)
            return
          }
          // 无令牌场景（典型是登录口令错误）：按业务错误提示，交给调用方处理
          if (body && body.message) {
            uni.showToast({ title: body.message, icon: 'none' })
          }
          reject(body || res)
          return
        }
        if (res.statusCode < 200 || res.statusCode >= 300) {
          uni.showToast({ title: `请求失败 ${res.statusCode}`, icon: 'none' })
          reject(res)
          return
        }
        // 统一响应体：{ code, message, data }
        if (body && typeof body === 'object' && 'code' in body) {
          if (body.code === 0) {
            resolve(body.data)
          } else {
            uni.showToast({ title: body.message || '请求失败', icon: 'none' })
            reject(body)
          }
          return
        }
        resolve(body)
      },
      fail: (err) => {
        uni.showToast({ title: '网络异常，请确认后端已启动', icon: 'none' })
        reject(err)
      },
    })
  })
}
