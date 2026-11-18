import axios from 'axios'
import { ElMessage } from 'element-plus'

// 统一走网关：/api/<服务名>/...，由 gateway 路由到具体微服务
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 10000,
})

// 请求拦截器：附加 Bearer token
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('yingxin_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 未登录 / 登录态失效：清 token 回登录页
function redirectToLogin() {
  localStorage.removeItem('yingxin_token')
  localStorage.removeItem('yingxin_username')
  localStorage.removeItem('yingxin_role')
  localStorage.removeItem('yingxin_user')
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

request.interceptors.response.use(
  (response) => {
    // 统一响应结构 { code, message, data }，code=0 表示成功
    const res = response.data
    if (res && typeof res.code === 'number' && res.code !== 0) {
      // 1001/1002 视为登录态失效
      if (res.code === 1001 || res.code === 1002) {
        redirectToLogin()
      } else {
        ElMessage.error(res.message || '请求失败，请稍后重试')
      }
      return Promise.reject(new Error(res.message || `code=${res.code}`))
    }
    return res
  },
  (error) => {
    // HTTP 401 表示未登录
    if (error?.response?.status === 401) {
      redirectToLogin()
    } else {
      ElMessage.error(error?.response?.data?.message || '请求失败，请稍后重试')
    }
    return Promise.reject(error)
  },
)

export default request
