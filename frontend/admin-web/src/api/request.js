import axios from 'axios'
import { ElMessage } from 'element-plus'

// 统一走网关：/api/<服务名>/...，由 gateway 路由到具体微服务
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 10000,
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('yingxin_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    ElMessage.error(error?.response?.data?.message || '请求失败，请稍后重试')
    return Promise.reject(error)
  },
)

export default request
