import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

// 用户状态：token 同时写 Pinia 与 localStorage（yingxin_token），沿用仓库既有约定
export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('yingxin_token') || '')
  const username = ref(localStorage.getItem('yingxin_username') || '')
  const role = ref(localStorage.getItem('yingxin_role') || '')
  const userInfo = ref(safeParse(localStorage.getItem('yingxin_user')))

  const realName = computed(() => userInfo.value?.realName || username.value)

  function safeParse(raw) {
    try {
      return raw ? JSON.parse(raw) : null
    } catch {
      return null
    }
  }

  // 登录成功 / 拉取到当前用户信息后调用
  function setLogin(newToken, name, info) {
    token.value = newToken
    username.value = name
    role.value = info?.role || ''
    userInfo.value = info || null
    localStorage.setItem('yingxin_token', newToken)
    localStorage.setItem('yingxin_username', name)
    localStorage.setItem('yingxin_role', info?.role || '')
    localStorage.setItem('yingxin_user', JSON.stringify(info || null))
  }

  function logout() {
    token.value = ''
    username.value = ''
    role.value = ''
    userInfo.value = null
    localStorage.removeItem('yingxin_token')
    localStorage.removeItem('yingxin_username')
    localStorage.removeItem('yingxin_role')
    localStorage.removeItem('yingxin_user')
  }

  return { token, username, role, userInfo, realName, setLogin, logout }
})
