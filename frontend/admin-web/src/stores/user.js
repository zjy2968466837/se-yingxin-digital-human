import { defineStore } from 'pinia'
import { ref } from 'vue'
import { logout as logoutApi, me } from '@/api/auth'

// 会话状态只放内存：真正的凭证是 HttpOnly Cookie（YX_SESSION），前端读不到，
// 所以这里不做任何 localStorage 持久化，刷新页面靠 fetchMe() 重新确认。
export const useUserStore = defineStore('user', () => {
  const username = ref('')
  const realName = ref('')
  const role = ref('')
  // checked：本次页面生命周期内是否已经确认过会话状态，避免每次路由跳转都打一次 /me
  const checked = ref(false)

  /**
   * 写入当前用户。允许部分字段缺省（如登录响应不含 role），缺省的字段保留原值，
   * 避免用不完整的数据把已知信息清空。
   */
  function setUser(user = {}) {
    if (user.username !== undefined) username.value = user.username || ''
    if (user.realName !== undefined) realName.value = user.realName || ''
    if (user.role !== undefined) role.value = user.role || ''
    checked.value = true
  }

  /** 只清本地状态，不发请求 */
  function clear() {
    username.value = ''
    realName.value = ''
    role.value = ''
  }

  /**
   * 用 /me 确认会话是否有效。
   * 成功：写入用户信息并返回 true；失败（401 / 网络错误 / 业务码非 0）：清空状态并返回 false。
   * 无论成功失败都置 checked = true，让守卫下次不再重复请求。
   */
  async function fetchMe() {
    try {
      const res = await me()
      if (res?.code !== 0 || !res.data) {
        clear()
        checked.value = true
        return false
      }
      setUser(res.data)
      return true
    } catch (e) {
      clear()
      checked.value = true
      return false
    }
  }

  /** 退出登录：调接口让后端删会话/清 Cookie，失败也照样清本地状态 */
  async function logout() {
    try {
      await logoutApi()
    } catch (e) {
      // 忽略失败：会话可能已经过期，本地状态必须清干净
    }
    clear()
    checked.value = true
  }

  return { username, realName, role, checked, setUser, fetchMe, logout }
})
