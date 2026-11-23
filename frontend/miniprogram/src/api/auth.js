// 认证相关接口（auth-service）
import { request } from './request'

/** 登录：{ username 学号, password, loginType: 1 } → { accessToken, expiresIn, userId, role, realName, groups, perms }
 *  auth:false —— 这是"取凭证"的请求，401 表示口令错误，不应触发跳登录页 */
export function login(data) {
  return request({ url: '/auth/login', method: 'POST', data, auth: false })
}

/** 当前登录用户：{ userId, account, realName, role, groups, perms } */
export function fetchMe() {
  return request({ url: '/auth/me' })
}

/** 退出登录：服务端删 Redis 会话，旧令牌立即失效 */
export function logout() {
  return request({ url: '/auth/logout', method: 'POST' })
}
