import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('yingxin_token') || '')
  const username = ref(localStorage.getItem('yingxin_username') || '')

  function setLogin(newToken, name) {
    token.value = newToken
    username.value = name
    localStorage.setItem('yingxin_token', newToken)
    localStorage.setItem('yingxin_username', name)
  }

  function logout() {
    token.value = ''
    username.value = ''
    localStorage.removeItem('yingxin_token')
    localStorage.removeItem('yingxin_username')
  }

  return { token, username, setLogin, logout }
})
