// 登录态守卫：未登录时跳登录页并返回 false，调用方直接 return
// 注意 tabBar 页要用 switchTab 之外的方式跳登录页（登录页不是 tabBar 页）
import { getToken } from '@/api/request'

/**
 * @param {{ relaunch?: boolean }} [options] relaunch=true 用 reLaunch（清空页面栈），否则 navigateTo
 * @returns {boolean} 已登录为 true
 */
export function requireLogin(options = {}) {
  if (getToken()) return true
  const url = '/pages/login/login'
  if (options.relaunch) {
    uni.reLaunch({ url })
  } else {
    uni.navigateTo({ url })
  }
  return false
}
