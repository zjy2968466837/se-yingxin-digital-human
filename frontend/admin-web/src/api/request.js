import axios from 'axios'
import { ElMessage } from 'element-plus'

// 统一走网关：/api/<服务名>/...，由 gateway 路由到具体微服务
// 鉴权已改为 HttpOnly Cookie（YX_SESSION）：前端读不到令牌，也不需要读，
// 靠 withCredentials 让浏览器自动带上/写回 Cookie。不要再往请求头塞 Authorization。
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 10000,
  withCredentials: true,
})

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const status = error?.response?.status

    // 401 = 未登录 / 会话过期（统一响应体 code 1001）。不在这里弹提示：
    // 路由守卫会跳登录页，登录/注册页由调用方展示业务文案（如 1003 账号或密码错误），
    // 否则会出现「请求失败」和业务提示重复弹两次。
    if (status === 401) {
      error.isUnauthorized = true
      return Promise.reject(error)
    }

    ElMessage.error(error?.response?.data?.message || '请求失败，请稍后重试')
    return Promise.reject(error)
  },
)

export default request
