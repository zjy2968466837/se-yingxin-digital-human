import request from './request'

// 路径与网关一致：/api/qa/** → qa-service(:8101)，网关不裁剪前缀。
// request.js 的 baseURL 是 /api，所以这里写 /qa/sessions。

/** 分页查询会话：{ page, size, userId } */
export function listSessions(params) {
  return request.get('/qa/sessions', { params })
}

/** 创建会话：{ userId, channel } */
export function createSession(data) {
  return request.post('/qa/sessions', data)
}

/** 按 id 查询（会命中 Redis 缓存） */
export function getSession(id) {
  return request.get(`/qa/sessions/${id}`)
}

/** 逻辑删除：deleted 写成本行 id */
export function deleteSession(id) {
  return request.delete(`/qa/sessions/${id}`)
}
