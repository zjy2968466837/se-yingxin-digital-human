import request from './request'

// 用户与审批
export function listUsers() {
  return request.get('/auth/admin/users')
}

export function listPendingUsers() {
  return request.get('/auth/admin/pending')
}

// 审批：approve true=通过 false=驳回
export function approveUser(userId, approve) {
  return request.post('/auth/admin/approve', { userId, approve })
}

// 用户组（角色）
export function listGroups() {
  return request.get('/auth/admin/groups')
}

export function createGroup(data) {
  return request.post('/auth/admin/group', data)
}

export function deleteGroup(id) {
  return request.delete('/auth/admin/group', { params: { id } })
}

// 为用户分配用户组
export function assignUserGroups(userId, groupCodes) {
  return request.post('/auth/admin/user-group', { userId, groupCodes })
}

// 权限矩阵：模块/权限点 + 各用户组授权情况
export function getMatrix() {
  return request.get('/auth/admin/matrix')
}

// 授权/取消授权：grant true=授权 false=取消
export function setPriv(groupCode, priv, grant) {
  return request.post('/auth/admin/priv', { groupCode, priv, grant })
}

// 审计日志（分页）
export function listAudit(params) {
  return request.get('/auth/admin/audit', { params })
}
