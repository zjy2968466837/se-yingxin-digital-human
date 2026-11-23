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

// 非 2xx 的兜底文案：403 常见于来源未进网关 CORS 白名单，502 常见于后端没起
const HTTP_HINT = {
  400: '请求参数有误',
  403: '没有访问权限（也可能是本站来源未加入网关白名单）',
  404: '接口不存在，可能前后端版本不一致',
  429: '请求过于频繁，请稍后再试',
  500: '服务内部错误，请稍后重试',
  502: '网关连不上后端服务，请确认服务已启动',
  503: '服务暂不可用，请稍后重试',
  504: '后端服务响应超时',
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
      ElMessage.error(
        error?.response?.data?.message || HTTP_HINT[status] || '请求失败，请稍后重试',
      )
    }
    return Promise.reject(error)
  },
)

export default request

