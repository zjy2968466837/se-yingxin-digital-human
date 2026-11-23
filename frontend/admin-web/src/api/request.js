import axios from 'axios'
// 统一走网关：/api/<服务名>/...，由 gateway 路由到具体微服务
// 鉴权已改为 HttpOnly Cookie（YX_SESSION）：前端读不到令牌，也不需要读，
// 靠 withCredentials 让浏览器自动带上/写回 Cookie。不要再往请求头塞 Authorization。
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 10000,
  withCredentials: true,
})

// 会话失效处理器：由 main.js 注入"跳登录页"的动作，避免 request ↔ router 循环依赖
let unauthorizedHandler = null

export function setUnauthorizedHandler(fn) {
  unauthorizedHandler = fn
}

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const status = error?.response?.status
    const silent = error?.config?.silent === true

    // 401 = 未登录 / 会话过期（code 1001）。运行期要跳登录页，否则页面会停在空数据上。
    // 登录/注册页由调用方展示业务文案（如 1003 账号或密码错误），所以不在拦截器弹提示。
    if (status === 401) {
      error.isUnauthorized = true
      if (typeof unauthorizedHandler === 'function') {
        unauthorizedHandler(error)
      }
      return Promise.reject(error)
    }

    // silent=true 表示调用方自己负责提示（登录/注册），避免同一错误弹两次
    if (!silent) {
      ElMessage.error(error?.response?.data?.message || '请求失败，请稍后重试')
    }
    return Promise.reject(error)
  },
)

export default request

