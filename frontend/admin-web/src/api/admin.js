import request from './request'

// 服务连通性自检：走网关打到 admin-service
export function pingAdmin() {
  return request.get('/admin/ping')
}
