import request from './request'

// 网关白名单放行 /api/admin/auth/**（开发规范 §4.2），所以登录本身不带令牌。
// request.js 的 baseURL 是 /api，因此这里写 /admin/auth/...

/** 获取图形验证码：{ captchaId, image(dataURL), expireInSeconds } */
export function getCaptcha() {
  return request.get('/admin/auth/captcha')
}

/** 管理员登录：{ username, password, captchaId, captchaCode } */
export function login(data) {
  return request.post('/admin/auth/login', data)
}
