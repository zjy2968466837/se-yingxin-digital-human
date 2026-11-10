import request from './request'

// 网关白名单只放行 captcha / login / register / logout（开发规范 §4.2），所以这几个请求不带凭证。
// request.js 的 baseURL 是 /api，因此这里写 /admin/auth/...
// 鉴权走 HttpOnly Cookie（YX_SESSION）：登录/注册成功时由后端 Set-Cookie，响应体只返回用户信息。

/** 获取图形验证码：{ captchaId, image(dataURL), expireInSeconds } */
export function getCaptcha() {
  return request.get('/admin/auth/captcha')
}

/** 管理员登录：{ username, password, captchaId, captchaCode } → { username, realName, expiresIn } */
export function login(data) {
  return request.post('/admin/auth/login', data)
}

/** 管理员注册：{ username, realName, password, confirmPassword, captchaId, captchaCode } → 直接建立会话 */
export function register(data) {
  return request.post('/admin/auth/register', data)
}

/** 退出登录：后端删会话并清 Cookie */
export function logout() {
  return request.post('/admin/auth/logout')
}

/** 当前登录用户：{ userId, username, realName, role }；未登录返回 HTTP 401（code 1001） */
export function me() {
  return request.get('/admin/auth/me')
}
