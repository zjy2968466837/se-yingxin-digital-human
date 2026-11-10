import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('yingxin_token') || '')
  const username = ref(localStorage.getItem('yingxin_username') || '')
  const realName = ref(localStorage.getItem('yingxin_real_name') || '')

  function setLogin(newToken, name, displayName = '') {
    token.value = newToken
    username.value = name
    realName.value = displayName
    localStorage.setItem('yingxin_token', newToken)
    localStorage.setItem('yingxin_username', name)
    localStorage.setItem('yingxin_real_name', displayName)
  }

  function logout() {
    token.value = ''
    username.value = ''
    realName.value = ''
    localStorage.removeItem('yingxin_token')
    localStorage.removeItem('yingxin_username')
    localStorage.removeItem('yingxin_real_name')
  }

  return { token, username, realName, setLogin, logout }
})
