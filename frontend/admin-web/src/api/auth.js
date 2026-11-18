import request from './request'

// 登录：loginType 2=管理员，1=学生学号
export function login(data) {
  return request.post('/auth/login', data)
}

// 当前登录用户信息
export function fetchMe() {
  return request.get('/auth/me')
}

// 退出登录
export function logout() {
  return request.post('/auth/logout')
}
